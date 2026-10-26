package com.dioxideliteng.krs.hacks.features.player;



import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class SpinBot extends Module {

    public SpinBot() {
        super("Spin Bot", ModuleCategory.Player, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private final FloatValue spinSpeed = new FloatValue(
            "Spin Speed",
            30f,
            -40f,
            40f
    );

    @Setting
    private final FloatValue pitch = new FloatValue(
            "Pitch",
            90f,
            -90f,
            90f
    );

    private static float spinYaw = 0f;

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        spinYaw = 0f;
        KrsClient.rotationManager.stopRotation();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (spinYaw >= 360f || spinYaw <= -360f)
            spinYaw = 0f;

        spinYaw += spinSpeed.get();

        KrsClient.rotationManager.startRotation(spinYaw, pitch.get(), 180f);
    }
}
