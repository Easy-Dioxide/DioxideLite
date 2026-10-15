package com.dioxideliteng.module.movement.fly.features;

import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.fly.FlyEvent;

public class ModernMMCFly implements FlyEvent {

    private final Fly2 fly2;

    public ModernMMCFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    @Override
    public String getName() {
        return "Modern MMC";
    }

    public static int tick = 0;

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        tick++;

        if (tick >= 40) {
            tick = 0;
        } else {
            event.setCancelled(true);
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
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (mc.player.onGround())
            mc.options.keyJump.setDown(false);
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
    }

    @Override
    public void onBlock(EventBlock event) {
    }
}
