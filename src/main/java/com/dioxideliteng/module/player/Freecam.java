package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.Freecam).
 *
 * Adaptations:
 *  - Krs static FloatValue settings mapped to instance NumberSettings (settings are per-Feature
 *    here); the movement math reads them through the instance while the camera state stays static
 *    so the public static API is preserved verbatim;
 *  - Krs onWorld (WorldEvent) -> Events.WORLD, onUpdate (UpdateEvent) -> Events.UPDATE;
 *  - {@code InputConstants.isKeyDown(window, key)} -> {@code InputConstants.isKeyDown(key)}
 *    (26.3 single-argument signature);
 *  - the camera detach itself needs a camera mixin that does not exist in this client, so the
 *    visual freecam is inert here; the module state, camera-position math and public API are
 *    ported verbatim for a hook site to call.
 */
public class Freecam extends Feature {

    private static Freecam instance;

    private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", this, 2, 0.1, 4, 0.1);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", this, 1, 0.1, 4, 0.1);

    private static boolean canFly = false;
    private static Vec3 camPos = Vec3.ZERO;
    private static Vec3 prevCamPos = Vec3.ZERO;
    private static float camYaw = 0f;
    private static float camPitch = 0f;

    public Freecam() {
        super("Freecam", Category.PLAYER);
        instance = this;
    }

    @Override
    public void onDisable() {
        if (canFly)
            reloadChunks();
        canFly = false;
    }

    @Override
    public void onEnable() {
        var player = mc.player;
        if (player == null) return;

        camPos = player.getEyePosition();
        prevCamPos = camPos;
        camYaw = player.getYRot();
        camPitch = player.getXRot();
        canFly = true;
        reloadChunks();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.WORLD) {
            toggle();
        } else if (event == Events.UPDATE) {
            onUpdate();
        }
    }

    private void onUpdate() {
        if (mc.player == null) return;

        if (!canFly || mc.gui.screen() != null) {
            prevCamPos = camPos;
            return;
        }

        Vec3 moveVector = getKeyboardMoveVector();
        double yawRad = Math.toRadians(camYaw);
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);
        double offsetX = moveVector.x * cosYaw - moveVector.y * sinYaw;
        double offsetZ = moveVector.x * sinYaw + moveVector.y * cosYaw;

        double offsetY = 0.0;
        if (isKeyDown(mc.options.keyJump))
            offsetY += verticalSpeed.getValue();
        if (isKeyDown(mc.options.keyShift))
            offsetY -= verticalSpeed.getValue();

        Vec3 offset = new Vec3(offsetX, 0.0, offsetZ)
                .scale(horizontalSpeed.getValue())
                .add(0.0, offsetY, 0.0);

        prevCamPos = camPos;
        camPos = camPos.add(offset);
    }

    private Vec3 getKeyboardMoveVector() {
        double left = 0.0;
        double forward = 0.0;

        if (isKeyDown(mc.options.keyLeft)) left += 1.0;
        if (isKeyDown(mc.options.keyRight)) left -= 1.0;
        if (isKeyDown(mc.options.keyUp)) forward += 1.0;
        if (isKeyDown(mc.options.keyDown)) forward -= 1.0;

        double length = Math.sqrt(left * left + forward * forward);
        if (length > 1.0) {
            left /= length;
            forward /= length;
        }

        return new Vec3(left, forward, 0.0);
    }

    private boolean isKeyDown(KeyMapping key) {
        return InputConstants.isKeyDown(
                InputConstants.getKey(key.saveString()).getValue()
        );
    }

    private void reloadChunks() {
        if (mc.level != null)
            mc.levelExtractor.allChanged();
    }

    public static boolean getCanFly() {
        return canFly;
    }

    public static void setCanFly(boolean value) {
        canFly = value;
    }

    public static Vec3 getCamPos(float partialTicks) {
        return new Vec3(
                Mth.lerp((double) partialTicks, prevCamPos.x, camPos.x),
                Mth.lerp((double) partialTicks, prevCamPos.y, camPos.y),
                Mth.lerp((double) partialTicks, prevCamPos.z, camPos.z)
        );
    }

    public static float getCamYaw() {
        return camYaw;
    }

    public static float getCamPitch() {
        return camPitch;
    }

    public static void turn(double deltaYaw, double deltaPitch) {
        camYaw += (float) (deltaYaw * 0.15);
        camPitch += (float) (deltaPitch * 0.15);
        camPitch = Mth.clamp(camPitch, -90f, 90f);
    }

    public static Vec3 getScaledCamDir(double scale) {
        return Vec3.directionFromRotation(camPitch, camYaw).scale(scale);
    }

}
