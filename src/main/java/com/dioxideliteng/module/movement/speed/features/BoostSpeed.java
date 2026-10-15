package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;

public class BoostSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public BoostSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "Boost";
    }

    public static float boostSpeed = 0f;

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.player.isInWater()) return;

        if (MovementUtil.isMoving()) {
            if (mc.player.onGround()) {
                MovementUtil.strafe(Math.max(0.24f, boostSpeed));
                mc.player.input.makeJump();
                if (!mc.player.horizontalCollision)
                    boostSpeed = Math.min(boostSpeed + 0.1f, 1f);
            } else MovementUtil.strafe(Math.max(0.24f, MovementUtil.getSpeed()));
        } else boostSpeed = 0f;
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (mc.player.onGround() && MovementUtil.isMoving())
            mc.options.keyJump.setDown(false);
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
    }
}
