package com.dioxideliteng.module.movement.fly.features;

import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.fly.FlyEvent;
import com.dioxideliteng.util.krs.MovementUtil;

public class VulcanGlideFly implements FlyEvent {

    private final Fly2 fly2;

    public VulcanGlideFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    public static int ticks;

    @Override
    public String getName() {
        return "Vulcan Glide";
    }

    @Override
    public void onUpdate(EventUpdate event) {
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
    public void onTick(EventTick event) {
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
