package com.dioxideliteng.krs.hacks.features.movement.speed.features;



import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.movement.speed.SpeedModule;
import com.dioxideliteng.krs.hacks.features.movement.speed.SpeedEvent;
import com.dioxideliteng.krs.utils.move.MovementUtil;

public class SmoothVanillaSpeed implements SpeedEvent {

    @Override
    public String getName() {
        return "Smooth Vanilla";
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (!mc.player.onGround())
            MovementUtil.smoothStrafe(SpeedModule.vanillaSpeed.get());
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(TickEvent event) {
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
    }
}