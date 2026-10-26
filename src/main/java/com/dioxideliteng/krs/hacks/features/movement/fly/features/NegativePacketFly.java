package com.dioxideliteng.krs.hacks.features.movement.fly.features;

import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.movement.InventoryMove;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyEvent;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class NegativePacketFly implements FlyEvent {

    @Override
    public String getName() {
        return "Negative Packet";
    }

    public static Vec3 motion = Vec3.ZERO;

    @Override
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        if (player == null) return;

        double speed = 1;
        double x = 0, y = 0, z = 0;

        if (InventoryMove.canMoveFreely()) {
            if (mc.options.keyUp.isDown()) z += speed;
            if (mc.options.keyDown.isDown()) z -= speed;
            if (mc.options.keyLeft.isDown()) x += speed;
            if (mc.options.keyRight.isDown()) x -= speed;
            if (mc.options.keyJump.isDown()) y += speed;
            if (mc.options.keyShift.isDown()) y -= speed;
        }

        if (x == 0.0 && y == 0.0 && z == 0.0) {
            motion = Vec3.ZERO;
            return;
        }

        motion = rotateVector(x, y, z, player.getYRot());
    }

    @Override
    public void onMotion(MotionEvent event) {
        var player = mc.player;
        if (player == null) return;

        player.setDeltaMovement(0, 0, 0);
    }

    @Override
    public void onTick(TickEvent event) {
        var player = mc.player;
        if (player == null) return;

        Vec3 pos = player.position();
        Vec3 newPos = pos.add(motion);

        player.setPos(newPos.x, newPos.y, newPos.z);
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        PacketUtil.sendPacket(new ServerboundMovePlayerPacket.PosRot(
                newPos.x, newPos.y, newPos.z,
                yaw, pitch,
                true, false
        ));

        PacketUtil.sendPacket(new ServerboundMovePlayerPacket.PosRot(
                newPos.x - 69420, newPos.y - 69420, newPos.z - 69420,
                yaw, pitch,
                false, false
        ));
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        if (mc.player == null) return;

        Packet<?> packet = event.packet;

        if (packet instanceof ClientboundPlayerPositionPacket)
            event.cancel();
    }

    @Override
    public void onBlock(BlockEvent event) {
    }

    private Vec3 rotateVector(double inputX, double inputY, double inputZ, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = inputX * cos - inputZ * sin;
        double z = inputX * sin + inputZ * cos;
        return new Vec3(x, inputY, z);
    }
}
