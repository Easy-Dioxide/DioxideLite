package com.dioxideliteng.krs.hacks.features.movement.fly.features;

import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyModule;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyEvent;
import com.dioxideliteng.krs.utils.math.TimerUtil;
import com.dioxideliteng.krs.utils.move.MovementUtil;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class Grim19To1181Fly implements FlyEvent {
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
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        if (player == null) return;

        TimerUtil.timerSpeed = FlyModule.grimTimerSpeed.get();

        if (!waitingVelocity && player.fallDistance > 0.0F) {
            PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
            player.fallDistance = 0.0F;
            waitingVelocity = true;
        }

        if (waitingVelocity)
            player.setDeltaMovement(0.0, 0.0, 0.0);

        if (boost) {
            MovementUtil.setSpeed(FlyModule.grimSpeed.get());
            MovementUtil.setVelocityY(FlyModule.grimSlowFall.get() ? -0.00011 : -0.0002);
            boost = false;
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (waitingVelocity)
            event.cancel();
    }

    @Override
    public void onTick(TickEvent event) {
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        var player = mc.player;
        if (player == null) return;

        Packet<?> packet = event.packet;
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.id() == player.getId()) {
            waitingVelocity = false;
            boost = true;
            event.cancel();
        }
    }

    @Override
    public void onBlock(BlockEvent event) {
    }
}
