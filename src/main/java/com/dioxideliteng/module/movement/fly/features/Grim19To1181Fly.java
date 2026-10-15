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
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.TimerUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class Grim19To1181Fly implements FlyEvent {

    private final Fly2 fly2;

    public Grim19To1181Fly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    private boolean waitingVelocity;
    private boolean boost;

    @Override
    public String getName() {
        return "Grim 1.9-1.18.1";
    }

    public void reset() {
        waitingVelocity = false;
        boost = false;
    }

    public void disable() {
        TimerUtil.reset();
        reset();
    }

    @Override
    public void onUpdate(EventUpdate event) {
        var player = mc.player;
        if (player == null) return;

        TimerUtil.timerSpeed = (float) this.fly2.grimTimerSpeed.getValue();

        if (!waitingVelocity && player.fallDistance > 0.0F) {
            PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
            player.fallDistance = 0.0F;
            waitingVelocity = true;
        }

        if (waitingVelocity)
            player.setDeltaMovement(0.0, 0.0, 0.0);

        if (boost) {
            MovementUtil.setSpeed(this.fly2.grimSpeed.getValue());
            MovementUtil.setVelocityY(this.fly2.grimSlowFall.getValue() ? -0.00011 : -0.0002);
            boost = false;
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (waitingVelocity)
            event.cancel();
    }

    @Override
    public void onTick(EventTick event) {
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
        var player = mc.player;
        if (player == null) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.id() == player.getId()) {
            waitingVelocity = false;
            boost = true;
            event.setCancelled(true);
        }
    }

    @Override
    public void onBlock(EventBlock event) {
    }
}
