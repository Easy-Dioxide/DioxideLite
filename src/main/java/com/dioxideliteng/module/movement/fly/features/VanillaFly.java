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

public class VanillaFly implements FlyEvent {

    private final Fly2 fly2;

    public VanillaFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    @Override
    public String getName() {
        return "Vanilla";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        float yMotion = 0f;

        if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue()) && Fly2.canMoveFreely())
            yMotion += this.fly2.vanillaVSpeed.getValue();

        if (InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue()) && Fly2.canMoveFreely())
            yMotion -= this.fly2.vanillaVSpeed.getValue();

        MovementUtil.setVelocityY(yMotion);
        MovementUtil.strafe((float) this.fly2.vanillaHSpeed.getValue());
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        mc.options.keyShift.setDown(false);
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
