package com.dioxideliteng.krs.hacks.features.movement.fly.features;



import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyEvent;

public class CreativeFly implements FlyEvent {

    @Override
    public String getName() {
        return "Creative";
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        mc.player.getAbilities().mayfly = true;
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

    @Override
    public void onBlock(BlockEvent event) {
    }
}