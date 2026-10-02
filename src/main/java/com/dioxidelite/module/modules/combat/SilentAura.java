package com.dioxidelite.module.modules.combat;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.manager.target.TargetManager;
import com.dioxidelite.manager.target.TargetRequest;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

/**
 * 移植自 Vape-v4 SilentAura。静默瞄准 + 自动攻击。
 * 转头统一走 RotationManager（内置 Vape PID 加速步进，绕过 Grim/Matrix/NCP）。
 */
public final class SilentAura extends Module {

    public static final SilentAura INSTANCE = new SilentAura();

    public enum TargetMode { Distance, Angle, Health, Armor }

    private final EnumSetting<TargetMode> targetMode = add(new EnumSetting<>("Target Mode", TargetMode.Distance));
    private final DoubleSetting range = add(new DoubleSetting("Range", 4.2, 3.0, 6.0, 0.1));
    private final DoubleSetting maxAngle = add(new DoubleSetting("Max Angle", 120.0, 30.0, 360.0, 5.0));
    private final DoubleSetting aimSpeed = add(new DoubleSetting("Aim Speed", 3.0, 0.1, 10.0, 0.1));
    private final DoubleSetting attackSpeed = add(new DoubleSetting("Attack Speed (CPS)", 8.0, 1.0, 20.0, 0.5));
    private final BooleanSetting switchTargets = add(new BooleanSetting("Switch Targets", false));
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    private final BooleanSetting animals = add(new BooleanSetting("Animals", false));
    private final BooleanSetting invisibles = add(new BooleanSetting("Invisibles", false));
    private final BooleanSetting showTarget = add(new BooleanSetting("Show Target", false));
    private final ColorSetting targetColor = add(new ColorSetting("Target Color", new Color(255, 200, 112, 180)));
    private final BooleanSetting perfectSwing = add(new BooleanSetting("Perfect Swing", true, "Only attack when cooldown ready"));

    private LivingEntity target;
    private long lastAttackTime = 0L;

    private SilentAura() {
        super("Silent Aura", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        target = null;
        RotationManager.INSTANCE.releaseSilentRotation(this);
    }

    @Listen
    private void onTick(PlayerTickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        updateTarget();

        if (target != null) {
            rotateToTarget();
            tryAttack();
        } else {
            RotationManager.INSTANCE.releaseSilentRotation(this);
        }
    }

    private void updateTarget() {
        double r = range.get();
        TargetRequest request = TargetRequest.builder()
                .range(r)
                .players(players.get())
                .mobs(mobs.get())
                .animals(animals.get())
                .invisibles(invisibles.get())
                .maxTargets(10)
                .build();

        List<LivingEntity> candidates = TargetManager.INSTANCE.acquireTargets(request);
        candidates.removeIf(e -> !isValidTarget(e));

        if (candidates.isEmpty()) {
            target = null;
            return;
        }

        Comparator<LivingEntity> comparator = switch (targetMode.get()) {
            case Angle -> Comparator.comparingDouble(e -> {
                Rot2f rot = RotationUtils.getRotationsToEntity(e);
                return Math.abs(rot.getYaw() - mc.player.getYRot());
            });
            case Health -> Comparator.comparingDouble(e -> e instanceof LivingEntity le ? le.getHealth() : 20.0);
            case Armor -> Comparator.comparingDouble(e -> e instanceof Player p ? p.getArmorValue() : 0);
            default -> Comparator.comparingDouble(RotationUtils::getEyeDistanceToEntity);
        };
        candidates.sort(comparator);

        LivingEntity newTarget = candidates.getFirst();
        if (target != null && !newTarget.equals(target) && !switchTargets.get()) {
            if (candidates.contains(target)) {
                return;
            }
        }
        target = newTarget;
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e == null || !e.isAlive() || e.isRemoved()) return false;
        if (e == mc.player) return false;
        if (FriendManager.INSTANCE.isFriend(e)) return false;
        if (RotationUtils.getEyeDistanceToEntity(e) > range.get()) return false;
        Rot2f rot = RotationUtils.getRotationsToEntity(e);
        double angleDiff = Math.abs(((rot.getYaw() - mc.player.getYRot() + 540) % 360) - 180);
        if (angleDiff > maxAngle.get() / 2.0) return false;
        return true;
    }

    private void rotateToTarget() {
        Rot2f targetRot = RotationUtils.calculate(target, true, range.get());
        // 统一走 RotationManager，内部已用 Vape PID 加速步进
        RotationManager.INSTANCE.setRotations(targetRot, aimSpeed.get(), Priority.High);
    }

    private void tryAttack() {
        if (mc.gameMode == null) return;
        float cooldown = mc.player.getAttackStrengthScale(0f);
        if (perfectSwing.get() && cooldown < 1.0f) return;

        // CPS 间隔 + 随机抖动（默认启用，绕过 Matrix/NCP 的固定攻击频率检测）
        long baseInterval = (long) (1000.0 / attackSpeed.get());
        long interval = baseInterval + (long) ((Math.random() - 0.5) * baseInterval * 0.4);
        if (System.currentTimeMillis() - lastAttackTime < interval) return;

        // 转头必须对准目标（容差内）才攻击，避免打空气被检测
        if (!RotationUtils.isInFov(target, 8.0)) return;

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (!showTarget.get() || target == null) return;
        try {
            Color c = targetColor.get();
            com.dioxidelite.util.render.esp.CircleESP.render(
                    event.getPoseStack(), target,
                    target.getBbWidth() * 0.6f,
                    c, c, 1.0f);
        } catch (Throwable ignored) {
        }
    }
}
