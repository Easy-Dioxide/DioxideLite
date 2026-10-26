package com.dioxideliteng.krs.hacks.features.movement.fly.features;

import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyEvent;

public class ModernMMCFly implements FlyEvent {

    @Override
    public String getName() {
        return "Modern MMC";
    }

    public static int tick = 0;

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        tick++;

        if (tick >= 40) {
            tick = 0;
        } else {
            event.cancel();
            if (mc.player.tickCount % 4 == 0) {
                mc.player.jumpFromGround();
            }
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (tick <= 40 - 2) {
            event.cancel();
        }
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        if (mc.player.onGround())
            mc.options.keyJump.setDown(false);
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