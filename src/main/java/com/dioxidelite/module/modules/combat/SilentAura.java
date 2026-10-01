package com.dioxidelite.module.modules.combat;

// ---------------------------------------------------------------------------
// 移植来源：Vape-v4 gg/vape/module/combat/SilentAura.java
// 变更：
//   - 去掉 Vape wrapper 层（gg.vape.wrapper.impl.* / MappedClasses / 混淆方法名），
//     改用 Mojang 官方映射（mc.player / LivingEntity / Vec3 / AABB）。
//   - 去掉 1.7.10 legacy 分支（updateAimLegacy / onSyntheticAttack / 直接 attackEntity）。
//   - 去掉 AI 模型分支（Vape ModelManager / LiquidBounce MLP），仅保留 PID 旋转引擎。
//   - 去掉 ForgeVersion 版本分支（DioxideLite 仅 26.1.2 单版本）。
//   - Value 系统改为 DioxideLite Setting（BooleanSetting/DoubleSetting/IntSetting/EnumSetting）。
//   - 旋转交给 DioxideLite RotationManager（setRotations + Rot2f + Priority），
//     移动修复/视觉转头由客户端自带的 MovementFix + RotationManager.renderAnimation 接管，
//     无需本模块再实现。
//   - 事件注解 @EventHandler -> @Listen，事件类 EventPrePlayerTick -> PlayerTickEvent.Pre。
// 保留：
//   - 目标选择算法（按 Distance/Yaw/Armor/Threat/Health 排序）。
//   - PID 旋转微调（pitchProportionalGain + pitchIntegral）。
//   - Perfect Swing（getAttackStrengthScale == 1.0 才攻击）。
//   - 瞄准抖动（SilentAuraAimJitter 的随机扰动）。
//   - 自适应最近可见点（RotationUtils.calculate(entity, adaptive=true, range)）。
// ---------------------------------------------------------------------------

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.RaytraceUtils;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 静默自动攻击模块，移植自 Vape-v4 SilentAura。
 * <p>
 * 与 KillAura 的区别：SilentAura 不改变实际朝向，只通过 RotationManager 注入"静默旋转"
 * （服务器侧朝向改变、客户端视觉不变），配合 Perfect Swing 节拍与 PID 微调实现隐蔽近战。
 * 移动修复与第三人称视觉转头由客户端自带的 {@code MovementFix} 和
 * {@code RotationManager.renderAnimation} 接管，本模块仅负责目标选择与瞄准/攻击。
 */
public final class SilentAura extends Module {

    public static final SilentAura INSTANCE = new SilentAura();

    private static final Minecraft mc = DioxideLite.mc();

    private SilentAura() {
        super("Silent Aura", Category.COMBAT);
    }

    /** 移植自 Vape SilentAura.TargetMode。 */
    public enum TargetMode {
        Distance,
        Yaw,
        Armor,
        Threat,
        Health
    }

    /** 移植自 Vape SilentAura.TargetArea。 */
    public enum TargetArea {
        Center,
        Closest
    }

    /** 旋转模式：Snap=瞬切、Smooth=线性平滑、Adaptive=PID 微调（Vape 默认）。 */
    public enum RotationMode {
        Snap,
        Smooth,
        Adaptive
    }

