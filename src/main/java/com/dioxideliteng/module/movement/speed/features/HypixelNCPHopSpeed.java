package com.dioxideliteng.module.movement.speed.features;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.movement.MotionEvent;
import com.dioxideliteng.module.movement.Speed2;
import com.dioxideliteng.module.movement.speed.SpeedEvent;
import com.dioxideliteng.module.misc.Disabler2;
import com.dioxideliteng.module.player.Scaffold2;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HypixelNCPHopSpeed implements SpeedEvent {

    private final Speed2 speed2;

    public HypixelNCPHopSpeed(Speed2 speed2) {
        this.speed2 = speed2;
    }

    @Override
    public String getName() {
        return "Hypixel NCP Hop";
    }

    // ===================== Krs cross-module state (adapted lookups) =====================

    /** Krs parity: ModuleManager.getModuleState(DisablerModule.class). */
    private static boolean disablerActive() {
        Disabler2 disabler = disabler();
        return disabler != null && disabler.isEnabled();
    }

    /** Krs parity: DisablerModule.hypixelMotion.get(). */
    private static boolean hypixelMotion() {
        Disabler2 disabler = disabler();
        return disabler != null && disabler.isHypixelNcpMotionEnabled();
    }

    /** Krs parity: HypixelDisabler.watchDogDisabled. */
    private static boolean watchDogDisabled() {
        Disabler2 disabler = disabler();
        return disabler != null && disabler.isWatchdogDisabled();
    }

    private static Disabler2 disabler() {
        for (Feature feature : FeatureManager.getModules()) {
            if (feature instanceof Disabler2 disabler)
                return disabler;
        }
        return null;
    }

    private boolean lowStrafeCheck() {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        return player != null && level != null && lowStrafeCheck(player, level);
    }

    private boolean lowStrafeCheck(LocalPlayer player, ClientLevel level) {
        BlockState state = level.getBlockState(player.blockPosition().below());
        return !state.isAir() && !(state.getBlock() instanceof SlabBlock) && !(state.getBlock() instanceof StairBlock);
    }

    private boolean hasCollisionBelow(LocalPlayer player, ClientLevel level) {
        double startY = player.getY();
        double x = player.getX();
        double z = player.getZ();

        for (int i = 0; i <= 1; i++) {
            double checkY = startY - i;
            BlockPos pos = BlockPos.containing(x, checkY, z);
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) continue;

            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty())
                return true;
        }

        return false;
    }

    private float speedAdjustedStrafe(LocalPlayer player, float base, float baseBonus, float amplifierBonus) {
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed == null)
            return base;

        int amplifier = speed.getAmplifier();
        return base + (((amplifier + 1f) * amplifier == 0) ? baseBonus : amplifierBonus);
    }

    public static boolean canLowHop = false;

    @Override
    public void onUpdate(EventUpdate event) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || player.isInWater() || player.isSpectator() || FeatureManager.scaffold2 != null && FeatureManager.scaffold2.isEnabled() && Scaffold2.getWasTowering()) {
            canLowHop = false;
            return;
        }

        if (disablerActive() && hypixelMotion()) {
            if (watchDogDisabled()) {
                if (player.onGround()) {
                    if (MovementUtil.isMoving())
                        player.jumpFromGround();

                    MovementUtil.strafe(speedAdjustedStrafe(player, 0.472f, 0.036f, 0.12f));

                    canLowHop = false;
                } else if (!player.hasEffect(MobEffects.JUMP_BOOST)) {
                    double baseVelocityY = player.getDeltaMovement().y;

                    switch (MovementUtil.fallTicks) {
                        case 1:
                            if (level.getBlockState(player.blockPosition().above(3)).isAir() && MovementUtil.isMoving() && !player.horizontalCollision) {
                                MovementUtil.setVelocityY(baseVelocityY + 0.0568);
                                canLowHop = true;
                            } else canLowHop = false;
                            break;

                        case 3:
                            if (canLowHop) {
                                MovementUtil.setVelocityY(baseVelocityY - 0.13);
                            }
                            break;

                        case 4:
                            if (canLowHop) {
                                MovementUtil.setVelocityY(baseVelocityY - 0.2);
                            }
                            break;

                        case 7:
                            if (canLowHop && lowStrafeCheck(player, level) && hasCollisionBelow(player, level)) {
                                MovementUtil.strafe(speedAdjustedStrafe(player, 0.285f, 0.036f, 0.044f));
                            }
                            canLowHop = false;
                            break;

                        case 8:
                            if (canLowHop) {
                                if (hasCollisionBelow(player, level)) {
                                    MovementUtil.setVelocityY(-0.4);
                                }

                                canLowHop = false;
                            }
                            break;
                    }
                }
            }
        } else {
            if (player.onGround()) {
                if (MovementUtil.isMoving())
                    player.jumpFromGround();

                MovementUtil.strafe(speedAdjustedStrafe(player, 0.481f, 0.036f, 0.12f));
            } else if (lowStrafeCheck(player, level)) {
                switch (MovementUtil.fallTicks) {
                    case 1:
                        MovementUtil.strafe((float) MovementUtil.getBaseMoveSpeed(0.2873));
                        break;

                    case 10:
                        if (player.hurtTime == 0) {
                            MovementUtil.setVelocityY(-0.28);

                            MovementUtil.strafe(speedAdjustedStrafe(player, 0.305f, 0.036f, 0.044f));
                        }
                        break;

                    case 11:
                        MovementUtil.strafe((float) MovementUtil.getBaseMoveSpeed(0.2713));
                        break;

                    case 12:
                        MovementUtil.stopMoving();
                        break;
                }
            }
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
    }

    @Override
    public void onTick(EventTick event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (player.onGround() && MovementUtil.isMoving() && (!disablerActive() || (!hypixelMotion() || watchDogDisabled())))
            mc.options.keyJump.setDown(false);
    }

    @Override
    public void onSendPacket(EventPacketSend event) {
    }

    @Override
    public void onReceivedPacket(EventPacketReceive event) {
    }
}
