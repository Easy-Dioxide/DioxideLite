package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.world.effect.MobEffects;

public class VerusSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public VerusSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "Verus";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            if (MovementUtil.isMoving())
                mc.player.jumpFromGround();

            if (mc.player.hasEffect(MobEffects.SPEED))
                MovementUtil.strafe(0.53f);
            else MovementUtil.strafe(0.48f);
        } else {
            if (mc.player.hasEffect(MobEffects.SPEED))
                MovementUtil.strafe(0.38f);
            else MovementUtil.strafe(0.33f);
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
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
