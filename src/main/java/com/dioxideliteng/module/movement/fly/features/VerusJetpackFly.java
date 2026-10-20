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
import net.minecraft.world.effect.MobEffects;

public class VerusJetpackFly implements FlyEvent {

    private final Fly2 fly2;

    public VerusJetpackFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    public static int ticks;

    @Override
    public String getName() {
        return "Verus Jetpack";
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || this.fly2.verusJetpackJumpKeyOnly.getValue() && !InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyJump.saveString()).getValue())) return;

        if (mc.player.isSprinting()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.4, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * 0.4);
        }

        ticks++;

        if (ticks >= 2) {
            mc.player.jumpFromGround();
            ticks = 0;
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