    // --- Settings（移植自 Vape SilentAura 的 Value 集合） ---
    private final DoubleSetting range = add(new DoubleSetting("Range", 3.5, 1.0, 6.0, 0.1));
    private final DoubleSetting aimRange = add(new DoubleSetting("Aim Range", 5.0, 1.0, 8.0, 0.1));
    private final DoubleSetting aimSpeed = add(new DoubleSetting("Aim Speed", 120.0, 1.0, 360.0, 1.0));
    private final DoubleSetting maxAngle = add(new DoubleSetting("Max Angle", 120.0, 1.0, 360.0, 1.0));
    private final IntSetting attackCps = add(new IntSetting("Attack Cps", 8, 1, 20, 1));
    private final BooleanSetting requireMouseDown = add(new BooleanSetting("Require Mouse Down", false));
    private final BooleanSetting disableOnDeath = add(new BooleanSetting("Disable On Death", true));
    private final BooleanSetting perfectSwing = add(new BooleanSetting("Perfect Swing", true));
    private final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", false));
    private final BooleanSetting switchTargets = add(new BooleanSetting("Switch Targets", false));
    private final BooleanSetting targetPlayers = add(new BooleanSetting("Target Players", true));
    private final BooleanSetting targetMobs = add(new BooleanSetting("Target Mobs", false));
    private final BooleanSetting targetAnimals = add(new BooleanSetting("Target Animals", false));
    private final EnumSetting<TargetMode> targetMode = add(new EnumSetting<>("Target Mode", TargetMode.Distance));
    private final EnumSetting<TargetArea> targetArea = add(new EnumSetting<>("Target Area", TargetArea.Center));
    private final EnumSetting<RotationMode> rotationMode = add(new EnumSetting<>("Rotation Mode", RotationMode.Adaptive));
    private final DoubleSetting jitterAmount = add(new DoubleSetting("Jitter Amount", 0.0, 0.0, 5.0, 0.1));

    // --- PID 状态（移植自 Vape SilentAura.updateAim PID 分支） ---
    private float pitchIntegral = 0.0f;
    private LivingEntity currentTarget;
    private long lastAttackTimeMs;

    @Listen
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (disableOnDeath.get() && (mc.player.isDeadOrDying() || mc.player.getHealth() <= 0.0f)) {
            setEnabled(false);
            return;
        }
        if (requireMouseDown.get() && !mc.options.keyAttack.isDown()) {
            currentTarget = null;
            return;
        }
        if (mc.screen != null) {
            currentTarget = null;
            return;
        }

        LivingEntity target = acquireTarget();
        currentTarget = target;
        if (target == null) {
            return;
        }

        // 瞄准（PID/Smooth/Snap）
        Rot2f desired = calculateAimRotation(target);
        applyAim(desired);

