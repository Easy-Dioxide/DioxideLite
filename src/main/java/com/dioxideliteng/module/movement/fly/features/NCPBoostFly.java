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
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NCPBoostFly implements FlyEvent {
    public static float moveSpeed;
    public static boolean timer;

    private final Fly2 fly2;

    public NCPBoostFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    @Override
    public String getName() {
        return "NCP Boost";
    }

    @Override
    public void onUpdate(EventUpdate event) {
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        event.callInPost = true;

        if (!event.post) {
            if (timer && !mc.player.onGround()) {
                TimerUtil.reset();
                timer = false;
            }

            event.onGround = true;
            if (!MovementUtil.hasXZMotion() && moveSpeed > 0.25f) {
                moveSpeed = 0.25f;
            } else {
                moveSpeed -= moveSpeed / 169f;
            }

            MovementUtil.strafe((float) Math.max(
                    moveSpeed + MovementUtil.getSpeedEffect() * 0.1,
                    MovementUtil.getBaseMoveSpeed(0.2873)
            ));
            MovementUtil.setVelocityY(-8E-6);
        } else {
            if (mc.player.onGround()) {
                if (this.fly2.ncpDamageBoost.getValue()) {
                    for (int i = 0; i < 65; i++) {
                        PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.position().x, mc.player.position().y + 0.049, mc.player.position().z, false, false));
                        PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.position().x, mc.player.position().y, mc.player.position().z, false, false));
                    }
                    PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.position().x, mc.player.position().y, mc.player.position().z, true, false));
                }

                TimerUtil.timerSpeed = 0.3f;
                mc.player.input.makeJump();
                MovementUtil.stopXZ();
                moveSpeed = 1.61f;
                timer = true;
            }
        }
    }

    @Override
    public void onTick(EventTick event) {
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
        if (mc.player == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundPlayerPositionPacket) {
            moveSpeed = 0.25f;
        }
    }

    @Override
    public void onBlock(EventBlock event) {
    }
}
