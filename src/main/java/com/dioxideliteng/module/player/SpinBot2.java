package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.RotationManager;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.SpinBot).
 *
 * Adaptations:
 *  - FloatValue mapped to NumberSetting;
 *  - {@code Client.rotationManager} replaced with RotationManager.INSTANCE (the rotation is
 *    applied to the outgoing packets by the shared rotation chain in MixinLocalPlayer);
 *  - Krs onUpdate -> Events.UPDATE.
 */
public class SpinBot2 extends Feature {

    private final NumberSetting spinSpeed = new NumberSetting("Spin Speed", this, 30, -40, 40, 1);
    private final NumberSetting pitch = new NumberSetting("Pitch", this, 90, -90, 90, 1);

    private static float spinYaw = 0f;

    public SpinBot2() {
        super("SpinBot2", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        spinYaw = 0f;
        RotationManager.INSTANCE.stopRotation();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.UPDATE) return;
        if (mc.player == null) return;

        if (spinYaw >= 360f || spinYaw <= -360f)
            spinYaw = 0f;

        spinYaw += (float) spinSpeed.getValue();

        RotationManager.INSTANCE.startRotation(spinYaw, (float) pitch.getValue(), 180f);
    }
}