        // 攻击
        if (shouldAttack(target)) {
            doAttack(target);
        }
    }

    /**
     * 移植自 Vape SilentAura 目标选择：遍历世界实体，过滤 + 按模式排序，取第一个在射程/FOV 内的。
     */
    private LivingEntity acquireTarget() {
        List<LivingEntity> candidates = new ArrayList<>();
        Vec3 eyePos = mc.player.getEyePosition();
        double rangeSqr = aimRange.get() * aimRange.get();

        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof ArmorStand) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isDeadOrDying()) continue;

            // 类型过滤
            if (living instanceof Player p) {
                if (!targetPlayers.get()) continue;
            } else if (living instanceof net.minecraft.world.entity.monster.Monster) {
                if (!targetMobs.get()) continue;
            } else if (living instanceof net.minecraft.world.entity.animal.Animal) {
                if (!targetAnimals.get()) continue;
            } else {
                continue;
            }

            // 距离
            double distSqr = living.getEyePosition().distanceToSqr(eyePos);
            if (distSqr > rangeSqr) continue;

            // FOV（maxAngle）
            float fov = maxAngle.get().floatValue();
            if (fov < 360.0f && !RotationUtils.isInFov(living, fov)) continue;

            // 穿墙过滤
            if (!throughWalls.get()) {
                Rot2f toEntity = RotationUtils.calculate(living, true, aimRange.get());
                HitResult hit = RaytraceUtils.raytrace(toEntity, aimRange.get(), 0.0f);
                if (hit == null || hit.getType() != HitResult.Type.ENTITY) continue;
            }

            candidates.add(living);
        }

        if (candidates.isEmpty()) return null;

        // 按目标模式排序（移植自 Vape SilentAuraEntityIdComparator + TargetMode）
        candidates.sort(comparatorFor(targetMode.get()));
        return candidates.getFirst();
    }

    private Comparator<LivingEntity> comparatorFor(TargetMode mode) {
        Vec3 eyePos = mc.player.getEyePosition();
        return switch (mode) {
            case Distance -> Comparator.comparingDouble(e -> e.getEyePosition().distanceToSqr(eyePos));
            case Yaw -> Comparator.comparingDouble(e -> {
                Rot2f r = RotationUtils.calculate(e);
                return Math.abs(Mth.wrapDegrees(r.getYaw() - mc.player.getYRot()))
                        + Math.abs(Mth.wrapDegrees(r.getPitch() - mc.player.getXRot()));
            });
            case Health -> Comparator.comparingDouble(LivingEntity::getHealth).reversed();
            case Armor -> Comparator.comparingDouble(LivingEntity::getArmorValue).reversed();
            case Threat -> Comparator.comparingDouble(e -> e.getHealth() + e.getArmorValue() * 2.0);
        };
    }

    /**
     * 移植自 Vape SilentAura 自适应瞄准：算到目标的最近可见点（adaptive=true 走 RotationUtils
     * 的边界采样），叠加 jitter 抖动（SilentAuraAimJitter 的随机扰动）。
     */
    private Rot2f calculateAimRotation(LivingEntity target) {
        Rot2f base;
        if (targetArea.get() == TargetArea.Closest) {
            // adaptive=true：在 AABB 边界采样找最近可见点
            base = RotationUtils.calculate(target, true, aimRange.get());
        } else {
            base = RotationUtils.calculate(target);
        }

        // 抖动（移植自 Vape SilentAuraAimJitter）
        double jitter = jitterAmount.get();
        if (jitter > 0.0) {
            float jy = (float) (Math.random() - 0.5) * 2.0f * (float) jitter;
            float jp = (float) (Math.random() - 0.5) * 2.0f * (float) jitter;
            base = new Rot2f(base.getYaw() + jy, Mth.clamp(base.getPitch() + jp, -90.0f, 90.0f));
        }
        return base;
    }

    /**
     * 移植自 Vape SilentAura.updateAim：Snap/Smooth 走 RotationManager.setRotations，
     * Adaptive 走 PID（pitchProportionalGain + pitchIntegral）。
     */
    private void applyAim(Rot2f desired) {
        switch (rotationMode.get()) {
            case Snap -> RotationManager.INSTANCE.setRotations(desired, 1000.0, Priority.High);
            case Smooth -> RotationManager.INSTANCE.setRotations(desired, aimSpeed.get(), Priority.High);
            case Adaptive -> {
                // PID（移植自 Vape updateAim PID 分支）
                float curYaw = RotationManager.INSTANCE.getYaw();
                float curPitch = RotationManager.INSTANCE.getPitch();
                float yawError = Mth.wrapDegrees(desired.getYaw() - curYaw);
                float pitchError = Mth.wrapDegrees(desired.getPitch() - curPitch);

                // pitch 积分项（Vape pitchIntegralGain）
                pitchIntegral += pitchError * 0.1f;
                pitchIntegral = Mth.clamp(pitchIntegral, -5.0f, 5.0f);

                // pitchProportionalGain=0.45（Vape 默认）
                float yawAdjust = yawError * 0.45f;
                float pitchAdjust = pitchError * 0.45f + pitchIntegral * 0.05f;

                float nextYaw = curYaw + yawAdjust;
                float nextPitch = Mth.clamp(curPitch + pitchAdjust, -90.0f, 90.0f);
                RotationManager.INSTANCE.setRotations(new Rot2f(nextYaw, nextPitch), aimSpeed.get(), Priority.High);
            }
        }
    }

    /**
     * 移植自 Vape SilentAura 攻击节拍：Perfect Swing 模式等 getAttackStrengthScale==1.0，
     * 否则按 attackCps 计时。
     */
    private boolean shouldAttack(LivingEntity target) {
        double distSqr = target.getEyePosition().distanceToSqr(mc.player.getEyePosition());
        if (distSqr > range.get() * range.get()) return false;

        if (perfectSwing.get() && mc.player.getAttackStrengthScale(0.0f) < 1.0f) {
            return false;
        }

        long now = System.currentTimeMillis();
        long interval = 1000L / Math.max(1, attackCps.get());
        return now - lastAttackTimeMs >= interval;
    }

    private void doAttack(LivingEntity target) {
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastAttackTimeMs = System.currentTimeMillis();

        // switchTargets：攻击后切换到下一个目标（移植自 Vape switchTargets 逻辑）
        if (switchTargets.get() && currentTarget != null) {
            currentTarget = null;
        }
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        pitchIntegral = 0.0f;
        lastAttackTimeMs = 0L;
    }
}
