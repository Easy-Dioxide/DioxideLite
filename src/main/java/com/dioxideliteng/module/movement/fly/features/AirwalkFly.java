package com.dioxideliteng.module.movement.fly.features;

import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.fly.FlyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.phys.shapes.Shapes;

public class AirwalkFly implements FlyEvent {

    private final Fly2 fly2;

    public AirwalkFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    @Override
    public String getName() {
        return "Airwalk";
    }

    @Override
    public void onUpdate(EventUpdate event) {
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
        if (mc.player == null || InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue()) || event.getBlockPos().getY() != mc.player.blockPosition().below().getY()) return;

        event.setVoxelShape(Shapes.block());
    }
}
