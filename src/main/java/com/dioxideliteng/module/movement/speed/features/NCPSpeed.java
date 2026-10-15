package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;

public class NCPSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public NCPSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "NCP";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.player.isInWater()) return;

        if (MovementUtil.isMoving()) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
                MovementUtil.strafe((float) Math.max(
                        0.47f + MovementUtil.getSpeedEffect() * 0.1,
                        MovementUtil.getBaseMoveSpeed(0.2873)
                ));
            } else {
                MovementUtil.strafe(Math.max(0.24f, MovementUtil.getSpeed()));
            }
        }
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
