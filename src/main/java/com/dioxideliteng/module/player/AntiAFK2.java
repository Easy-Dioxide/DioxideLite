package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.RotationManager;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

import java.util.Locale;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.AntiAFK).
 *
 * Adaptations:
 *  - ListValue/IntValue/BooleanValue mapped to ModeSetting/NumberSetting/BooleanSetting;
 *  - {@code Client.notificationManager.addNotification} replaced with DynamicIslandManager.notifyInfo;
 *  - {@code Client.rotationManager} replaced with RotationManager.INSTANCE;
 *  - Krs onUpdate -> Events.UPDATE; Krs onWorld -> Events.WORLD; tag() -> setSuffix.
 */
public class AntiAFK2 extends Feature {
    private final ModeSetting action = new ModeSetting("Action", this, "Cycle", new String[]{"Swing", "Rotate", "Jump", "Cycle"});
    private final NumberSetting interval = new NumberSetting("Interval", this, 5, 1, 300, 1);
    private final BooleanSetting pauseWhileHurt = new BooleanSetting("Pause While Hurt", this, true);
    private final BooleanSetting notifications = new BooleanSetting("Notifications", this, false);

    private int idleTicks;
    private int cycleIndex;
    private float lastYaw;
    private float lastPitch;
    private boolean initialized;
    private boolean rotateRight;

    public AntiAFK2() {
        super("AntiAFK2", Category.PLAYER);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.UPDATE) {
            setSuffix(action.getValue());
            onUpdate();
        } else if (event == Events.WORLD) {
            reset();
        }
    }

    private void onUpdate() {
        var player = mc.player;
        if (player == null || player.isDeadOrDying()) {
            reset();
            return;
        }

        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (!initialized) {
            lastYaw = yaw;
            lastPitch = pitch;
            initialized = true;
            return;
        }

        boolean cameraMoved = Math.abs(Mth.wrapDegrees(yaw - lastYaw)) > 0.05f
                || Math.abs(pitch - lastPitch) > 0.05f;
        lastYaw = yaw;
        lastPitch = pitch;

        if (hasManualInput() || cameraMoved || pauseWhileHurt.getValue() && player.hurtTime > 0) {
            idleTicks = 0;
            return;
        }

        if (++idleTicks < (int) (interval.getValue() * 20))
            return;

        performConfiguredAction();
        idleTicks = 0;
    }

    private boolean hasManualInput() {
        return mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown()
                || mc.options.keyJump.isDown()
                || mc.options.keyShift.isDown()
                || mc.options.keySprint.isDown()
                || mc.options.keyAttack.isDown()
                || mc.options.keyUse.isDown();
    }

    private void performConfiguredAction() {
        String selectedAction = action.getValue().toLowerCase(Locale.ROOT);
        if (selectedAction.equals("cycle")) {
            selectedAction = switch (cycleIndex++ % 3) {
                case 1 -> "rotate";
                case 2 -> "jump";
                default -> "swing";
            };
        }

        switch (selectedAction) {
            case "rotate" -> rotate();
            case "jump" -> jump();
            default -> swing();
        }

        if (notifications.getValue())
            DynamicIslandManager.notifyInfo("Anti AFK", "Performed " + selectedAction);
    }

    private void swing() {
        if (mc.player != null)
            mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
    }

    private void rotate() {
        var player = mc.player;
        if (player == null)
            return;

        float offset = rotateRight ? 3.0f : -3.0f;
        rotateRight = !rotateRight;
        float currentYaw = player.getYRot();
        float targetYaw = currentYaw + offset;
        targetYaw = RotationManager.INSTANCE.normalizeRotation(
                targetYaw,
                player.getXRot(),
                currentYaw,
                player.getXRot()
        )[0];

        player.turn(Mth.wrapDegrees(targetYaw - currentYaw) / 0.15D, 0.0D);
    }

    private void jump() {
        if (mc.player != null)
            mc.player.input.makeJump();
    }

    private void reset() {
        idleTicks = 0;
        cycleIndex = 0;
        lastYaw = 0.0f;
        lastPitch = 0.0f;
        initialized = false;
        rotateRight = false;
    }
}
