package com.dioxidelite.module.modules.movement;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（来源开源版）com/dioxidelite/module/modules/movement/MovementFix.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.events.KeyboardInputEvent;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import net.minecraft.util.Mth;

public final class MovementFix extends Module {

    public static final MovementFix INSTANCE = new MovementFix();

    public enum Mode {
        Setting,
        Packet,
        SprintOnly,
        Off,
        Strict,
        Silent,
        ChangeLook,
        // [Vape 移植] 三个 Vape movementCorrection 选项。
        // VapeNone  = ClientSettings.NO_MOVEMENT_CORRECTION（完全不修正）。
        // VapeSlow  = ClientSettings.SLOW_MOVEMENT_CORRECTION（yaw+180° 反向 + 重映射按键，减速防不规则速度）。
        // VapeProper= ClientSettings.PROPER_MOVEMENT_CORRECTION（yaw=静默yaw + 重映射 W/A/S/D 到 45° 桶，等价于 Silent）。
        VapeNone,
        VapeSlow,
        VapeProper
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Silent));
    private final BooleanSetting packetOnly = add(new BooleanSetting("Packet Only", false)
            .visibleWhen(() -> mode.is(Mode.Packet) || isSprintOnlyMode()));

    private MovementFix() {
        super("Movement Fix", Category.MOVEMENT);
        setEnabled(true);
    }

    public boolean shouldFixInput() {
        return mode.is(Mode.Setting)
                || mode.is(Mode.Silent)
                || mode.is(Mode.VapeProper)
                || mode.is(Mode.VapeSlow)
                || (mode.is(Mode.Packet) && !packetOnly.get());
    }

    public boolean shouldFixRotationYaw() {
        return mode.is(Mode.Setting)
                || mode.is(Mode.Packet)
                || mode.is(Mode.SprintOnly)
                || mode.is(Mode.Strict)
                || mode.is(Mode.Silent)
                || mode.is(Mode.ChangeLook)
                || mode.is(Mode.VapeProper)
                || mode.is(Mode.VapeSlow);
    }

    public boolean shouldChangeLook() {
        return mode.is(Mode.ChangeLook);
    }

    public boolean shouldFixPacketOnly() {
        return mode.is(Mode.Packet) && packetOnly.get();
    }

    public boolean shouldPreventLocalSprint() {
        return (isSprintOnlyMode() || mode.is(Mode.Strict) || mode.is(Mode.Silent))
                && !packetOnly.get() && hasLargeRotationDelta();
    }

    public boolean shouldSuppressSprintPacket() {
        return (isSprintOnlyMode() || mode.is(Mode.Strict) || mode.is(Mode.Silent)) && hasLargeRotationDelta();
    }

    public boolean hasLargeRotationDelta() {
        if (!isEnabled() || mode.is(Mode.Off) || mode.is(Mode.VapeNone) || noPlayer() || !RotationManager.INSTANCE.isActive()) {
            return false;
        }
        float currentMoveYaw = getMoveYaw(mc.player.getYRot(), currentForward(), currentStrafe());
        float packetMoveYaw = getMoveYaw(RotationManager.INSTANCE.getYaw(), currentForward(), currentStrafe());
        return Math.abs(Mth.wrapDegrees(packetMoveYaw - currentMoveYaw)) > 45.005F;
    }

    private float getDirection(float forward, float strafe) {
        float direction = mc.player.getYRot();

        boolean isMovingForward = forward > 0.0F;
        boolean isMovingBack = forward < 0.0F;
        boolean isMovingRight = strafe > 0.0F;
        boolean isMovingLeft = strafe < 0.0F;
        boolean isMovingSideways = isMovingRight || isMovingLeft;
        boolean isMovingStraight = isMovingForward || isMovingBack;

        if (forward != 0.0F || strafe != 0.0F) {
            if (isMovingBack && !isMovingSideways) {
                return direction + 180.0F;
            }
            if (isMovingForward && isMovingLeft) {
                return direction + 45.0F;
            }
            if (isMovingForward && isMovingRight) {
                return direction - 45.0F;
            }
            if (!isMovingStraight && isMovingLeft) {
                return direction + 90.0F;
            }
            if (!isMovingStraight && isMovingRight) {
                return direction - 90.0F;
            }
            if (isMovingBack && isMovingLeft) {
                return direction + 135.0F;
            }
            if (isMovingBack) {
                return direction - 135.0F;
            }
        }

        return direction;
    }

    public static float getMoveYaw(float yaw, float forward, float strafe) {
        if (forward < 0.0F) {
            yaw += 180.0F;
        }

        float factor = 1.0F;
        if (forward < 0.0F) {
            factor = -0.5F;
        } else if (forward > 0.0F) {
            factor = 0.5F;
        }

        if (strafe > 0.0F) {
            yaw -= 90.0F * factor;
        } else if (strafe < 0.0F) {
            yaw += 90.0F * factor;
        }

        return yaw;
    }

    public void fixMovement(KeyboardInputEvent event, float yaw) {
        if (!shouldFixInput()) return;

        // [Vape 移植] SLOW_MOVEMENT_CORRECTION：把目标 yaw 反向 180°，
        // 再把量化后的输入幅度减半，模拟 Vape "减速防止不规则速度" 的语义。
        if (mode.is(Mode.VapeSlow)) {
            quantizeToAngle(event, yaw + 180.0F);
            event.setForward(event.getForward() * 0.5F);
            event.setStrafe(event.getStrafe() * 0.5F);
            return;
        }
        // [Vape 移植] PROPER_MOVEMENT_CORRECTION：目标=静默 yaw，幅度不变（与现有 Silent 等价）。
        if (mode.is(Mode.VapeProper)) {
            quantizeToAngle(event, yaw);
            return;
        }

        quantizeToAngle(event, yaw);
    }

    /**
     * 把 forward/strafe 重新量化到最近的 45° 桶，使移动方向对齐 {@code yaw}。
     * 来自原 fixMovement 的核心算法，Vape Proper/Slow 与原 Silent/Setting 共用。
     */
    private void quantizeToAngle(KeyboardInputEvent event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();

        int angleUnit = 45;
        float angleTolerance = 22.5F;
        float directionFactor = Math.max(Math.abs(forward), Math.abs(strafe));
        double angleDifference = Mth.wrapDegrees(getDirection(forward, strafe) - yaw);
        double angleDistance = Math.abs(angleDifference);

        forward = 0.0F;
        strafe = 0.0F;

        if (angleDistance <= (double) ((float) angleUnit + angleTolerance)) {
            forward++;
        } else if (angleDistance >= (double) (180.0F - (float) angleUnit - angleTolerance)) {
            forward--;
        }

        if (angleDifference >= (double) ((float) angleUnit - angleTolerance)
                && angleDifference <= (double) (180.0F - (float) angleUnit + angleTolerance)) {
            strafe--;
        } else if (angleDifference <= (double) ((float) (-angleUnit) + angleTolerance)
                && angleDifference >= (double) (-180.0F + (float) angleUnit - angleTolerance)) {
            strafe++;
        }

        forward *= directionFactor;
        strafe *= directionFactor;

        event.setForward(forward);
        event.setStrafe(strafe);
    }

    private float currentForward() {
        if (mc.player.input.keyPresses.forward()) return 1.0F;
        if (mc.player.input.keyPresses.backward()) return -1.0F;
        return 0.0F;
    }

    private float currentStrafe() {
        if (mc.player.input.keyPresses.left()) return 1.0F;
        if (mc.player.input.keyPresses.right()) return -1.0F;
        return 0.0F;
    }

    private boolean isSprintOnlyMode() {
        return mode.is(Mode.SprintOnly);
    }
}
