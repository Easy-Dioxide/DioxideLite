package com.dioxideliteng.module.movement.fly.features;

import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.Fly2;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.fly.FlyEvent;
import com.dioxideliteng.util.krs.InputUtil;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.TimerUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public class MatrixFly implements FlyEvent {

    private final Fly2 fly2;

    public MatrixFly(Fly2 fly2) {
        this.fly2 = fly2;
    }

    private boolean positionLookReceived;
    private boolean highBoost;
    private boolean highRestoreMotion;
    private Vec3 highMotion = Vec3.ZERO;
    private int highSelfDamageJumps;
    private int damageTicks;
    private int damageSelfDamageJumps;
    private int damageBoostTicks;
    private boolean damageStarted;
    private int airTicks;
    private String lastMode = "";

    @Override
    public String getName() {
        return "Matrix";
    }

    public void reset() {
        positionLookReceived = false;
        highBoost = false;
        highRestoreMotion = false;
        highMotion = Vec3.ZERO;
        highSelfDamageJumps = 0;
        damageTicks = 0;
        damageSelfDamageJumps = 0;
        damageBoostTicks = 0;
        damageStarted = false;
        airTicks = 0;
        lastMode = this.fly2.matrixMode.getValue();
    }

    public void disable() {
        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("high"))
            MovementUtil.stopMoving();

        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("damage")) {
            TimerUtil.reset();

            var player = mc.player;
            if (player != null && this.fly2.matrixDamagePacket.getValue()) {
                Vec3 position = player.position();
                PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.PosRot(
                        position.x,
                        position.y,
                        position.z,
                        player.getYRot(),
                        player.getXRot(),
                        false,
                        player.horizontalCollision
                ));
                PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.Pos(
                        position.x,
                        position.y,
                        position.z,
                        false,
                        player.horizontalCollision
                ));
                PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.PosRot(
                        position.x,
                        position.y,
                        position.z,
                        player.getYRot(),
                        player.getXRot(),
                        false,
                        player.horizontalCollision
                ));
            }
        }

        reset();
    }

    @Override
    public void onUpdate(EventUpdate event) {
        var player = mc.player;
        if (player == null) return;

        if (!this.fly2.matrixMode.getValue().equalsIgnoreCase(lastMode)) {
            if (lastMode.equalsIgnoreCase("damage"))
                TimerUtil.reset();
            reset();
        }

        if (player.onGround())
            airTicks = 0;
        else
            airTicks++;

        switch (this.fly2.matrixMode.getValue().toLowerCase(java.util.Locale.ROOT)) {
            case "normal" -> {
                if (player.onGround())
                    player.input.makeJump();

                if (airTicks > 1)
                    MovementUtil.setVelocityY(player.getDeltaMovement().y + 0.00348);

                if (player.fallDistance > 0.1F && !positionLookReceived) {
                    MovementUtil.setVelocityY(0.42);
                    MovementUtil.strafe(1.97F);
                }

                if (positionLookReceived && airTicks == 20) {
                    MovementUtil.setVelocityY(0.42);
                    MovementUtil.strafe(9.3F);
                }
            }
            case "high" -> {
                if (highBoost)
                    MovementUtil.setVelocityY(this.fly2.matrixHighHeight.getValue());

                if (player.hurtTime >= 1 && player.hurtTime <= 8) {
                    if (player.onGround())
                        player.input.makeJump();
                    else if (player.getDeltaMovement().y < 0.2)
                        highBoost = true;
                }

                if (this.fly2.matrixHighSelfDamage.getValue() && highSelfDamageJumps < 4 && player.onGround()) {
                    player.input.makeJump();
                    highSelfDamageJumps++;
                }
            }
            case "damage" -> {
                if (player.hurtTime > 0 && !damageStarted && airTicks >= 3) {
                    damageBoostTicks = 20 * (int) (float) this.fly2.matrixDamageTimerSpeed.getValue();
                    damageStarted = true;
                }

                if (this.fly2.matrixDamageSelfDamage.getValue() && player.hurtTime <= 0 && damageSelfDamageJumps < 4 && player.onGround()) {
                    player.input.makeJump();
                    damageSelfDamageJumps++;
                }

                if (!this.fly2.matrixDamageDetectDamage.getValue() || damageTicks <= this.fly2.matrixDamageFlyTicks.getValue() || !damageStarted) {
                    float speed = damageBoostTicks > 0 ? (float) this.fly2.matrixDamageSpeed.getValue() : 0.03F;

                    if (!this.fly2.matrixDamageNoSpeed.getValue())
                        MovementUtil.strafe(speed);

                    TimerUtil.timerSpeed = (float) this.fly2.matrixDamageTimerSpeed.getValue();

                    switch (this.fly2.matrixDamageMotionY.getValue().toLowerCase(java.util.Locale.ROOT)) {
                        case "none" -> MovementUtil.setVelocityY(player.getDeltaMovement().y * 0.039);
                        case "simple" -> MovementUtil.setVelocityY(this.fly2.matrixDamageMotion.getValue());
                        case "multiply" -> MovementUtil.setVelocityY(player.getDeltaMovement().y * this.fly2.matrixDamageMotion.getValue());
                    }

                    if (damageBoostTicks > 0)
                        damageBoostTicks--;

                    damageTicks++;
                }

                if (this.fly2.matrixDamageDetectDamage.getValue() && damageStarted) {
                    if (!this.fly2.matrixDamageAutoDisable.getValue() && damageTicks >= this.fly2.matrixDamageFlyTicks.getValue()) {
                        this.fly2.setEnabled(false);
                    } else if (this.fly2.matrixDamageAutoDisable.getValue() && damageBoostTicks <= 0) {
                        this.fly2.setEnabled(false);
                    }
                }
            }
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        var player = mc.player;
        if (player == null) return;

        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("high")) {
            if (this.fly2.matrixHighSelfDamage.getValue() && highSelfDamageJumps < 4)
                event.onGround = false;
            return;
        }

        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("damage")) {
            if (this.fly2.matrixDamageSelfDamage.getValue() && player.hurtTime <= 0 && damageSelfDamageJumps < 4)
                event.onGround = false;

            if (this.fly2.matrixDamageNewSelfDamage.getValue() && player.fallDistance > 3.0F) {
                event.onGround = true;
                player.setOnGround(true);
                MovementUtil.setVelocityY(0.0);
                player.fallDistance = 0.0F;
            }
        }
    }

    @Override
    public void onTick(EventTick event) {
        var player = mc.player;
        if (player == null) return;

        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("normal")) {
            if (airTicks <= 15 || airTicks >= 19) return;

            Input keyPresses = player.input.keyPresses;
            InputUtil.applyInput(player.input, new Input(
                    false,
                    false,
                    keyPresses.left(),
                    keyPresses.right(),
                    keyPresses.jump(),
                    keyPresses.shift(),
                    keyPresses.sprint()
            ));
        } else if (this.fly2.matrixMode.getValue().equalsIgnoreCase("damage") && this.fly2.matrixDamageSelfDamage.getValue() && !damageStarted && (damageSelfDamageJumps < 4 || !player.onGround())) {
            Input keyPresses = player.input.keyPresses;
            InputUtil.applyInput(player.input, new Input(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    keyPresses.sprint()
            ));
        }
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("high") && highRestoreMotion && event.getPacket() instanceof ServerboundMovePlayerPacket) {
            var player = mc.player;
            if (player != null) {
                highRestoreMotion = false;
                player.setDeltaMovement(highMotion);
            }
        }
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
        var player = mc.player;
        if (player == null) return;

        Packet<?> packet = event.getPacket();

        if (this.fly2.matrixMode.getValue().equalsIgnoreCase("high")) {
            if (highBoost && packet instanceof ClientboundPlayerPositionPacket) {
                highRestoreMotion = true;
                highMotion = player.getDeltaMovement();
                highBoost = false;
            }
            return;
        }

        if (!this.fly2.matrixMode.getValue().equalsIgnoreCase("normal"))
            return;

        if (!(packet instanceof ClientboundPlayerPositionPacket(
                int id, PositionMoveRotation change, java.util.Set<net.minecraft.world.entity.Relative> relatives
        )))
            return;

        event.setCancelled(true);

        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                PositionMoveRotation.of(player),
                change,
                relatives
        );
        Vec3 position = absolute.position();

        // 26.3 adaptation: ServerboundAcceptTeleportationPacket takes the teleport
        // id plus the acknowledged position/rotation (Krs 26.2 only had the id).
        // Vanilla sends the player's current position/rotation here; the mode
        // teleports the player to the packet's absolute position below, which is
        // the value the acknowledgement refers to.
        PacketUtil.sendPacketAsSilent(new ServerboundAcceptTeleportationPacket(
                id,
                position.x,
                position.y,
                position.z,
                player.getYRot(),
                player.getXRot()
        ));
        PacketUtil.sendPacketAsSilent(new ServerboundMovePlayerPacket.PosRot(
                position.x,
                position.y,
                position.z,
                player.getYRot(),
                player.getXRot(),
                false,
                player.horizontalCollision
        ));

        player.setPos(position);
        player.jumpFromGround();

        if (positionLookReceived) {
            this.fly2.setEnabled(false);
            return;
        }

        positionLookReceived = true;
    }

    @Override
    public void onBlock(EventBlock event) {
    }
}
