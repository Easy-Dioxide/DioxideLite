package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;
import com.mojang.blaze3d.platform.InputConstants;

public class VanillaSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public VanillaSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "Vanilla";
    }

    public static boolean cancelledOnce = false;

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (this.speed2.vanillaAutoBHop.getValue() && mc.player.onGround() && MovementUtil.isMoving())
            mc.player.input.makeJump();

        MovementUtil.strafe((float) this.speed2.vanillaSpeed.getValue());
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (this.speed2.vanillaAutoBHop.getValue() && mc.player.onGround() && MovementUtil.isMoving()) {
            mc.options.keyJump.setDown(false);
            cancelledOnce = true;
        } else if (cancelledOnce && mc.player.onGround()) {
            mc.options.keyJump.setDown(InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue()));
            cancelledOnce = false;
        }
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
    }
}
