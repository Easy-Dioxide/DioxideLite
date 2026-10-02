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
import com.dioxidelite.util.rotation.AdaptiveRotationController;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

/**
 * 移植自 Vape-v4 SilentAura。静默瞄准 + 自动攻击：
 * - 目标选择支持 Distance / Angle / Health / Armor
 * - 旋转框架可切换：Vape(PID via AdaptiveRotationController) 或 DioxideLite(RotationManager)
 * - 服务端静默转头，客户端相机不动
 */
public final class SilentAura extends Module {

    public static final SilentAura INSTANCE = new SilentAura();

    public enum TargetMode { Distance, Angle, Health, Armor }
    public enum RotationMode { Vape, DioxideLite }

    private final EnumSetting<TargetMode> targetMode = add(new EnumSetting<>("Target Mode", TargetMode.Distance));
    private final EnumSetting<RotationMode> rotationMode = add(new EnumSetting<>("Rotation Mode", RotationMode.Vape));
    private final DoubleSetting range = add(new DoubleSetting("Range", 4.2, 3.0, 6.0, 0.1));
    private final DoubleSetting maxAngle = add(new DoubleSetting("Max Angle", 120.0, 30.0, 360.0, 5.0));
    private final DoubleSetting aimSpeed = add(new DoubleSetting("Aim Speed", 1.0, 0.1, 10.0, 0.1));
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
    private final AdaptiveRotationController pidController = new AdaptiveRotationController();

    private SilentAura() {
        super("Silent Aura", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        target = null;
        pidController.clearTarget();
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
            pidController.clearTarget();
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
            // keep current target if still valid
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
        if (rotationMode.get() == RotationMode.Vape) {
            pidController.setSpeed(aimSpeed.get().floatValue());
            pidController.setTarget(target.position().add(0, target.getBbHeight() / 2.0, 0));
            pidController.update();
            RotationManager.INSTANCE.setRotations(
                    new Rot2f(pidController.getCurrentYaw(), pidController.getCurrentPitch()),
                    180.0, Priority.High);
        } else {
            RotationManager.INSTANCE.setRotations(targetRot, aimSpeed.get(), Priority.High);
        }
    }

    private void tryAttack() {
        if (mc.gameMode == null) return;
        float cooldown = mc.player.getAttackStrengthScale(0f);
        if (perfectSwing.get() && cooldown < 1.0f) return;

        long interval = (long) (1000.0 / attackSpeed.get());
        if (System.currentTimeMillis() - lastAttackTime < interval) return;

        // raytrace check
        Rot2f cur = new Rot2f(
                RotationManager.INSTANCE.getYaw(),
                RotationManager.INSTANCE.getPitch());
        if (!RotationUtils.isInFov(target, 10.0)) {
            // still try if within small angle
        }

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
