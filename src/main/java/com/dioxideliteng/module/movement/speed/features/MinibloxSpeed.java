package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.util.krs.MovementUtil;

/**
 * Ported from Krs MinibloxSpeed
 * (com.instrumentalist.krs.hacks.features.movement.speed.features.MinibloxSpeed).
 */
public class MinibloxSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public MinibloxSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "Miniblox";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            MovementUtil.strafe(0.36f);
            if (MovementUtil.isMoving())
                mc.player.input.makeJump();
        } else {
            switch (MovementUtil.fallTicks) {
                case 3:
                    MovementUtil.setVelocityY(-0.2);
                    break;

                case 5:
                    MovementUtil.strafe(0.7f);
                    MovementUtil.setVelocityY(0.3);
                    break;

                case 10:
                    MovementUtil.strafe(0.8f);
                    MovementUtil.setVelocityY(0.2);
                    break;

                case 18:
                    MovementUtil.strafe(0.6f);
                    MovementUtil.setVelocityY(0.2);
                    break;

                default:
                    MovementUtil.strafe(0.3f);
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
