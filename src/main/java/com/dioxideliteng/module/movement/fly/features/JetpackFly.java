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
import com.mojang.blaze3d.platform.InputConstants;

public class JetpackFly implements FlyEvent {

    private final Fly2 fly2;

    public JetpackFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    @Override
    public String getName() {
        return "Jetpack";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue()) && Fly2.canMoveFreely()) {
            MovementUtil.setVelocityY(mc.player.getDeltaMovement().y + 0.14);
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
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
