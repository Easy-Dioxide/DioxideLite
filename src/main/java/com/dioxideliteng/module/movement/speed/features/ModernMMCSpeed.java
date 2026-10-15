package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;

public class ModernMMCSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public ModernMMCSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    public static int tick1;
    public static int tick2;

    @Override
    public String getName() {
        return "Modern MMC";
    }

    @Override
    public void onUpdate(EventUpdate event) {
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (MovementUtil.isMoving()) {
            if (mc.player.onGround()) {
                mc.player.input.makeJump();
            }
        }
    }

    @Override
    public void onTick(EventTick event) {
        if (mc.player != null && mc.player.onGround() && MovementUtil.isMoving())
            mc.options.keyJump.setDown(false);
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
    }
}
