package com.dioxideliteng.krs.hacks.features.movement.fly.features;

import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyEvent;
import com.dioxideliteng.krs.utils.move.MovementUtil;

public class VulcanGlideFly implements FlyEvent {

    public static int ticks;

    @Override
    public String getName() {
        return "Vulcan Glide";
    }

    @Override
    public void onUpdate(UpdateEvent event) {
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (!mc.player.onGround() & mc.player.getDeltaMovement().y < 0.0) {
            MovementUtil.setVelocityY(ticks % 2 == 0 ? -0.17 : -0.10);
            ticks++;
        }
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

    @Override
    public void onBlock(BlockEvent event) {
    }
}