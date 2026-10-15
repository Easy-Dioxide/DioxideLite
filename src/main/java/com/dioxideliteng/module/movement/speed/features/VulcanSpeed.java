package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;

public class VulcanSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public VulcanSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "Vulcan";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (!MovementUtil.isMoving()) {
            return;
        }

        if (mc.player.onGround()) {
            MovementUtil.strafe(MovementUtil.getSpeed());
            mc.player.input.makeJump();
            return;
        }

        switch (MovementUtil.fallTicks) {
            case 2, 9:
                MovementUtil.strafe(MovementUtil.getSpeed());
                break;
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(EventTick event) {
        if (mc.player != null && MovementUtil.isMoving())
            mc.options.keyJump.setDown(false);
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
    }
}
