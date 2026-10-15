package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.BehaviorUtils;
import com.dioxideliteng.util.krs.InputUtil;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.PlayerUtil;
import com.dioxideliteng.util.krs.RandomUtil;
import com.dioxideliteng.util.krs.RotationManager;
import com.dioxideliteng.util.krs.RotationScraper;
import com.dioxideliteng.util.krs.TimerUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Port of Krs Scaffold (com.instrumentalist.krs.hacks.features.player.Scaffold) to DioxideLiteNG.
 *
 * Adaptations:
 *  - {@code Client.rotationManager} replaced with a local {@link RotationManager} instance; its
 *    yaw/pitch are applied to the player through {@link Events#ROTATION}.
 *  - {@code ModuleManager.rotTick} replaced with a local counter.
 *  - {@code MovementFix} (no client equivalent) dropped; movement direction uses the player yaw.
 *  - {@code Sprint.tryStartSprinting} replaced with direct sprint toggling.
 *  - {@code ServerboundSwingPacket} (removed in 26.3) replaced with {@code player.swing(...)}.
 *  - {@code ModuleManager.getModuleState(SpeedModule)} replaced with a FeatureManager lookup.
 */
public class Scaffold2 extends Feature {

    private static final double MATH_MOVEMENT_ALIGNMENT_WEIGHT = 1.35D;
    private static final double MATH_MOVEMENT_PATH_ALIGNMENT_WEIGHT = 0.24D;
    private static final double MATH_MOVEMENT_SWAY_WEIGHT = 0.32D;
    private static final float MATH_MOVEMENT_SECTOR_DEGREES = 45.0F;
    private static final float MATH_MOVEMENT_ALIGNMENT_EPSILON = 0.01F;
    private static final int MATH_MOVEMENT_PREDICTION_TICKS = 8;
    private static final double MATH_HUMAN_AIM_GROUND_RADIUS = 0.055D;
    private static final double MATH_HUMAN_AIM_AIR_RADIUS = 0.03D;
    private static final float MATH_HUMAN_AIM_ERROR_TOLERANCE = 1.25F;
    private static final float MATH_HUMAN_SPEED_BIAS = 0.08F;
    private static final int MATH_HUMAN_AIM_ATTEMPTS = 6;
    private static final int ROTATION_SPEED_JITTER_MIN_TICKS = 3;
    private static final int ROTATION_SPEED_JITTER_MAX_TICKS = 7;
    private static final double MATH_FACE_CENTER_WEIGHT = 0.12D;
    private static final double MATH_EDGE_HIT_PENALTY = 4.0D;
    private static final double MATH_SAFE_HIT_INSET = 0.0625D;
    private static final double MATH_ALIGNED_RAY_EPSILON = 1.0E-5D;
    private static final int MATH_FACE_SAMPLE_DIVISIONS = 16;
    private static final double MATH_SEARCH_BASE_ROTATION_SAVING_TICKS = 0.30D;
    private static final double MATH_SEARCH_DISTANCE_SAVING_TICKS = 0.42D;
    private static final double MATH_SEARCH_DISTANCE_SQUARED_SAVING_TICKS = 0.12D;
    private static final double MATH_SEARCH_ORIGIN_PRIORITY_TICKS = 0.30D;
    private static final double MATH_SEARCH_PATH_WEIGHT_TICKS = 0.24D;
    private static final double MATH_SEARCH_PLAYER_DISTANCE_WEIGHT_TICKS = 0.35D;

    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", new String[]{"Normal", "Telly"});
    private final BooleanSetting tower = new BooleanSetting("Tower", this, true);
    private final BooleanSetting towerCenter = new BooleanSetting("Tower Center", this, false);
    private final ModeSetting towerWhen = new ModeSetting("Tower When", this, "Always", new String[]{"Always", "Moving", "Standing"});
    private final ModeSetting towerMode = new ModeSetting("Tower Mode", this, "Vanilla", new String[]{"Vanilla", "NCP", "Hypixel"});
    private final NumberSetting towerSpeed = new NumberSetting("Tower Speed", this, 1.0, 0.1, 1.0, 0.1);
    private final ModeSetting tellyMode = new ModeSetting("Telly Mode", this, "Normal", new String[]{"Normal", "Non Upwards"});
    private final ModeSetting rotationMode = new ModeSetting("Rotation Mode", this, "Math", new String[]{"Math", "Simple", "Backwards", "None"});
    private final NumberSetting maxRotationSpeed = new NumberSetting("Max Rotation Speed", this, 60.0, 0.0, 180.0, 1.0);
    private final NumberSetting minRotationSpeed = new NumberSetting("Min Rotation Speed", this, 40.0, 0.0, 180.0, 1.0);
    private final BooleanSetting jitter = new BooleanSetting("Jitter", this, true);
    private final NumberSetting preRotationLead = new NumberSetting("Pre Rotation Lead", this, 1.0, 0.0, 5.0, 0.5);
    private final BooleanSetting snapRotation = new BooleanSetting("Snap Rotation", this, false);
    private final BooleanSetting noPlaceWhenSnapping = new BooleanSetting("No Place when Snapping", this, false);
    private final BooleanSetting noHitCheck = new BooleanSetting("No Hit Check", this, false);
    private final BooleanSetting tellyAfterPlaceNoHitCheck = new BooleanSetting("Telly After Place No Hit Check", this, false);
    private final NumberSetting searchRange = new NumberSetting("Search Range", this, 2.0, 0.0, 5.0, 1.0);
    private final BooleanSetting customTimer = new BooleanSetting("Custom Timer", this, false);
    private final NumberSetting towerTimerSpeed = new NumberSetting("Tower Timer Speed", this, 1.5, 0.1, 10.0, 0.1);
    private final NumberSetting normalTimerSpeed = new NumberSetting("Normal Timer Speed", this, 1.5, 0.1, 10.0, 0.1);
    private final BooleanSetting keepY = new BooleanSetting("KeepY", this, true);
    private final BooleanSetting keepYOnlySpeed = new BooleanSetting("KeepY Only Speed", this, true);
    private final BooleanSetting intelligentPicker = new BooleanSetting("Intelligent Picker", this, true);
    private final BooleanSetting noSprint = new BooleanSetting("No Sprint", this, false);
    private final ModeSetting edgeSafe = new ModeSetting("Edge Safe", this, "None", new String[]{"None", "Safewalk", "Sneak", "Auto Input Fix"});
    private final BooleanSetting down = new BooleanSetting("Down", this, true);

    private static int hotbarStackSize = 0;
    private static boolean wasTowering = false;
    private static boolean startedScaffold = false;
    private static boolean autoSneaking = false;

    private final RotationManager rotationManager = RotationManager.INSTANCE;

    private boolean once = false;
    private double jumpGround = 0.0;
    private boolean checkGround = false;
    private Integer launchY = null;
    private boolean placedScaffoldBlock = false;
    private BlockPos lastPlacedScaffoldBlock = null;
    private boolean snapRotationResetPending = false;
    private boolean tellyAfterPlaceNoHitCheckActive = false;
    private PlacementTarget lockedMathPlacementTarget = null;
    private int tellyStage = 0;
    private int tellyNormalJumpIndex = 0;
    private boolean suppressTellyNormalJumpSprint = false;
    private boolean tellyNormalJumpedThisTick = false;
    private boolean tellyJumpedThisTick = false;
    private boolean wasTellyKeepYOnGround = false;
    private boolean tellyJumpDownOnLastGround = false;
    private boolean tellyClutchMode = false;
    private Integer lastSlot = null;
    private PlacementTarget humanizedMathAimTarget = null;
    private Vec3 humanizedMathAimPos = null;
    private int humanizedMathAimRefreshTick = Integer.MIN_VALUE;
    private int humanizedMathSpeedTick = Integer.MIN_VALUE;
    private int humanizedMathSpeedBiasRefreshTick = Integer.MIN_VALUE;
    private float humanizedMathYawSpeed = Float.NaN;
    private float humanizedMathPitchSpeed = Float.NaN;
    private float humanizedMathSpeedBias = 0.0F;
    private int rotTick = 0;
    private final RotationSpeedJitterState rotationSpeedJitter = new RotationSpeedJitterState();
    private final RotationSpeedJitterState mathPitchSpeedJitter = new RotationSpeedJitterState();

    private final Direction[] orderedHorizontalDirections = new Direction[4];
    private final Direction[] orderedPlaceDirections = new Direction[6];
    private final ArrayList<BlockPos> scaffoldCandidates = new ArrayList<>(64);
    private final ArrayList<BlockPos> scaffoldDistanceCandidates = new ArrayList<>(32);
    private final HashSet<BlockPos> scaffoldCandidateSet = new HashSet<>(64);

    private record PlacementTarget(BlockPos pos, BlockPos neighbour, Vec3 hitPos, Direction side, int searchDistance) {
    }

    private record PlacementHit(Vec3 hitPos, double rotationScore, double distanceSqr) {
    }

    private static final class RotationSpeedJitterState {
        private int updateTick = Integer.MIN_VALUE;
        private int refreshTick = Integer.MIN_VALUE;
        private float speed = Float.NaN;
        private float targetSpeed = Float.NaN;

        private void reset() {
            updateTick = Integer.MIN_VALUE;
            refreshTick = Integer.MIN_VALUE;
            speed = Float.NaN;
            targetSpeed = Float.NaN;
        }
    }

    public Scaffold2() {
        super("Scaffold2", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        var player = mc.player;
        if (player == null || mc.level == null) return;

        DynamicIslandManager.notifyInfo("Scaffold2", "已开启 Scaffold2 搭路");
        launchY = player.blockPosition().getY();
        lastSlot = player.getInventory().getSelectedSlot();
        placedScaffoldBlock = false;
        lastPlacedScaffoldBlock = null;
        snapRotationResetPending = false;
        tellyAfterPlaceNoHitCheckActive = false;
        clearMathPlacementTarget();
        tellyStage = 0;
        tellyNormalJumpIndex = 0;
        suppressTellyNormalJumpSprint = false;
        tellyNormalJumpedThisTick = false;
        tellyJumpedThisTick = false;
        wasTellyKeepYOnGround = player.onGround();
        tellyJumpDownOnLastGround = isTellyMode() && player.onGround() && isPhysicalJumpDown();
        tellyClutchMode = false;
        resetRotationHumanization();
        countBlockItems(player.getInventory());
        rotTick = 0;
    }

    @Override
    public void onDisable() {
        var player = mc.player;
        if (player != null && mc.level != null) {
            if (wasTowering) {
                switch (towerMode.getValue().toLowerCase(Locale.ROOT)) {
                    case "vanilla" -> {
                        if (hasAroundBlock())
                            MovementUtil.setVelocityY(-0.4);
                    }
                    case "hypixel" -> launchY = player.blockPosition().getY() - 1;
                }
            }
            if (once) {
                rotationManager.stopRotation();
                TimerUtil.reset();
                BehaviorUtils.noKillAura = false;
                if (lastSlot != null)
                    selectSlot(lastSlot);
            }
        }

        lastSlot = null;
        once = false;
        jumpGround = 0.0;
        wasTowering = false;
        checkGround = false;
        launchY = null;
        startedScaffold = false;
        placedScaffoldBlock = false;
        lastPlacedScaffoldBlock = null;
        snapRotationResetPending = false;
        tellyAfterPlaceNoHitCheckActive = false;
        clearMathPlacementTarget();
        tellyStage = 0;
        tellyNormalJumpIndex = 0;
        suppressTellyNormalJumpSprint = false;
        tellyNormalJumpedThisTick = false;
        tellyJumpedThisTick = false;
        wasTellyKeepYOnGround = false;
        tellyJumpDownOnLastGround = false;
        tellyClutchMode = false;
        resetRotationHumanization();
        updateAutoSneak(false);
        PlayerUtil.INSTANCE.stopSpoof();
        rotTick = 0;
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.POST_MOTION) {
            DynamicIslandManager.sampleScaffoldMovement();
        } else if (event == Events.BLOCK_EDGE) {
            onBlockEdge();
        } else if (event == Events.TICK) {
            onTick();
            onUpdate();
        } else if (event == Events.ROTATION) {
            setSuffix(mode.getValue());
            if (rotationManager.isRotating()) {
                Events.ROTATION.setYaw(rotationManager.getClientYaw());
                Events.ROTATION.setPitch(rotationManager.getClientPitch());
                rotTick++;
            } else {
                rotTick = 0;
            }
        } else if (event == Events.POST_MOVE_INPUT) {
            handlePostMoveInput();
        }
    }

    private void onBlockEdge() {
        if (mc.player == null || mc.level == null) return;

        if (edgeSafe.is("Safewalk") && (!down.getValue() || !isPhysicalKeyDown(mc.options.keyShift.saveString())))
            Events.BLOCK_EDGE.setCancelled(true);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (mc.player.onGround())
            MovementUtil.fallTicks = 0;
        else
            MovementUtil.fallTicks++;

        tellyJumpedThisTick = false;
        tellyNormalJumpedThisTick = false;
        boolean tellyGroundPhase = isTellyGroundPhase();

        if (tellyGroundPhase)
            faceTellyJumpRotation();

        if (down.getValue())
            mc.options.keyShift.setDown(false);

        if (tellyGroundPhase || wasTowering)
            mc.options.keyJump.setDown(false);

        if (!tellyGroundPhase
                && !wasTowering
                && isTellyUpwardJumpInputActive(mc.player)
                && !isSpeedModuleEnabled())
            mc.options.keyJump.setDown(true);
    }

    private void handlePostMoveInput() {
        var player = mc.player;
        if (player == null || mc.level == null) return;

        ClientInput input = player.input;
        if (input == null) return;

        if (isTellyMode())
            handleTellyJumpInput(input);

        if (edgeSafe.is("Sneak")) {
            boolean shouldSneak = shouldAutoSneakAtEdge(input);
            autoSneaking = shouldSneak;
            if (shouldSneak)
                setInputSneak(input, true);
            else if (down.getValue() && isPhysicalKeyDown(mc.options.keyShift.saveString()))
                setInputSneak(input, false);
        } else {
            autoSneaking = false;
        }

        if (edgeSafe.is("Auto Input Fix"))
            handleAutoInputFix(input);
    }

    private void handleTellyJumpInput(ClientInput input) {
        var player = mc.player;
        if (input == null || player == null || mc.level == null || player.input != input || !isTellyMode())
            return;

        updateTellyJumpGroundInput(player);
        updateTellyClutchMode(player);
        updateTellyNormalJumpIndex(player);
        if (shouldUseNormalScaffoldForTelly(player))
            return;

        if (shouldTowerBlockedTellyJump(player)) {
            InputUtil.setJumping(input, false);
            return;
        }

        boolean normalTellyJump = isTellyNormalJumpTiming(player);
        boolean suppressTellySprint = shouldSuppressTellyNormalJumpSprint();

        if ((normalTellyJump || suppressTellySprint) && player.isSprinting())
            player.setSprinting(false);

        if (isTellyMovementActive() && hotbarStackSize > 0 && !normalTellyJump && !suppressTellySprint)
            tryStartSprinting(player);

        if (!isTellyGroundPhase(player))
            return;

        if (tellyStage <= 0) {
            InputUtil.setJumping(input, false);
            return;
        }

        faceTellyJumpRotation();
        if (!requestTellyJump(player, input))
            InputUtil.setJumping(input, false);
    }

    private void tryStartSprinting(net.minecraft.client.player.LocalPlayer player) {
        if (player == null) return;
        Input presses = player.input != null ? player.input.keyPresses : Input.EMPTY;
        if (presses.forward() && !presses.backward() && !player.isShiftKeyDown())
            player.setSprinting(true);
    }

    private boolean requestTellyJump(net.minecraft.client.player.LocalPlayer player, ClientInput input) {
        Input keyPresses = input.keyPresses == null ? Input.EMPTY : input.keyPresses;
        if (isTellyNormalJumpTiming(player))
            return requestNormalTellyJump(player, input, keyPresses);

        boolean sprinting = keyPresses.sprint() || player.isSprinting();
        tryStartSprinting(player);
        sprinting = true;

        if (!sprinting)
            return false;

        InputUtil.applyInput(input, new Input(
                keyPresses.forward(),
                keyPresses.backward(),
                keyPresses.left(),
                keyPresses.right(),
                true,
                keyPresses.shift(),
                sprinting
        ));
        tellyJumpedThisTick = true;
        suppressTellyNormalJumpSprint = false;
        tellyNormalJumpedThisTick = false;
        recordTellyJump();
        return true;
    }

    private boolean requestNormalTellyJump(net.minecraft.client.player.LocalPlayer player, ClientInput input, Input keyPresses) {
        player.setSprinting(false);
        InputUtil.applyInput(input, new Input(
                keyPresses.forward(),
                keyPresses.backward(),
                keyPresses.left(),
                keyPresses.right(),
                true,
                keyPresses.shift(),
                false
        ));
        tellyJumpedThisTick = true;
        suppressTellyNormalJumpSprint = true;
        tellyNormalJumpedThisTick = true;
        recordTellyJump();
        return true;
    }

    private void updateTellyStage(net.minecraft.client.player.LocalPlayer player) {
        if (!isTellyMovementActive() || hotbarStackSize <= 0 || shouldUseNormalScaffoldForTelly(player)) {
            tellyStage = 0;
            tellyNormalJumpIndex = 0;
            suppressTellyNormalJumpSprint = false;
            tellyNormalJumpedThisTick = false;
            return;
        }

        if (!player.onGround())
            return;

        if (tellyStage > 0)
            tellyStage--;
        else if (tellyStage < 0)
            tellyStage++;

        if (tellyStage == 0)
            tellyStage = 1;

        faceTellyJumpRotation();
    }

    private boolean shouldTellyAvoidAiming() {
        var player = mc.player;
        if (shouldUseNormalScaffoldForTelly(player))
            return false;

        if (isTellyGroundPhase(player))
            return !wasTowering;

        return player != null
                && isTellyMode()
                && MovementUtil.isMoving()
                && hotbarStackSize > 0
                && MovementUtil.fallTicks <= 0
                && !wasTowering;
    }

    private boolean isTellyGroundPhase() {
        return isTellyGroundPhase(mc.player);
    }

    private boolean isTellyGroundPhase(net.minecraft.client.player.LocalPlayer player) {
        return player != null
                && isTellyMovementActive()
                && hotbarStackSize > 0
                && player.onGround()
                && !shouldUseNormalScaffoldForTelly(player);
    }

    private boolean shouldUseNormalScaffoldForTelly(net.minecraft.client.player.LocalPlayer player) {
        return isTellyMode()
                && player != null
                && (player.onGround() && isTellyHeadBlocked(player) || isTellyClutchMode(player));
    }

    private void updateTellyClutchMode(net.minecraft.client.player.LocalPlayer player) {
        if (!isTellyMode() || player == null || player.onGround()) {
            tellyClutchMode = false;
            return;
        }

        if (isTellyClutchTrigger(player))
            tellyClutchMode = true;
    }

    private boolean isTellyClutchMode(net.minecraft.client.player.LocalPlayer player) {
        return tellyClutchMode && player != null && !player.onGround();
    }

    private boolean isTellyClutchTrigger(net.minecraft.client.player.LocalPlayer player) {
        return player.getDeltaMovement().y <= -0.65D
                || isFarFromLaunchY(player)
                || player.hurtTime != 0;
    }

    private boolean isFarFromLaunchY(net.minecraft.client.player.LocalPlayer player) {
        return launchY != null && Math.abs(player.getY() - launchY) >= 4.0D;
    }

    private boolean isTellyHeadBlocked(net.minecraft.client.player.LocalPlayer player) {
        var level = mc.level;
        if (player == null || level == null)
            return false;

        AABB jumpCheckBox = player.getBoundingBox().move(0.0D, 0.42D, 0.0D);
        return !level.noCollision(player, jumpCheckBox);
    }

    private boolean isTellyMovementActive() {
        return isTellyMode() && isTellyMoving();
    }

    private boolean canDisplayTellyMode() {
        return isTellyMode() && !tower.getValue();
    }

    private boolean isTellyNormalJumpModeActive() {
        return !tellyMode.getValue().equalsIgnoreCase("normal")
                && canDisplayTellyMode()
                && isPhysicalJumpDown()
                && isTellyMovementActive()
                && hotbarStackSize > 0;
    }

    private void updateTellyNormalJumpIndex(net.minecraft.client.player.LocalPlayer player) {
        if (!isTellyNormalJumpModeActive() || shouldUseNormalScaffoldForTelly(player)) {
            tellyNormalJumpIndex = 0;
            suppressTellyNormalJumpSprint = false;
            return;
        }

        if (suppressTellyNormalJumpSprint && player != null && player.onGround() && !isTellyNormalJumpQueued())
            suppressTellyNormalJumpSprint = false;
    }

    private boolean isTellyNormalJumpQueued() {
        if (!isTellyNormalJumpModeActive())
            return false;

        return tellyMode.getValue().equalsIgnoreCase("non upwards");
    }

    private boolean isTellyNormalJumpTiming(net.minecraft.client.player.LocalPlayer player) {
        return isTellyNormalJumpQueued()
                && isTellyGroundPhase(player)
                && tellyStage > 0;
    }

    private boolean isTellyNormalJumpRotationTiming(net.minecraft.client.player.LocalPlayer player) {
        return isTellyNormalJumpTiming(player)
                || tellyNormalJumpedThisTick && isTellyNormalJumpModeActive() && player != null;
    }

    private boolean shouldSuppressTellyNormalJumpSprint() {
        return isTellyNormalJumpModeActive()
                && !shouldUseNormalScaffoldForTelly(mc.player)
                && suppressTellyNormalJumpSprint;
    }

    private void recordTellyJump() {
        if (isTellyNormalJumpModeActive())
            tellyNormalJumpIndex++;
        else
            tellyNormalJumpIndex = 0;
    }

    private static boolean isTellyMoving() {
        return MovementUtil.isMoving();
    }

    private boolean shouldFreezeTellyKeepY(net.minecraft.client.player.LocalPlayer player) {
        return isTellyMode()
                && player != null
                && !shouldUseNormalScaffoldForTelly(player)
                && !isTellyUpwardJumpInputActive(player)
                && (isTellyMoving() || !isPhysicalMovementInputDown());
    }

    private boolean shouldUpdateTellyGroundKeepY(net.minecraft.client.player.LocalPlayer player) {
        return keepY.getValue()
                && isTellyMode()
                && player != null
                && player.onGround()
                && isPhysicalMovementInputDown()
                && !shouldUseNormalScaffoldForTelly(player)
                && !wasTellyKeepYOnGround;
    }

    private void updateTellyKeepYGroundState(net.minecraft.client.player.LocalPlayer player) {
        wasTellyKeepYOnGround = player != null && player.onGround();
    }

    private void updateTellyJumpGroundInput(net.minecraft.client.player.LocalPlayer player) {
        if (!isTellyMode() || player == null) {
            tellyJumpDownOnLastGround = false;
            return;
        }

        if (player.onGround())
            tellyJumpDownOnLastGround = isPhysicalJumpDown();
    }

    private static boolean isPhysicalJumpDown() {
        return isPhysicalKeyDown(mc.options.keyJump.saveString());
    }

    private static boolean isPhysicalMovementInputDown() {
        return isPhysicalKeyDown(mc.options.keyUp.saveString())
                || isPhysicalKeyDown(mc.options.keyDown.saveString())
                || isPhysicalKeyDown(mc.options.keyLeft.saveString())
                || isPhysicalKeyDown(mc.options.keyRight.saveString());
    }

    private boolean isTowerJumpInputActive(net.minecraft.client.player.LocalPlayer player) {
        return isTellyUpwardJumpInputActive(player) && !isTellyJumpPriorityActive(player);
    }

    private boolean shouldTowerBlockedTellyJump(net.minecraft.client.player.LocalPlayer player) {
        return tower.getValue()
                && isTellyMode()
                && player != null
                && player.horizontalCollision
                && isPhysicalJumpDown()
                && isTellyMoving();
    }

    private boolean isTellyUpwardJumpInputActive(net.minecraft.client.player.LocalPlayer player) {
        if (!isPhysicalJumpDown())
            return false;
        if (!isTellyMode() || player == null || player.onGround())
            return true;

        return tellyJumpDownOnLastGround;
    }

    private boolean isTellyJumpPriorityActive(net.minecraft.client.player.LocalPlayer player) {
        return (isTellyGroundPhase(player) && !shouldTowerBlockedTellyJump(player)) || tellyJumpedThisTick;
    }

    private static boolean shouldAutoSneakAtEdge(ClientInput clientInput) {
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return false;
        Scaffold2 s = inst();
        if (s != null && s.down.getValue() && isPhysicalKeyDown(mc.options.keyShift.saveString())) return false;
        if (!player.onGround() || player.getAbilities().flying || player.isPassenger() || player.isSwimming() || player.isFallFlying())
            return false;

        Input input = clientInput != null && clientInput.keyPresses != null ? clientInput.keyPresses : Input.EMPTY;
        if (!isMovingInput(input))
            return false;

        Vec3 direction = getAutoSneakDirection(input);
        if (direction.lengthSqr() <= 1.0E-6)
            return false;

        double distance = edgeCheckDistance();
        Vec3 position = player.position();
        return wouldBeCloseToFallOff(position)
                || wouldBeCloseToFallOff(position.add(direction.scale(distance)));
    }

    private static Vec3 getAutoSneakDirection(Input input) {
        var player = mc.player;
        if (player == null || !isMovingInput(input)) return Vec3.ZERO;

        float movementYaw = player.getYRot();
        return MovementUtil.getHorizontalDirectionVector(getMovementDirection(movementYaw, input));
    }

    private static double edgeCheckDistance() {
        var player = mc.player;
        if (player == null) return 0.45D;

        Vec3 motion = player.getDeltaMovement();
        double speed = MovementUtil.getSpeed(motion.x, motion.z);
        return Mth.clamp(Math.max(speed + 0.1D, player.isSprinting() ? 0.45D : 0.4D), 0.1D, 0.6D);
    }

    private static boolean wouldBeCloseToFallOff(Vec3 position) {
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return false;

        AABB hitbox = player.getDimensions(Pose.STANDING)
                .makeBoundingBox(position)
                .inflate(-0.05D, 0.0D, -0.05D)
                .move(0.0D, player.fallDistance - player.maxUpStep(), 0.0D);
        return level.noCollision(player, hitbox);
    }

    private void handleAutoInputFix(ClientInput clientInput) {
        var player = mc.player;
        var level = mc.level;
        if (clientInput == null || player == null || level == null)
            return;

        if (!player.onGround()
                || hotbarStackSize <= 0
                || player.getAbilities().flying
                || player.isPassenger()
                || player.isSwimming()
                || player.isFallFlying()
                || player.isSpectator())
            return;

        Input input = clientInput.keyPresses == null ? Input.EMPTY : clientInput.keyPresses;
        Vec3 position = player.position();
        double distance = autoInputFixDistance();
        if (!shouldApplyAutoInputFix(position, input, distance))
            return;

        InputUtil.applyInput(clientInput, stopAutoInputFixMovement(input));
    }

    private boolean shouldApplyAutoInputFix(Vec3 position, Input input, double distance) {
        if (isUnsupportedFallRisk(position))
            return true;

        if (isMovingInput(input) && isUnsupportedFallRisk(projectAutoInputFixPosition(position, input, distance)))
            return true;

        Vec3 motionPosition = projectAutoInputFixMotionPosition(position, distance);
        return !motionPosition.equals(position) && isUnsupportedFallRisk(motionPosition);
    }

    private boolean isUnsupportedFallRisk(Vec3 position) {
        return wouldBeCloseToFallOff(position) && !hasScaffoldSupportForPosition(position);
    }

    private Vec3 projectAutoInputFixPosition(Vec3 position, Input input, double distance) {
        Vec3 direction = getAutoInputFixDirection(input);
        if (direction.lengthSqr() <= 1.0E-6)
            return position;

        return position.add(direction.scale(distance));
    }

    private Vec3 projectAutoInputFixMotionPosition(Vec3 position, double distance) {
        var player = mc.player;
        if (player == null)
            return position;

        Vec3 motion = player.getDeltaMovement();
        double speed = MovementUtil.getSpeed(motion.x, motion.z);
        if (speed <= 1.0E-4)
            return position;

        Vec3 direction = MovementUtil.normalizeHorizontal(motion.x, motion.z);
        double motionDistance = Mth.clamp(speed + 0.1D, 0.25D, distance);
        return position.add(direction.scale(motionDistance));
    }

    private static Input stopAutoInputFixMovement(Input source) {
        return new Input(
                false,
                false,
                false,
                false,
                source != null && source.jump(),
                source != null && source.shift(),
                source != null && source.sprint()
        );
    }

    private boolean hasScaffoldSupportForPosition(Vec3 position) {
        var level = mc.level;
        if (level == null)
            return false;

        BlockPos targetBlock = scaffoldTargetBlockAt(position);
        if (!isScaffoldReplaceable(targetBlock))
            return true;

        PlacementTarget target = findPlacementTarget(targetBlock);
        return target != null && isRotationReadyForPlacement(target);
    }

    private BlockPos scaffoldTargetBlockAt(Vec3 position) {
        var player = mc.player;
        double targetY = launchY != null ? launchY : player != null ? player.blockPosition().getY() : position.y;
        return new BlockPos.MutableBlockPos(Mth.floor(position.x), scaffoldTargetBlockY(targetY), Mth.floor(position.z));
    }

    private static int scaffoldTargetBlockY(double y) {
        return Mth.floor(y - 0.75D);
    }

    private static double autoInputFixDistance() {
        var player = mc.player;
        if (player == null)
            return 0.45D;

        Vec3 motion = player.getDeltaMovement();
        double speed = MovementUtil.getSpeed(motion.x, motion.z);
        return Mth.clamp(Math.max(speed * 2.0D + 0.15D, player.isSprinting() ? 0.55D : 0.4D), 0.25D, 0.85D);
    }

    private static Vec3 getAutoInputFixDirection(Input input) {
        var player = mc.player;
        if (player == null || !isMovingInput(input))
            return Vec3.ZERO;

        float movementYaw = player.getYRot();
        return MovementUtil.getHorizontalDirectionVector(getMovementDirection(movementYaw, input));
    }

    private static void updateAutoSneak(boolean shouldSneak) {
        if (mc.player == null) {
            autoSneaking = false;
            return;
        }

        if (shouldSneak || autoSneaking) {
            boolean physicalSneak = isPhysicalKeyDown(mc.options.keyShift.saveString());
            Scaffold2 s = inst();
            boolean downVal = s != null && s.down.getValue();
            setSneakDown(shouldSneak || physicalSneak && !downVal);
        }

        autoSneaking = shouldSneak;
    }

    private static void setSneakDown(boolean sneaking) {
        mc.options.keyShift.setDown(sneaking);

        var player = mc.player;
        if (player == null || player.input == null)
            return;

        setInputSneak(player.input, sneaking);
    }

    private static void setInputSneak(ClientInput clientInput, boolean sneaking) {
        if (clientInput == null)
            return;

        Input input = clientInput.keyPresses == null ? Input.EMPTY : clientInput.keyPresses;
        if (input.shift() == sneaking)
            return;

        InputUtil.applyInput(clientInput, new Input(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.jump(),
                sneaking,
                input.sprint()
        ));
    }

    private static boolean isMovingInput(Input input) {
        return input != null && (input.forward() != input.backward() || input.left() != input.right());
    }

    public static boolean isScaffoldBlock(Block block) {
        if (block.defaultBlockState().isAir()) return false;

        var item = block.asItem();
        return !(block instanceof ChestBlock || block instanceof WebBlock || block instanceof CakeBlock || block instanceof BrewingStandBlock || block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock || block instanceof FurnaceBlock
                || block instanceof CraftingTableBlock || block instanceof CrafterBlock || block instanceof SmokerBlock || block instanceof BlastFurnaceBlock
                || block instanceof CartographyTableBlock || block instanceof AnvilBlock || block instanceof BellBlock || block instanceof BeaconBlock
                || block instanceof DragonEggBlock || block instanceof LeverBlock || block instanceof EnchantingTableBlock || block instanceof net.minecraft.world.level.block.ButtonBlock
                || block instanceof GrindstoneBlock || block instanceof LoomBlock || block instanceof NoteBlock || block instanceof FenceGateBlock
                || block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof StonecutterBlock || block instanceof StandingSignBlock
                || block instanceof WallSignBlock || block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock || block instanceof RepeaterBlock
                || block instanceof ComparatorBlock || block instanceof DispenserBlock || block instanceof JigsawBlock || block instanceof CommandBlock
                || block instanceof StructureBlock || block instanceof HopperBlock || block instanceof BedBlock || block instanceof FenceBlock || block instanceof SlabBlock || block instanceof PressurePlateBlock || block instanceof WallBlock || block instanceof StairBlock || block instanceof LadderBlock || block instanceof net.minecraft.world.level.block.ChainBlock || block instanceof CarpetBlock || block instanceof BarrelBlock || block instanceof RailBlock || block instanceof PoweredRailBlock || block instanceof DetectorRailBlock || item instanceof PlayerHeadItem || block instanceof HugeMushroomBlock || item == Items.TORCH || item == Items.REDSTONE || item == Items.REDSTONE_TORCH || item == Items.STRING || item == Items.TRIPWIRE_HOOK || item == Items.DECORATED_POT || item == Items.MELON_SEEDS || item == Items.WHEAT_SEEDS || item == Items.PUMPKIN_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.TORCHFLOWER_SEEDS || item == Items.IRON_BARS);
    }

    public static int countUsableHotbarBlocks(Container inventory) {
        if (inventory == null) return 0;

        int stackSize = 0;
        if (inventory.getContainerSize() > 40) {
            var offhandStack = inventory.getItem(40);
            if (isUsableScaffoldStack(offhandStack))
                stackSize += offhandStack.getCount();
        }

        for (int i = 0; i < Math.min(9, inventory.getContainerSize()); i++) {
            var stack = inventory.getItem(i);
            if (isUsableScaffoldStack(stack))
                stackSize += stack.getCount();
        }

        return stackSize;
    }

    private static boolean isUsableScaffoldStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;
        return isScaffoldBlock(blockItem.getBlock());
    }

    private void countBlockItems(Container inventory) {
        hotbarStackSize = countUsableHotbarBlocks(inventory);
    }

    private void onUpdate() {
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return;

        countBlockItems(player.getInventory());
        updateTellyClutchMode(player);
        updateTellyAfterPlaceNoHitCheckState(player);
        boolean updateTellyGroundKeepY = shouldUpdateTellyGroundKeepY(player);
        updateTellyKeepYGroundState(player);
        updateTellyJumpGroundInput(player);

        if (hotbarStackSize <= 0) {
            if (wasTowering) {
                switch (towerMode.getValue().toLowerCase(Locale.ROOT)) {
                    case "vanilla" -> {
                        if (hasAroundBlock())
                            MovementUtil.setVelocityY(-0.4);
                    }
                    case "hypixel" -> launchY = player.blockPosition().getY() - 1;
                }
            }
            jumpGround = 0.0;
            wasTowering = false;
            checkGround = false;
            if (once) {
                rotationManager.stopRotation();
                TimerUtil.reset();
                BehaviorUtils.noKillAura = false;
                if (lastSlot != null) {
                    selectSlot(lastSlot);
                    PlayerUtil.INSTANCE.stopSpoof();
                    lastSlot = null;
                }
            }
            once = false;
            placedScaffoldBlock = false;
            lastPlacedScaffoldBlock = null;
            snapRotationResetPending = false;
            tellyAfterPlaceNoHitCheckActive = false;
            clearMathPlacementTarget();
            tellyStage = 0;
            tellyNormalJumpIndex = 0;
            suppressTellyNormalJumpSprint = false;
            tellyNormalJumpedThisTick = false;
            tellyJumpedThisTick = false;
            wasTellyKeepYOnGround = false;
            tellyJumpDownOnLastGround = false;
            tellyClutchMode = false;
            resetRotationHumanization();
            return;
        }

        updateTellyStage(player);

        once = true;
        boolean skipScaffoldActionThisTick = consumeSnapRotationReset();

        if (noSprint.getValue() && !isTellyJumpPriorityActive(player))
            player.setSprinting(false);

        boolean hypixelPlaced = false;

        if (customTimer.getValue())
            TimerUtil.timerSpeed = (float) (wasTowering ? towerTimerSpeed.getValue() : normalTimerSpeed.getValue());

        if (isTellyJumpPriorityActive(player) && wasTowering) {
            jumpGround = 0.0;
            wasTowering = false;
        }

        if (tower.getValue()
                && (!towerMode.getValue().equalsIgnoreCase("hypixel") || !player.hasEffect(MobEffects.JUMP_BOOST) && !isSpeedModuleEnabled() || player.hasEffect(MobEffects.SPEED))
                && (towerWhen.getValue().equalsIgnoreCase("always") || towerWhen.getValue().equalsIgnoreCase("standing") && !MovementUtil.isMoving() || towerWhen.getValue().equalsIgnoreCase("moving") && MovementUtil.isMoving())) {
            if (isTowerJumpInputActive(player)) {
                if (!towerMode.getValue().equalsIgnoreCase("hypixel") || player.onGround())
                    wasTowering = true;
                if (hasAroundBlock() && wasTowering) {
                    if (towerWhen.getValue().equalsIgnoreCase("always") && !MovementUtil.isMoving() || towerWhen.getValue().equalsIgnoreCase("standing"))
                        MovementUtil.stopMoving();
                    if (towerCenter.getValue() && !MovementUtil.isMoving()) {
                        Vec3 center = Vec3.atCenterOf(player.blockPosition());
                        player.setPos(center.x, player.getY(), center.z);
                    }
                    switch (towerMode.getValue().toLowerCase(Locale.ROOT)) {
                        case "vanilla" -> {
                            if (MovementUtil.isMoving() && towerSpeed.getValue() >= 0.6)
                                MovementUtil.setVelocityY(0.5);
                            else MovementUtil.setVelocityY(towerSpeed.getValue());
                        }
                        case "ncp" -> {
                            if (player.onGround()) {
                                jumpGround = player.getY();
                                MovementUtil.setVelocityY(0.42);
                            }
                            if (player.getY() > jumpGround + 0.79f) {
                                player.setPos(player.getX(), Mth.floor(player.getY()), player.getZ());
                                MovementUtil.setVelocityY(0.42);
                                jumpGround = player.getY();
                            }
                        }
                        case "hypixel" -> {
                            if (MovementUtil.isMoving()) {
                                if (player.onGround())
                                    checkGround = true;
                                if (checkGround) {
                                    if (MovementUtil.fallTicks >= 18) {
                                        wasTowering = false;
                                        hypixelPlaced = true;
                                    } else {
                                        switch (MovementUtil.fallTicks % 3) {
                                            case 0 -> {
                                                var speedEffect = player.getEffect(MobEffects.SPEED);
                                                if (speedEffect != null)
                                                    MovementUtil.strafe(0.22f + ((speedEffect.getAmplifier() + 1) * (speedEffect.getAmplifier() == 0 ? 0.036f : 0.042f)));
                                                else MovementUtil.strafe(0.22f);
                                                MovementUtil.setVelocityY(0.42);
                                            }
                                            case 1 -> MovementUtil.setVelocityY(0.33);
                                            case 2 -> MovementUtil.setVelocityY((player.blockPosition().getY() + 1.0) - player.getY());
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (wasTowering) {
                    switch (towerMode.getValue().toLowerCase(Locale.ROOT)) {
                        case "vanilla" -> {
                            if (hasAroundBlock())
                                MovementUtil.setVelocityY(-0.4);
                        }
                        case "hypixel" -> launchY = player.blockPosition().getY() - 1;
                    }
                }
            } else if (wasTowering) {
                switch (towerMode.getValue().toLowerCase(Locale.ROOT)) {
                    case "vanilla" -> {
                        if (hasAroundBlock())
                            MovementUtil.setVelocityY(-0.4);
                    }
                    case "hypixel" -> launchY = player.blockPosition().getY() - 1;
                }
                jumpGround = 0.0;
                wasTowering = false;
            }
        } else if (wasTowering) {
            switch (towerMode.getValue().toLowerCase(Locale.ROOT)) {
                case "vanilla" -> {
                    if (hasAroundBlock())
                        MovementUtil.setVelocityY(-0.4);
                }
                case "hypixel" -> launchY = player.blockPosition().getY() - 1;
            }
            jumpGround = 0.0;
            wasTowering = false;
        }

        if (wasTowering)
            startedScaffold = true;

        if (updateTellyGroundKeepY) {
            launchY = player.blockPosition().getY();
        } else if (!shouldFreezeTellyKeepY(player)) {
            if (keepY.getValue()) {
                if (keepYOnlySpeed.getValue()) {
                    if ((!isSpeedModuleEnabled() || isTellyUpwardJumpInputActive(player))
                            && (wasTowering || MovementUtil.fallTicks >= 4))
                        launchY = player.blockPosition().getY();
                }
            } else if (wasTowering || MovementUtil.fallTicks >= 4) launchY = player.blockPosition().getY();
        }

        if (launchY == null)
            launchY = player.blockPosition().getY();

        if (player.distanceToSqr(player.getX(), launchY, player.getZ()) > 15)
            launchY = player.blockPosition().getY();

        if (down.getValue() && isPhysicalKeyDown(mc.options.keyShift.saveString()) && !wasTowering)
            launchY = player.blockPosition().getY() - 1;

        BlockPos playerBlockPos = player.blockPosition();
        BlockPos targetBlock = new BlockPos.MutableBlockPos(playerBlockPos.getX(), scaffoldTargetBlockY(launchY), playerBlockPos.getZ());

        BehaviorUtils.noKillAura = true;

        boolean targetBlockReplaceable = isScaffoldReplaceable(targetBlock);
        PlacementTarget placementTarget = targetBlockReplaceable ? findPlacementTarget(targetBlock) : null;
        PlacementTarget preRotationTarget = !skipScaffoldActionThisTick && !isSnapRotationActive() && placementTarget == null ? findPreRotationTarget(targetBlock) : null;
        if (!skipScaffoldActionThisTick) {
            faceScaffoldRotation(targetBlock, placementTarget, preRotationTarget, targetBlockReplaceable);

            if (targetBlockReplaceable && rotationMode.getValue().equalsIgnoreCase("math")) {
                PlacementTarget rotationTarget = findRotationPriorityPlacementTarget(targetBlock);
                placementTarget = selectAdaptiveMathSearchTarget(targetBlock, placementTarget, rotationTarget);
                lockedMathPlacementTarget = placementTarget;
            }
        }

        PlayerUtil.INSTANCE.doSpoof(lastSlot);

        if (skipScaffoldActionThisTick)
            return;

        if (isTellyGroundPhase())
            return;

        if (!targetBlockReplaceable || placementTarget == null || !isRotationReadyForPlacement(placementTarget))
            return;

        startedScaffold = true;

        if (!hypixelPlaced) {
            placeBlock(placementTarget);
        }
    }

    private void placeBlock(PlacementTarget target) {
        var player = mc.player;
        if (player == null) return;
        Integer slot = findBlockSlot(player.getInventory());
        if (slot == null) return;

        selectSlot(slot);
        tryPlaceBlock(target, slot);
    }

    private PlacementTarget findPlacementTarget(BlockPos pos) {
        if (usesRayTracePlacementSearch())
            return findRayTracePlacementTarget(pos);

        clearMathPlacementTarget();
        for (BlockPos candidate : buildScaffoldCandidates(pos)) {
            PlacementTarget target = findPlacementTargetForCandidate(pos, candidate, scaffoldSearchDistance(pos, candidate));
            if (target != null)
                return target;
        }

        return null;
    }

    private PlacementTarget findRayTracePlacementTarget(BlockPos pos) {
        PlacementTarget rotationTarget = findRotationPriorityPlacementTarget(pos);
        PlacementTarget lockedTarget = reusableMathPlacementTarget(pos);
        PlacementTarget nearestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos candidate : buildScaffoldCandidates(pos)) {
            int searchDistance = scaffoldSearchDistance(pos, candidate);
            if (nearestTarget != null && searchDistance > nearestTarget.searchDistance())
                break;

            PlacementTarget target = findRayTracePlacementTargetForCandidate(pos, candidate, searchDistance);
            if (target == null)
                continue;

            double score = placementTargetScore(pos, target);
            if (score < bestScore) {
                nearestTarget = target;
                bestScore = score;
            }
        }

        PlacementTarget positionTarget = selectMathPositionTarget(pos, lockedTarget, nearestTarget);
        PlacementTarget selectedTarget = selectAdaptiveMathSearchTarget(pos, positionTarget, rotationTarget);
        lockedMathPlacementTarget = selectedTarget;
        return selectedTarget;
    }

    private PlacementTarget findRotationPriorityPlacementTarget(BlockPos searchOrigin) {
        var level = mc.level;
        var player = mc.player;
        if (searchOrigin == null || level == null || player == null || !rotationMode.getValue().equalsIgnoreCase("math"))
            return null;

        BlockHitResult hit = rotationManager.rayTraceBlocks(
                rotationManager.getClientYaw(),
                rotationManager.getClientPitch(),
                placementReach()
        );
        if (hit.getType() != HitResult.Type.BLOCK)
            return null;

        BlockPos neighbour = hit.getBlockPos();
        Direction sideToClick = hit.getDirection();
        BlockPos placementPos = neighbour.relative(sideToClick);
        if (placementPos.getY() != searchOrigin.getY())
            return null;

        int searchDistance = scaffoldSearchDistance(searchOrigin, placementPos);
        if (searchDistance > currentSearchRange()
                || !isScaffoldReplaceable(placementPos)
                || !isPlacementPositionClearForPlayer(placementPos)
                || !isScaffoldSupport(level.getBlockState(neighbour)))
            return null;

        Vec3 hitPos = hit.getLocation();
        return new PlacementTarget(placementPos, neighbour, hitPos, sideToClick, searchDistance);
    }

    private PlacementTarget reusableMathPlacementTarget(BlockPos currentTargetBlock) {
        PlacementTarget locked = lockedMathPlacementTarget;
        if (locked == null)
            return null;

        var level = mc.level;
        if (level == null)
            return null;

        if (locked.pos().getY() != currentTargetBlock.getY())
            return null;

        if (!isScaffoldReplaceable(locked.pos()))
            return null;

        if (!isScaffoldSupport(level.getBlockState(locked.neighbour())))
            return null;

        int distance = scaffoldSearchDistance(currentTargetBlock, locked.pos());
        if (distance > currentSearchRange())
            return null;

        return locked;
    }

    private PlacementTarget selectMathPositionTarget(BlockPos currentTargetBlock, PlacementTarget lockedTarget, PlacementTarget bestTarget) {
        if (lockedTarget == null)
            return bestTarget;

        if (bestTarget == null)
            return lockedTarget;

        double lockedScore = placementTargetScore(currentTargetBlock, lockedTarget);
        double bestScore = placementTargetScore(currentTargetBlock, bestTarget);

        if (lockedScore <= bestScore + MATH_SEARCH_ORIGIN_PRIORITY_TICKS)
            return lockedTarget;

        return bestTarget;
    }

    private PlacementTarget selectAdaptiveMathSearchTarget(BlockPos searchOrigin, PlacementTarget positionTarget, PlacementTarget rotationTarget) {
        if (positionTarget == null)
            return rotationTarget;

        if (rotationTarget == null)
            return positionTarget;

        double positionScore = placementTargetScore(searchOrigin, positionTarget);
        double rotationScore = placementTargetScore(searchOrigin, rotationTarget);

        if (isTellyMode()) {
            if (shouldConserveMathTellyBlocks()) {
                double positionTellyScore = mathTellyConservationScore(searchOrigin, positionTarget);
                double rotationTellyScore = mathTellyConservationScore(searchOrigin, rotationTarget);
                if (Math.abs(positionTellyScore - rotationTellyScore) > 1.0E-4D)
                    return positionTellyScore < rotationTellyScore ? positionTarget : rotationTarget;
            }
        }

        if (rotationScore < positionScore)
            return rotationTarget;

        return positionTarget;
    }

    private PlacementTarget preferMovementAlignedTarget(PlacementTarget currentTarget, PlacementTarget candidateTarget) {
        if (currentTarget == null)
            return candidateTarget;

        if (candidateTarget == null)
            return currentTarget;

        float currentYaw = rotationsToHitPos(currentTarget.hitPos())[0];
        float candidateYaw = rotationsToHitPos(candidateTarget.hitPos())[0];
        float movementYaw = getScaffoldMovementDirection();

        double currentAlignment = mathMovementAlignmentScore(currentYaw);
        double candidateAlignment = mathMovementAlignmentScore(candidateYaw);

        if (Math.abs(candidateAlignment - currentAlignment) > MATH_MOVEMENT_ALIGNMENT_EPSILON)
            return candidateAlignment < currentAlignment ? candidateTarget : currentTarget;

        return currentTarget;
    }

    private double mathSearchPathPenaltyTicks(BlockPos origin, BlockPos candidate, int distanceGap) {
        if (distanceGap <= 0)
            return 0.0D;

        BlockPos lastPlaced = usableLastPlacedScaffoldBlock(origin);
        if (lastPlaced == null)
            return 0.0D;

        return lastPlacedBridgePathPenalty(origin, candidate, lastPlaced) * MATH_SEARCH_PATH_WEIGHT_TICKS;
    }

    private double placementRotationTicks(PlacementTarget target) {
        if (target == null)
            return Double.MAX_VALUE;

        float[] rotations = rotationsToHitPos(target.hitPos());
        float yawDiff = Math.abs(Mth.wrapDegrees(rotations[0] - rotationManager.getClientYaw()));
        float pitchDiff = Math.abs(rotations[1] - rotationManager.getClientPitch());

        double yawTicks = rotationTicks(yawDiff, expectedMathYawSpeed());
        double pitchTicks = rotationTicks(pitchDiff, expectedMathPitchSpeed());
        return Math.max(yawTicks, pitchTicks);
    }

    private double availablePreRotationTicks(BlockPos searchOrigin, PlacementTarget target) {
        if (target == null)
            return 0.0D;

        double speed = MovementUtil.getSpeed();
        double leadDistance = mathPreRotationLeadDistance(speed);
        if (leadDistance <= 1.0E-6D)
            return 0.0D;

        double originDistance = horizontalDistanceToBlock(searchOrigin);
        double targetDistance = horizontalDistanceToBlock(target.pos());
        double distanceGain = Math.max(0.0D, originDistance - targetDistance);
        if (distanceGain <= 1.0E-6D)
            return 0.0D;

        double ticksToReach = leadDistance / Math.max(speed, 1.0E-4D);
        double distanceTicks = distanceGain / Math.max(speed, 1.0E-4D) * MATH_SEARCH_DISTANCE_SAVING_TICKS;
        return Math.min(ticksToReach, distanceTicks + MATH_SEARCH_BASE_ROTATION_SAVING_TICKS);
    }

    private static double rotationTicks(float angleDifference, double degreesPerTick) {
        if (degreesPerTick <= 0.0D)
            return angleDifference > 0.0D ? Double.MAX_VALUE : 0.0D;
        return angleDifference / degreesPerTick;
    }

    private static double expectedMathYawSpeed() {
        Scaffold2 s = inst();
        if (s == null) return 0.0D;
        return (Math.min(s.minRotationSpeed.getValue(), s.maxRotationSpeed.getValue())
                + Math.max(s.minRotationSpeed.getValue(), s.maxRotationSpeed.getValue())) * 0.5D;
    }

    private static double expectedMathPitchSpeed() {
        float[] bounds = mathPitchSpeedBounds();
        return (bounds[0] + bounds[1]) * 0.5D;
    }

    private static double horizontalDistanceToBlock(BlockPos pos) {
        var player = mc.player;
        if (player == null) return Double.MAX_VALUE;
        double dx = pos.getX() + 0.5D - player.getX();
        double dz = pos.getZ() + 0.5D - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isIdealPlacementHitReachable(PlacementTarget target) {
        if (target == null) return false;
        float[] rotations = rotationsToHitPos(target.hitPos());
        BlockHitResult hit = rotationManager.rayTraceBlocks(rotations[0], rotations[1], placementReach());
        return isMatchingPlacementHit(hit, target);
    }

    private boolean shouldConserveMathTellyBlocks() {
        return isTellyMode() && hotbarStackSize <= 8;
    }

    private double mathTellyConservationScore(BlockPos origin, PlacementTarget target) {
        if (target == null) return Double.MAX_VALUE;
        return scaffoldSearchDistance(origin, target.pos()) + mathTellyFallbackAlignment(origin, target.pos()) * 0.5D;
    }

    private double mathTellyFallbackAlignment(BlockPos origin, BlockPos candidate) {
        float movementYaw = getScaffoldMovementDirection();
        float candidateYaw = blockOffsetYaw(candidate.getX() - origin.getX(), candidate.getZ() - origin.getZ());
        return Math.abs(Mth.wrapDegrees(candidateYaw - movementYaw));
    }

    private static boolean samePlacementTarget(PlacementTarget first, PlacementTarget second) {
        if (first == null || second == null)
            return first == second;
        return first.pos().equals(second.pos())
                && first.neighbour().equals(second.neighbour())
                && first.side() == second.side();
    }

    private void clearMathPlacementTarget() {
        lockedMathPlacementTarget = null;
    }

    private List<BlockPos> buildScaffoldCandidates(BlockPos pos) {
        scaffoldCandidates.clear();
        scaffoldCandidateSet.clear();
        addScaffoldCandidate(pos);

        int range = currentSearchRange();
        float searchYaw = scaffoldSearchYaw();
        Direction[] searchDirections = orderedHorizontalDirections(searchYaw);
        if (range >= 1) {
            for (Direction side : searchDirections)
                addScaffoldCandidate(pos.relative(side));
        }

        if (range >= 2) {
            for (Direction side : searchDirections) {
                for (Direction side2 : searchDirections) {
                    if (side2 == side.getOpposite()) continue;
                    addScaffoldCandidate(pos.relative(side).relative(side2));
                }
            }
        }

        if (range > 2) {
            for (int distance = 3; distance <= range; distance++)
                addScaffoldCandidatesAtDistance(pos, distance, searchYaw);
        }

        return scaffoldCandidates;
    }

    private void addScaffoldCandidatesAtDistance(BlockPos pos, int distance, float searchYaw) {
        scaffoldDistanceCandidates.clear();

        for (int x = -distance; x <= distance; x++) {
            for (int z = -distance; z <= distance; z++) {
                if (Math.abs(x) + Math.abs(z) != distance) continue;
                scaffoldDistanceCandidates.add(pos.offset(x, 0, z));
            }
        }

        scaffoldDistanceCandidates.sort((first, second) -> compareScaffoldCandidateDirection(pos, first, second, searchYaw));

        for (BlockPos candidate : scaffoldDistanceCandidates)
            addScaffoldCandidate(candidate);
    }

    private Direction[] orderedHorizontalDirections(float searchYaw) {
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        System.arraycopy(horizontalDirections, 0, orderedHorizontalDirections, 0, horizontalDirections.length);

        for (int i = 1; i < orderedHorizontalDirections.length; i++) {
            Direction direction = orderedHorizontalDirections[i];
            float directionScore = horizontalDirectionYawDistance(direction, searchYaw);
            int j = i - 1;

            while (j >= 0 && horizontalDirectionYawDistance(orderedHorizontalDirections[j], searchYaw) > directionScore) {
                orderedHorizontalDirections[j + 1] = orderedHorizontalDirections[j];
                j--;
            }

            orderedHorizontalDirections[j + 1] = direction;
        }

        return orderedHorizontalDirections;
    }

    private Direction[] orderedPlaceDirections(BlockPos searchOrigin, BlockPos candidate) {
        Direction preferredBridgeDirection = lastPlacedBridgeSupportDirection(searchOrigin, candidate);
        Direction[] horizontalDirections = orderedHorizontalDirections(scaffoldSearchYaw());
        int index = 0;

        if (preferredBridgeDirection != null)
            orderedPlaceDirections[index++] = preferredBridgeDirection;

        for (Direction direction : horizontalDirections) {
            if (direction == preferredBridgeDirection)
                continue;
            orderedPlaceDirections[index++] = direction;
        }

        orderedPlaceDirections[index++] = Direction.DOWN;
        orderedPlaceDirections[index] = Direction.UP;

        return orderedPlaceDirections;
    }

    private float scaffoldSearchYaw() {
        Float inputYaw = rotationMode.getValue().equalsIgnoreCase("math") && !isTellyMode()
                ? scaffoldInputDirection()
                : null;
        float movementYaw = inputYaw != null ? inputYaw : MovementUtil.getMotionDirection();
        return normalizeYaw(movementYaw + 180.0F);
    }

    private static float normalizeYaw(float yaw) {
        return (yaw % 360.0F + 360.0F) % 360.0F;
    }

    private static int compareScaffoldCandidateDirection(BlockPos origin, BlockPos first, BlockPos second, float searchYaw) {
        int yawCompare = Float.compare(scaffoldCandidateYawDistance(origin, first, searchYaw), scaffoldCandidateYawDistance(origin, second, searchYaw));
        if (yawCompare != 0)
            return yawCompare;

        int xCompare = Integer.compare(Math.abs(first.getX() - origin.getX()), Math.abs(second.getX() - origin.getX()));
        if (xCompare != 0)
            return xCompare;

        return Integer.compare(Math.abs(first.getZ() - origin.getZ()), Math.abs(second.getZ() - origin.getZ()));
    }

    private static float scaffoldCandidateYawDistance(BlockPos origin, BlockPos candidate, float searchYaw) {
        int x = candidate.getX() - origin.getX();
        int z = candidate.getZ() - origin.getZ();
        if (x == 0 && z == 0)
            return 0.0F;

        float yaw = blockOffsetYaw(x, z);
        return Math.abs(Mth.wrapDegrees(yaw - searchYaw));
    }

    private static float blockOffsetYaw(int x, int z) {
        return (float) ((Math.toDegrees(Math.atan2(-x, z)) + 360.0D) % 360.0D);
    }

    private Direction lastPlacedBridgeSupportDirection(BlockPos searchOrigin, BlockPos candidate) {
        BlockPos lastPlaced = usableLastPlacedScaffoldBlock(searchOrigin);
        if (lastPlaced == null || candidate == null || lastPlaced.getY() != candidate.getY())
            return null;

        return horizontalDirectionBetween(candidate, lastPlaced);
    }

    private double lastPlacedBridgePlacementScore(BlockPos searchOrigin, BlockPos candidate, BlockPos neighbour) {
        BlockPos lastPlaced = usableLastPlacedScaffoldBlock(searchOrigin);
        if (lastPlaced == null || candidate == null || neighbour == null)
            return 0.0D;

        double score = lastPlacedBridgePathPenalty(searchOrigin, candidate, lastPlaced);
        if (neighbour.equals(lastPlaced))
            score -= 1.0D;

        return score;
    }

    private double lastPlacedBridgePathPenalty(BlockPos origin, BlockPos candidate, BlockPos lastPlaced) {
        int bridgeDistance = scaffoldSearchDistance(lastPlaced, origin);
        if (bridgeDistance <= 1)
            return 0.0D;

        int distanceFromLastPlaced = scaffoldSearchDistance(lastPlaced, candidate);
        int detour = scaffoldSearchDistance(lastPlaced, candidate) + scaffoldSearchDistance(candidate, origin) - bridgeDistance;
        return (Math.max(0, distanceFromLastPlaced - 1) + Math.max(0, detour)) * 0.25D;
    }

    private BlockPos usableLastPlacedScaffoldBlock(BlockPos origin) {
        var level = mc.level;
        if (origin == null || lastPlacedScaffoldBlock == null || level == null)
            return null;
        if (lastPlacedScaffoldBlock.getY() != origin.getY())
            return null;
        if (scaffoldSearchDistance(origin, lastPlacedScaffoldBlock) > currentSearchRange() + 1)
            return null;

        return isScaffoldSupport(level.getBlockState(lastPlacedScaffoldBlock)) ? lastPlacedScaffoldBlock : null;
    }

    private static Direction horizontalDirectionBetween(BlockPos from, BlockPos to) {
        if (from == null || to == null || from.getY() != to.getY())
            return null;

        int x = to.getX() - from.getX();
        int z = to.getZ() - from.getZ();
        if (Math.abs(x) + Math.abs(z) != 1)
            return null;

        if (x > 0) return Direction.EAST;
        if (x < 0) return Direction.WEST;
        if (z > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    private static float horizontalDirectionYawDistance(Direction direction, float searchYaw) {
        return Math.abs(Mth.wrapDegrees(horizontalDirectionYaw(direction) - searchYaw));
    }

    private static float horizontalDirectionYaw(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    private void addScaffoldCandidate(BlockPos pos) {
        if (scaffoldCandidateSet.add(pos))
            scaffoldCandidates.add(pos);
    }

    private int currentSearchRange() {
        if (down.getValue() && isPhysicalKeyDown(mc.options.keyShift.saveString()))
            return 1;
        if (isPhysicalKeyDown(mc.options.keyJump.saveString()) && !MovementUtil.hasXZMotion() && MovementUtil.isBlockBelow())
            return 0;
        return (int) searchRange.getValue();
    }

    private static int scaffoldSearchDistance(BlockPos origin, BlockPos candidate) {
        return Math.abs(candidate.getX() - origin.getX()) + Math.abs(candidate.getZ() - origin.getZ());
    }

    private static boolean isScaffoldReplaceable(BlockPos pos) {
        var level = mc.level;
        return level != null && isScaffoldReplaceable(level.getBlockState(pos));
    }

    private static boolean isScaffoldReplaceable(BlockState state) {
        return state.canBeReplaced();
    }

    private static boolean isScaffoldSupport(BlockState state) {
        return !state.isAir() && !state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private PlacementTarget findPlacementTargetForCandidate(BlockPos searchOrigin, BlockPos pos, int searchDistance) {
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null) return null;

        if (!isScaffoldReplaceable(pos)) return null;
        if (!isPlacementPositionClearForPlayer(pos)) return null;

        Vec3 eyesPos = player.getEyePosition();
        Vec3 targetCenter = Vec3.atCenterOf(pos);

        for (Direction side : orderedPlaceDirections(searchOrigin, pos)) {
            BlockPos neighbour = pos.relative(side);
            Direction sideToClick = side.getOpposite();
            var neighbourState = level.getBlockState(neighbour);

            if (!isScaffoldSupport(neighbourState)) continue;

            Vec3 neighbourCenter = Vec3.atCenterOf(neighbour);

            if (!isDownwardsPlacementSupport(side) && eyesPos.distanceToSqr(targetCenter) >= eyesPos.distanceToSqr(neighbourCenter))
                continue;

            Vec3 hitPos = neighbourCenter.add(
                    sideToClick.getStepX() * 0.5,
                    sideToClick.getStepY() * 0.5,
                    sideToClick.getStepZ() * 0.5
            );

            PlacementTarget target = new PlacementTarget(pos, neighbour, hitPos, sideToClick, searchDistance);
            if (shouldSkipBackwardsPlacementTarget(target))
                continue;

            boolean centerInReach = eyesPos.distanceToSqr(hitPos) <= placementReachSqr();
            if (isNoHitCheckActive(target) || isRotationTargetHitCheckMode(target)) {
                if (centerInReach)
                    return target;
                continue;
            }

            PlacementHit rayTraceHit = findRayTraceablePlacementHit(neighbour, sideToClick);
            if (rayTraceHit != null)
                return new PlacementTarget(pos, neighbour, rayTraceHit.hitPos(), sideToClick, searchDistance);
        }

        return null;
    }

    private PlacementTarget findRayTracePlacementTargetForCandidate(BlockPos searchOrigin, BlockPos pos, int searchDistance) {
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null) return null;

        if (!isScaffoldReplaceable(pos)) return null;
        if (!isPlacementPositionClearForPlayer(pos)) return null;

        Vec3 eyesPos = player.getEyePosition();
        PlacementTarget bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (Direction side : orderedPlaceDirections(searchOrigin, pos)) {
            BlockPos neighbour = pos.relative(side);
            Direction sideToClick = side.getOpposite();
            var neighbourState = level.getBlockState(neighbour);

            if (!isScaffoldSupport(neighbourState)) continue;

            PlacementHit hit = findRayTraceablePlacementHit(neighbour, sideToClick);
            if (hit == null) continue;

            PlacementTarget target = new PlacementTarget(pos, neighbour, hit.hitPos(), sideToClick, searchDistance);
            if (shouldSkipBackwardsPlacementTarget(target))
                continue;

            double score = hit.rotationScore() + hit.distanceSqr() * MATH_SEARCH_DISTANCE_SQUARED_SAVING_TICKS;
            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private PlacementHit findRayTraceablePlacementHit(BlockPos neighbour, Direction sideToClick) {
        var player = mc.player;
        if (player == null || neighbour == null || sideToClick == null)
            return null;

        Vec3 eyesPos = player.getEyePosition();
        Vec3 faceCenter = placementFaceCenter(neighbour, sideToClick);
        double reach = placementReach();
        double reachSqr = reach * reach;
        float currentYaw = rotationManager.getClientYaw();
        float currentPitch = rotationManager.getClientPitch();
        float[] centerRotations = rotationsToHitPos(faceCenter);
        BlockHitResult currentHit = rotationManager.rayTraceBlocks(currentYaw, currentPitch, reach);
        PlacementHit currentPlacementHit = null;
        Vec3 bestHitPos = null;
        double bestDistanceSqr = 0.0D;
        double bestScore = Double.MAX_VALUE;
        if (isMatchingPlacementHit(currentHit, neighbour, sideToClick)) {
            Vec3 hitPos = currentHit.getLocation();
            currentPlacementHit = new PlacementHit(hitPos, 0.0D, eyesPos.distanceToSqr(hitPos));
            if (isStablePlacementHit(hitPos, neighbour, sideToClick)) {
                bestHitPos = hitPos;
                bestDistanceSqr = currentPlacementHit.distanceSqr();
                bestScore = smartPlacementHitScore(
                        hitPos,
                        new float[]{currentYaw, currentPitch},
                        currentYaw,
                        currentPitch,
                        eyesPos,
                        faceCenter,
                        sideToClick,
                        true
                );
            }
        }

        PlacementHit movementAlignedHit = findMovementAlignedPlacementHit(
                neighbour,
                sideToClick,
                eyesPos,
                faceCenter,
                reach,
                currentYaw,
                currentPitch,
                currentPlacementHit != null
        );
        if (movementAlignedHit != null && movementAlignedHit.rotationScore() < bestScore) {
            bestHitPos = movementAlignedHit.hitPos();
            bestDistanceSqr = movementAlignedHit.distanceSqr();
            bestScore = movementAlignedHit.rotationScore();
        }

        BlockHitResult centerHit = rotationManager.rayTraceBlocks(centerRotations[0], centerRotations[1], reach);
        if (isMatchingPlacementHit(centerHit, neighbour, sideToClick)) {
            Vec3 hitPos = centerHit.getLocation();
            double score = smartPlacementHitScore(hitPos, centerRotations, currentYaw, currentPitch, eyesPos, faceCenter, sideToClick, currentPlacementHit != null);
            if (score < bestScore) {
                bestHitPos = hitPos;
                bestDistanceSqr = eyesPos.distanceToSqr(hitPos);
                bestScore = score;
            }
        }

        int xCount = fixedOffsetCount(sideToClick, Direction.EAST, Direction.WEST);
        int yCount = fixedOffsetCount(sideToClick, Direction.UP, Direction.DOWN);
        int zCount = fixedOffsetCount(sideToClick, Direction.SOUTH, Direction.NORTH);
        for (int xIndex = 0; xIndex < xCount; xIndex++) {
            double dx = placementOffset(sideToClick, Direction.EAST, Direction.WEST, xIndex);
            for (int yIndex = 0; yIndex < yCount; yIndex++) {
                double dy = placementOffset(sideToClick, Direction.UP, Direction.DOWN, yIndex);
                for (int zIndex = 0; zIndex < zCount; zIndex++) {
                    double dz = placementOffset(sideToClick, Direction.SOUTH, Direction.NORTH, zIndex);
                    Vec3 hitPos = new Vec3(neighbour.getX() + dx, neighbour.getY() + dy, neighbour.getZ() + dz);
                    double distanceSqr = eyesPos.distanceToSqr(hitPos);
                    if (distanceSqr > reachSqr)
                        continue;

                    float[] rotations = rotationsToHitPos(hitPos);
                    BlockHitResult hit = rotationManager.rayTraceBlocks(rotations[0], rotations[1], reach);
                    if (!isMatchingPlacementHit(hit, neighbour, sideToClick))
                        continue;

                    Vec3 actualHitPos = hit.getLocation();
                    double score = smartPlacementHitScore(actualHitPos, rotations, currentYaw, currentPitch, eyesPos, faceCenter, sideToClick, currentPlacementHit != null);
                    if (score < bestScore) {
                        bestScore = score;
                        bestDistanceSqr = eyesPos.distanceToSqr(actualHitPos);
                        bestHitPos = actualHitPos;
                    }
                }
            }
        }

        if (bestHitPos != null)
            return new PlacementHit(bestHitPos, bestScore, bestDistanceSqr);

        return currentPlacementHit;
    }

    private PlacementHit findMovementAlignedPlacementHit(BlockPos neighbour, Direction sideToClick, Vec3 eyesPos, Vec3 faceCenter,
                                                          double reach, float currentYaw, float currentPitch, boolean currentCanHit) {
        Float movementYaw = movementFixMathInputYaw();
        if (movementYaw == null)
            return null;

        PlacementHit bestHit = null;
        double bestScore = Double.MAX_VALUE;
        double reachSqr = reach * reach;
        for (int sector = 0; sector < 8; sector++) {
            float alignedYaw = Mth.wrapDegrees(movementYaw + sector * MATH_MOVEMENT_SECTOR_DEGREES);
            Vec3 targetHitPos = movementAlignedHitPos(neighbour, sideToClick, eyesPos, faceCenter, alignedYaw, currentPitch);
            if (targetHitPos == null || eyesPos.distanceToSqr(targetHitPos) > reachSqr)
                continue;

            float alignedPitch = rotationsToHitPos(targetHitPos)[1];
            BlockHitResult hit = rotationManager.rayTraceBlocks(alignedYaw, alignedPitch, reach);
            if (!isMatchingPlacementHit(hit, neighbour, sideToClick))
                continue;

            Vec3 actualHitPos = hit.getLocation();
            if (!isStablePlacementHit(actualHitPos, faceCenter, sideToClick))
                continue;

            double distanceSqr = eyesPos.distanceToSqr(actualHitPos);
            double score = smartPlacementHitScore(
                    actualHitPos,
                    new float[]{alignedYaw, alignedPitch},
                    currentYaw,
                    currentPitch,
                    eyesPos,
                    faceCenter,
                    sideToClick,
                    currentCanHit
            );
            if (score < bestScore) {
                bestHit = new PlacementHit(actualHitPos, score, distanceSqr);
                bestScore = score;
            }
        }

        return bestHit;
    }

    private static Vec3 movementAlignedHitPos(BlockPos neighbour, Direction sideToClick, Vec3 eyesPos, Vec3 faceCenter,
                                              float yaw, float currentPitch) {
        double yawRadians = Math.toRadians(yaw);
        double directionX = -Math.sin(yawRadians);
        double directionZ = Math.cos(yawRadians);
        double safeInset = MATH_SAFE_HIT_INSET + MATH_ALIGNED_RAY_EPSILON;
        double minX = neighbour.getX() + safeInset;
        double maxX = neighbour.getX() + 1.0D - safeInset;
        double minY = neighbour.getY() + safeInset;
        double maxY = neighbour.getY() + 1.0D - safeInset;
        double minZ = neighbour.getZ() + safeInset;
        double maxZ = neighbour.getZ() + 1.0D - safeInset;
        double pitchTangent = Math.tan(Math.toRadians(currentPitch));

        if (sideToClick.getAxis() == Direction.Axis.X) {
            if (Math.abs(directionX) <= MATH_ALIGNED_RAY_EPSILON)
                return null;

            double planeX = sideToClick == Direction.EAST ? neighbour.getX() + 1.0D : neighbour.getX();
            double distance = (planeX - eyesPos.x) / directionX;
            if (distance <= MATH_ALIGNED_RAY_EPSILON)
                return null;

            double hitZ = eyesPos.z + directionZ * distance;
            if (hitZ < minZ || hitZ > maxZ)
                return null;

            double hitY = Mth.clamp(eyesPos.y - pitchTangent * distance, minY, maxY);
            return new Vec3(planeX, hitY, hitZ);
        }

        if (sideToClick.getAxis() == Direction.Axis.Z) {
            if (Math.abs(directionZ) <= MATH_ALIGNED_RAY_EPSILON)
                return null;

            double planeZ = sideToClick == Direction.SOUTH ? neighbour.getZ() + 1.0D : neighbour.getZ();
            double distance = (planeZ - eyesPos.z) / directionZ;
            if (distance <= MATH_ALIGNED_RAY_EPSILON)
                return null;

            double hitX = eyesPos.x + directionX * distance;
            if (hitX < minX || hitX > maxX)
                return null;

            double hitY = Mth.clamp(eyesPos.y - pitchTangent * distance, minY, maxY);
            return new Vec3(hitX, hitY, planeZ);
        }

        double[] xInterval = horizontalRayAxisInterval(eyesPos.x, directionX, minX, maxX);
        double[] zInterval = horizontalRayAxisInterval(eyesPos.z, directionZ, minZ, maxZ);
        if (xInterval == null || zInterval == null)
            return null;

        double minDistance = Math.max(MATH_ALIGNED_RAY_EPSILON, Math.max(xInterval[0], zInterval[0]));
        double maxDistance = Math.min(xInterval[1], zInterval[1]);
        if (minDistance > maxDistance)
            return null;

        double planeY = sideToClick == Direction.UP ? neighbour.getY() + 1.0D : neighbour.getY();
        double preferredDistance = (faceCenter.x - eyesPos.x) * directionX + (faceCenter.z - eyesPos.z) * directionZ;
        if (Math.abs(pitchTangent) > MATH_ALIGNED_RAY_EPSILON) {
            double currentPitchDistance = (eyesPos.y - planeY) / pitchTangent;
            if (Double.isFinite(currentPitchDistance) && currentPitchDistance > 0.0D)
                preferredDistance = currentPitchDistance;
        }

        double distance = Mth.clamp(preferredDistance, minDistance, maxDistance);
        return new Vec3(
                eyesPos.x + directionX * distance,
                planeY,
                eyesPos.z + directionZ * distance
        );
    }

    private static double[] horizontalRayAxisInterval(double origin, double direction, double min, double max) {
        if (Math.abs(direction) <= MATH_ALIGNED_RAY_EPSILON)
            return origin >= min && origin <= max
                    ? new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}
                    : null;

        double first = (min - origin) / direction;
        double second = (max - origin) / direction;
        return new double[]{Math.min(first, second), Math.max(first, second)};
    }

    private static int fixedOffsetCount(Direction sideToClick, Direction positive, Direction negative) {
        return sideToClick == positive || sideToClick == negative ? 1 : MATH_FACE_SAMPLE_DIVISIONS;
    }

    private static double placementOffset(Direction sideToClick, Direction positive, Direction negative, int index) {
        if (sideToClick == positive)
            return 1.0D;
        if (sideToClick == negative)
            return 0.0D;
        return (index + 0.5D) / MATH_FACE_SAMPLE_DIVISIONS;
    }

    private boolean usesRayTracePlacementSearch() {
        return rotationMode.getValue().equalsIgnoreCase("math") && !isNoHitCheckActive();
    }

    private static double placementReach() {
        var player = mc.player;
        return player != null ? player.blockInteractionRange() : 4.5D;
    }

    private static double placementReachSqr() {
        double reach = placementReach();
        return reach * reach;
    }

    private double placementRotationScore(float[] rotations, float currentYaw, float currentPitch) {
        float yawDiff = Math.abs(Mth.wrapDegrees(rotations[0] - currentYaw));
        float pitchDiff = Math.abs(rotations[1] - currentPitch);
        return yawDiff + pitchDiff * 1.5D;
    }

    private double smartPlacementHitScore(Vec3 hitPos, float[] rotations, float currentYaw, float currentPitch, Vec3 eyesPos, Vec3 faceCenter, Direction side, boolean currentCanHit) {
        double baseScore = placementRotationScore(rotations, currentYaw, currentPitch);
        if (!currentCanHit)
            baseScore += MATH_EDGE_HIT_PENALTY;

        if (isStablePlacementHit(hitPos, faceCenter, side))
            baseScore -= MATH_FACE_CENTER_WEIGHT;

        return baseScore;
    }

    private double mathMovementAlignmentScore(float targetYaw) {
        float movementYaw = getScaffoldMovementDirection();
        float error = mathMovementAlignmentError(targetYaw, movementYaw);
        return error * MATH_MOVEMENT_ALIGNMENT_WEIGHT;
    }

    private static float mathMovementAlignmentError(float targetYaw, float movementYaw) {
        return Math.abs(Mth.wrapDegrees(targetYaw - movementYaw));
    }

    private static float mathMovementSignedAlignmentError(float targetYaw, float movementYaw) {
        return Mth.wrapDegrees(targetYaw - movementYaw);
    }

    private double mathMovementTransitionScore(float currentYaw, float targetYaw, float movementYaw) {
        float yawDifference = Mth.wrapDegrees(targetYaw - currentYaw);
        float targetError = mathMovementAlignmentError(targetYaw, movementYaw);
        float currentError = mathMovementAlignmentError(currentYaw, movementYaw);

        double transitionScore = 0.0D;
        if (Math.abs(yawDifference) > 1.0E-4D) {
            float errorChange = targetError - currentError;
            transitionScore = errorChange * MATH_MOVEMENT_PATH_ALIGNMENT_WEIGHT;
        }

        double swayScore = targetError * MATH_MOVEMENT_SWAY_WEIGHT;
        return transitionScore + swayScore;
    }

    private double humanizedMathMovementTransitionScore(float currentYaw, float yawDifference, float movementYaw) {
        float[] speeds = humanizedMathRotationSpeeds(currentYaw + yawDifference, rotationManager.getClientPitch());
        return mathMovementTransitionScoreAtHumanizedSpeed(currentYaw, yawDifference, movementYaw, speeds[0]);
    }

    private double mathMovementTransitionScoreAtHumanizedSpeed(float currentYaw, float yawDifference, float movementYaw, float yawSpeed) {
        return mathMovementTransitionScoreAtSpeed(currentYaw, yawDifference, movementYaw, yawSpeed);
    }

    private static double mathMovementTransitionScoreAtSpeed(float currentYaw, float yawDifference, float movementYaw, double yawSpeed) {
        if (yawSpeed <= 0.0D)
            return Double.MAX_VALUE;

        float targetYaw = currentYaw + yawDifference;
        float targetError = mathMovementAlignmentError(targetYaw, movementYaw);
        float currentError = mathMovementAlignmentError(currentYaw, movementYaw);

        double ticks = Math.abs(yawDifference) / yawSpeed;
        double errorChangePerTick = (targetError - currentError) / Math.max(ticks, 1.0E-4D);
        return errorChangePerTick * MATH_MOVEMENT_PATH_ALIGNMENT_WEIGHT + targetError * MATH_MOVEMENT_SWAY_WEIGHT;
    }

    private static double mathMovementSpeedFactor() {
        var player = mc.player;
        if (player == null) return 1.0D;
        double speed = MovementUtil.getSpeed();
        return Mth.clamp(speed / 0.28D, 0.0D, 1.0D);
    }

    private static boolean isStablePlacementHit(Vec3 hitPos, BlockPos neighbour, Direction side) {
        return isStablePlacementHit(hitPos, placementFaceCenter(neighbour, side), side);
    }

    private static boolean isStablePlacementHit(Vec3 hitPos, Vec3 faceCenter, Direction side) {
        double distSqr = normalizedFaceCenterDistanceSqr(hitPos, faceCenter, side);
        return distSqr <= 0.04D;
    }

    private static double normalizedFaceCenterDistanceSqr(Vec3 hitPos, Vec3 faceCenter, Direction side) {
        double dx = 0.0D, dy = 0.0D, dz = 0.0D;
        switch (side.getAxis()) {
            case X -> { dy = hitPos.y - faceCenter.y; dz = hitPos.z - faceCenter.z; }
            case Y -> { dx = hitPos.x - faceCenter.x; dz = hitPos.z - faceCenter.z; }
            case Z -> { dx = hitPos.x - faceCenter.x; dy = hitPos.y - faceCenter.y; }
        }
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vec3 placementFaceCenter(BlockPos neighbour, Direction side) {
        return Vec3.atCenterOf(neighbour).add(
                side.getStepX() * 0.5D,
                side.getStepY() * 0.5D,
                side.getStepZ() * 0.5D
        );
    }

    private double placementTargetScore(PlacementTarget target) {
        return target == null ? Double.MAX_VALUE : placementTargetScore(null, target);
    }

    private double placementTargetScore(BlockPos currentTargetBlock, PlacementTarget target) {
        if (target == null) return Double.MAX_VALUE;

        double score = placementRotationTicks(target);
        score -= availablePreRotationTicks(currentTargetBlock != null ? currentTargetBlock : target.pos(), target);
        score += target.searchDistance() * MATH_SEARCH_DISTANCE_SAVING_TICKS;
        score += placementSideScore(target.side());

        if (currentTargetBlock != null)
            score += mathSearchPathPenaltyTicks(currentTargetBlock, target.pos(), target.searchDistance());

        if (target.searchDistance() == 0)
            score -= minimumPlacementTargetScore(0);

        return score;
    }

    private static double minimumPlacementTargetScore(int searchDistance) {
        return searchDistance * MATH_SEARCH_DISTANCE_SAVING_TICKS;
    }

    private static double placementSideScore(Direction side) {
        return side == Direction.DOWN ? 1.0D : 0.0D;
    }

    private boolean isDownwardsPlacementSupport(Direction side) {
        return side == Direction.UP;
    }

    private boolean tryPlaceBlock(PlacementTarget target, int slot) {
        var level = mc.level;
        var player = mc.player;
        var gameMode = mc.gameMode;
        if (level == null || player == null || gameMode == null) return false;

        if (!isScaffoldReplaceable(target.pos())) return true;
        if (!isScaffoldSupport(level.getBlockState(target.neighbour()))) return false;

        try {
            InteractionHand hand = slot == -1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getInventory().getItem(slot);
            if (!canPlaceScaffoldStackAt(target.pos(), stack))
                return false;

            if (hand == InteractionHand.MAIN_HAND)
                selectSlot(slot);

            BlockHitResult bhr = new BlockHitResult(target.hitPos(), target.side(), target.neighbour(), false);
            if (!rotationMode.getValue().equalsIgnoreCase("none") && !isPlacementBypassRotationMode(target) && !isNoHitCheckActive(target)) {
                facePlacementTarget(target);
                BlockHitResult currentHit = currentPlacementHit(target);
                if (currentHit == null)
                    return false;

                bhr = currentHit;
            }

            boolean placed = gameMode.useItemOn(player, hand, bhr).consumesAction();

            if (placed) {
                placedScaffoldBlock = true;
                lastPlacedScaffoldBlock = new BlockPos(target.pos().getX(), target.pos().getY(), target.pos().getZ());
                startedScaffold = true;
                if (shouldActivateTellyAfterPlaceNoHitCheck(player))
                    tellyAfterPlaceNoHitCheckActive = true;
                clearMathPlacementTarget();
                if (isSnapRotationActive())
                    snapRotationResetPending = true;
                player.swing(hand, SwingAnimation.DEFAULT, true);
            }

            return placed;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean canPlaceScaffoldStackAt(BlockPos pos, ItemStack stack) {
        if (pos == null || stack == null || !(stack.getItem() instanceof BlockItem blockItem))
            return false;

        return canPlaceScaffoldStateAt(pos, blockItem.getBlock().defaultBlockState());
    }

    private boolean canPlaceScaffoldStateAt(BlockPos pos, BlockState state) {
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null || pos == null || state == null)
            return false;

        var shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty())
            return true;

        return !doesShapeIntersectBox(pos, shape.toAabbs(), player.getBoundingBox());
    }

    private static boolean isPlacementPositionClearForPlayer(BlockPos pos) {
        var player = mc.player;
        if (player == null || pos == null)
            return false;

        AABB blockBox = new AABB(pos);
        return !intersectsPlayerBox(blockBox, player.getBoundingBox());
    }

    private static boolean doesShapeIntersectBox(BlockPos pos, List<AABB> boxes, AABB playerBox) {
        if (boxes == null || playerBox == null)
            return true;

        for (AABB box : boxes) {
            if (intersectsPlayerBox(box.move(pos), playerBox))
                return true;
        }

        return false;
    }

    private static boolean intersectsPlayerBox(AABB blockBox, AABB playerBox) {
        return blockBox != null && playerBox != null && blockBox.intersects(playerBox.deflate(1.0E-7D));
    }

    private boolean isRotationReadyForPlacement(PlacementTarget target) {
        if (rotationMode.getValue().equalsIgnoreCase("none"))
            return true;
        if (isNoHitCheckActive(target))
            return true;
        if (isRotationTargetHitCheckMode(target))
            return rotationManager.isRotating() && rotTick > 1 && isFacingRotationTarget(target);

        return rotationManager.isRotating() && rotTick > 1 && currentPlacementHit(target) != null;
    }

    private boolean isNoHitCheckActive() {
        if (isDownScaffoldActive())
            return true;

        if (isTellyAfterPlaceNoHitCheckActive())
            return true;

        return hasNoHitCheckOption() && noHitCheck.getValue();
    }

    private boolean isNoHitCheckActive(PlacementTarget target) {
        return isNoHitCheckActive() || isBackwardsMathNoHitCheckTarget(target);
    }

    private boolean consumeSnapRotationReset() {
        if (!snapRotationResetPending)
            return false;

        snapRotationResetPending = false;
        rotationManager.stopRotation();
        return noPlaceWhenSnapping.getValue();
    }

    private static boolean usesRotationSpeed() {
        Scaffold2 s = inst();
        return s != null && (s.rotationMode.getValue().equalsIgnoreCase("math")
                || s.rotationMode.getValue().equalsIgnoreCase("simple")
                || s.rotationMode.getValue().equalsIgnoreCase("backwards"));
    }

    private static boolean usesMathPreRotation() {
        Scaffold2 s = inst();
        return s != null && s.rotationMode.getValue().equalsIgnoreCase("math") && !s.isSnapRotationActive();
    }

    private static boolean hasNoHitCheckOption() {
        return usesRotationSpeed();
    }

    private static boolean canDisplayTellyAfterPlaceNoHitCheck() {
        Scaffold2 s = inst();
        return s != null && s.mode.getValue().equalsIgnoreCase("telly")
                && s.rotationMode.getValue().equalsIgnoreCase("math")
                && hasNoHitCheckOption()
                && !s.noHitCheck.getValue();
    }

    private boolean canUseTellyAfterPlaceNoHitCheck() {
        return tellyAfterPlaceNoHitCheck.getValue()
                && canDisplayTellyAfterPlaceNoHitCheck();
    }

    private void updateTellyAfterPlaceNoHitCheckState(net.minecraft.client.player.LocalPlayer player) {
        if (!canUseTellyAfterPlaceNoHitCheck() || player == null || player.onGround())
            tellyAfterPlaceNoHitCheckActive = false;
    }

    private boolean shouldActivateTellyAfterPlaceNoHitCheck(net.minecraft.client.player.LocalPlayer player) {
        return canUseTellyAfterPlaceNoHitCheck()
                && player != null
                && !player.onGround();
    }

    private boolean isTellyAfterPlaceNoHitCheckActive() {
        var player = mc.player;
        return tellyAfterPlaceNoHitCheckActive
                && canUseTellyAfterPlaceNoHitCheck()
                && player != null
                && !player.onGround();
    }

    private boolean isPlacementBypassRotationMode(PlacementTarget target) {
        return rotationMode.getValue().equalsIgnoreCase("simple")
                || rotationMode.getValue().equalsIgnoreCase("backwards") && !shouldBackwardsUseMathPlacement(target);
    }

    private boolean isRotationTargetHitCheckMode(PlacementTarget target) {
        return rotationMode.getValue().equalsIgnoreCase("simple")
                || rotationMode.getValue().equalsIgnoreCase("backwards") && !shouldBackwardsUseMathPlacement(target);
    }

    private boolean shouldBackwardsUseMathPlacement(PlacementTarget target) {
        return isBackwardsSearchMathPlacementTarget(target);
    }

    private boolean isBackwardsMathNoHitCheckTarget(PlacementTarget target) {
        return isBackwardsSearchMathPlacementTarget(target);
    }

    private boolean shouldSkipBackwardsPlacementTarget(PlacementTarget target) {
        return isBackwardsPlacementBehindRotation(target);
    }

    private static boolean isBackwardsSearchMathPlacementTarget(PlacementTarget target) {
        Scaffold2 s = inst();
        return target != null
                && s != null && s.rotationMode.getValue().equalsIgnoreCase("backwards")
                && target.searchDistance() >= 2;
    }

    private boolean isBackwardsPlacementBehindRotation(PlacementTarget target) {
        if (target == null || !rotationMode.getValue().equalsIgnoreCase("backwards"))
            return false;

        float placementYaw = rotationsToHitPos(target.hitPos())[0];
        float yawDiff = Math.abs(Mth.wrapDegrees(placementYaw - rotationManager.getClientYaw()));
        return yawDiff >= 90.0F;
    }

    private boolean isSnapRotationActive() {
        return (rotationMode.getValue().equalsIgnoreCase("math") || rotationMode.getValue().equalsIgnoreCase("simple")) && snapRotation.getValue();
    }

    private float getRotationSpeed() {
        return getJitteredRotationSpeed(
                (float) Math.min(minRotationSpeed.getValue(), maxRotationSpeed.getValue()),
                (float) Math.max(minRotationSpeed.getValue(), maxRotationSpeed.getValue()),
                rotationSpeedJitter
        );
    }

    private float getMathPitchRotationSpeed() {
        float[] bounds = mathPitchSpeedBounds();
        return getJitteredRotationSpeed(bounds[0], bounds[1], mathPitchSpeedJitter);
    }

    private float getJitteredRotationSpeed(float min, float max, RotationSpeedJitterState state) {
        float lowerBound = Mth.clamp(Math.min(min, max), 0.0F, 180.0F);
        float upperBound = Mth.clamp(Math.max(min, max), lowerBound, 180.0F);
        float midpoint = (lowerBound + upperBound) * 0.5F;
        if (!jitter.getValue() || upperBound - lowerBound <= MATH_MOVEMENT_ALIGNMENT_EPSILON) {
            state.reset();
            return midpoint;
        }

        var player = mc.player;
        int currentTick = player != null ? player.tickCount : 0;
        if (state.updateTick == currentTick && Float.isFinite(state.speed))
            return state.speed;

        state.speed = Float.isFinite(state.speed)
                ? Mth.clamp(state.speed, lowerBound, upperBound)
                : midpoint;
        if (!Float.isFinite(state.targetSpeed) || currentTick >= state.refreshTick) {
            state.targetSpeed = (RandomUtil.nextFloat(lowerBound, upperBound)
                    + RandomUtil.nextFloat(lowerBound, upperBound)) * 0.5F;
            state.refreshTick = currentTick + RandomUtil.nextInt(
                    ROTATION_SPEED_JITTER_MIN_TICKS,
                    ROTATION_SPEED_JITTER_MAX_TICKS
            );
        } else {
            state.targetSpeed = Mth.clamp(state.targetSpeed, lowerBound, upperBound);
        }

        float maxChange = Math.max(0.75F, (upperBound - lowerBound) * 0.12F);
        state.speed += Mth.clamp(state.targetSpeed - state.speed, -maxChange, maxChange);
        state.updateTick = currentTick;
        return state.speed;
    }

    private boolean isTellyMode() {
        return mode.getValue().equalsIgnoreCase("telly");
    }

    private boolean isDownScaffoldActive() {
        return down.getValue() && isPhysicalKeyDown(mc.options.keyShift.saveString());
    }

    private void faceScaffoldRotation(BlockPos targetBlock, PlacementTarget placementTarget, PlacementTarget preRotationTarget, boolean targetBlockReplaceable) {
        if (rotationMode.getValue().equalsIgnoreCase("none"))
            return;

        if (shouldTellyAvoidAiming()) {
            faceTellyJumpRotation(placementTarget != null ? placementTarget : preRotationTarget);
            return;
        }

        if (rotationMode.getValue().equalsIgnoreCase("simple")) {
            if (isSnapRotationActive() && (!targetBlockReplaceable || placementTarget == null))
                return;

            faceSimpleRotation();
            return;
        }

        if (rotationMode.getValue().equalsIgnoreCase("backwards")) {
            if (targetBlockReplaceable && placementTarget != null && shouldBackwardsUseMathPlacement(placementTarget))
                facePlacementTarget(placementTarget);
            else
                faceBackwardsRotation(targetBlockReplaceable ? placementTarget : null);
            return;
        }

        if (!isSnapRotationActive() && !placedScaffoldBlock && preRotationTarget == null && (!targetBlockReplaceable || placementTarget == null)) {
            faceSimpleRotation();
            return;
        }

        if (targetBlockReplaceable && placementTarget != null)
            facePlacementTarget(placementTarget);
        else if (preRotationTarget != null)
            facePlacementTarget(preRotationTarget);
    }

    private PlacementTarget findPreRotationTarget(BlockPos currentTargetBlock) {
        if (!rotationMode.getValue().equalsIgnoreCase("math"))
            return null;

        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null)
            return null;

        Vec3 direction = getPreRotationDirection();
        if (direction.lengthSqr() <= 1.0E-6)
            return null;

        double speed = MovementUtil.getSpeed(player.getDeltaMovement().x, player.getDeltaMovement().z);
        double leadDistance = mathPreRotationLeadDistance(speed);
        if (leadDistance <= 1.0E-6D)
            return null;

        Vec3 predictedPosition = player.position().add(direction.normalize().scale(leadDistance));
        BlockPos predictedTargetBlock = new BlockPos.MutableBlockPos(
                Mth.floor(predictedPosition.x),
                scaffoldTargetBlockY(launchY),
                Mth.floor(predictedPosition.z)
        );

        if (predictedTargetBlock.equals(currentTargetBlock) || !isScaffoldReplaceable(predictedTargetBlock))
            return null;

        return findPlacementTarget(predictedTargetBlock);
    }

    private static double mathPreRotationLeadDistance(double speed) {
        Scaffold2 s = inst();
        double lead = s != null ? s.preRotationLead.getValue() : 1.0D;
        double baseLeadDistance = Mth.clamp(Math.max(0.35D, speed * 2.0D + 0.15D), 0.35D, 0.85D);
        return baseLeadDistance * Mth.clamp(lead, 0.0D, 2.0D);
    }

    private Vec3 getPreRotationDirection() {
        var player = mc.player;
        if (player == null)
            return Vec3.ZERO;

        Float inputYaw = !isTellyMode() ? scaffoldInputDirection() : null;
        if (inputYaw != null)
            return MovementUtil.getHorizontalDirectionVector(inputYaw);

        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontalMotion = new Vec3(motion.x, 0.0D, motion.z);
        if (horizontalMotion.lengthSqr() > 1.0E-4)
            return horizontalMotion;

        inputYaw = scaffoldInputDirection();
        return inputYaw != null ? MovementUtil.getHorizontalDirectionVector(inputYaw) : Vec3.ZERO;
    }

    private void faceSimpleRotation() {
        var player = mc.player;
        if (player == null) return;

        float[] rotations = simpleRotationTarget();
        rotationManager.startRotation(rotations[0], rotations[1], getRotationSpeed());
    }

    private void faceBackwardsRotation(PlacementTarget target) {
        var player = mc.player;
        if (player == null) return;

        float[] rotations = backwardsRotationTarget(target);
        rotationManager.startRotation(rotations[0], rotations[1], getRotationSpeed());
    }

    private boolean isFacingRotationTarget(PlacementTarget placementTarget) {
        float[] rotations = scaffoldRotationTarget(placementTarget);
        if (rotations == null)
            return false;

        float yawDiff = Math.abs(Mth.wrapDegrees(rotationManager.getClientYaw() - rotations[0]));
        float pitchDiff = Math.abs(rotationManager.getClientPitch() - rotations[1]);
        return yawDiff <= 5.0F && pitchDiff <= 5.0F;
    }

    private float[] scaffoldRotationTarget(PlacementTarget placementTarget) {
        if (rotationMode.getValue().equalsIgnoreCase("simple"))
            return simpleRotationTarget();
        if (rotationMode.getValue().equalsIgnoreCase("backwards"))
            return backwardsRotationTarget(placementTarget);

        return null;
    }

    private float[] simpleRotationTarget() {
        var player = mc.player;
        float yaw = RotationScraper.INSTANCE.simpleSnap(getScaffoldMovementDirection() - 180f);
        float pitch = player != null && player.onGround() ? 70f : 80f;
        return new float[]{yaw, pitch};
    }

    private float[] backwardsRotationTarget(PlacementTarget target) {
        float yaw = Mth.wrapDegrees(getScaffoldMovementDirection() - 180f);
        float pitch = target != null ? rotationsToHitPos(target.hitPos())[1] : 85f;
        return new float[]{yaw, pitch};
    }

    private void faceTellyJumpRotation() {
        faceTellyJumpRotation(null);
    }

    private void faceTellyJumpRotation(PlacementTarget mathPitchTarget) {
        var player = mc.player;
        if (player == null || rotationMode.getValue().equalsIgnoreCase("none")) return;
        if (isTellyNormalJumpRotationTiming(player)) return;

        Float preparedPitch = mathTellyPreparationPitch(mathPitchTarget);
        if (preparedPitch != null)
            rotationManager.startRotation(tellyJumpRotationYaw(), preparedPitch, 180f, getRotationSpeed());
        else
            rotationManager.startRotation(tellyJumpRotationYaw(), rotationManager.getClientPitch(), 180f);
    }

    private Float mathTellyPreparationPitch(PlacementTarget target) {
        if (!rotationMode.getValue().equalsIgnoreCase("math") || isSnapRotationActive())
            return null;

        return target != null ? rotationsToHitPos(target.hitPos())[1] : simpleRotationTarget()[1];
    }

    private float tellyJumpRotationYaw() {
        float movementYaw = getScaffoldMovementDirection();
        if (!usesDiagonalTellyJumpRotation())
            return movementYaw;

        return Mth.wrapDegrees(movementYaw);
    }

    private boolean usesDiagonalTellyJumpRotation() {
        return rotationMode.getValue().equalsIgnoreCase("simple")
                || rotationMode.getValue().equalsIgnoreCase("backwards");
    }

    private BlockHitResult currentPlacementHit(PlacementTarget target) {
        BlockHitResult hit = rotationManager.rayTraceBlocks(
                rotationManager.getClientYaw(),
                rotationManager.getClientPitch(),
                mc.player != null ? mc.player.blockInteractionRange() : 4.5D
        );

        return isMatchingPlacementHit(hit, target) ? hit : null;
    }

    private boolean isMatchingPlacementHit(BlockHitResult hit, PlacementTarget target) {
        return isMatchingPlacementHit(hit, target.neighbour(), target.side());
    }

    private boolean isMatchingPlacementHit(BlockHitResult hit, BlockPos neighbour, Direction side) {
        return hit != null
                && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(neighbour)
                && hit.getDirection() == side;
    }

    private void facePlacementTarget(PlacementTarget target) {
        if (!usesHumanizedMathRotation()) {
            faceHitPos(target.hitPos(), getRotationSpeed());
            return;
        }

        Vec3 aimPos = getHumanizedMathAimPos(target);
        float[] rotations = rotationsToHitPos(aimPos);
        float[] speeds = humanizedMathRotationSpeeds(rotations[0], rotations[1]);
        rotationManager.startRotation(rotations[0], rotations[1], speeds[0], speeds[1]);
    }

    private boolean usesHumanizedMathRotation() {
        return rotationMode.getValue().equalsIgnoreCase("math")
                && !isSnapRotationActive()
                && !isDownScaffoldActive();
    }

    private Vec3 getHumanizedMathAimPos(PlacementTarget target) {
        var player = mc.player;
        if (player == null || target == null)
            return target != null ? target.hitPos() : Vec3.ZERO;

        int currentTick = player.tickCount;
        if (samePlacementTarget(humanizedMathAimTarget, target)
                && humanizedMathAimPos != null
                && currentTick < humanizedMathAimRefreshTick
                && isValidHumanizedMathAim(target, humanizedMathAimPos))
            return humanizedMathAimPos;

        double radius = humanizedMathAimRadius(player);
        for (int attempt = 0; attempt < MATH_HUMAN_AIM_ATTEMPTS; attempt++) {
            double offsetA = triangularRandomOffset(radius);
            double offsetB = triangularRandomOffset(radius);
            Vec3 candidate = clampedPlacementFaceHit(
                    offsetPlacementFaceHit(target.hitPos(), target.side(), offsetA, offsetB),
                    target.neighbour(),
                    target.side()
            );
            if (!isValidHumanizedMathAim(target, candidate))
                continue;

            humanizedMathAimTarget = target;
            humanizedMathAimPos = candidate;
            humanizedMathAimRefreshTick = currentTick + RandomUtil.nextInt(8, 17);
            return candidate;
        }

        humanizedMathAimTarget = target;
        humanizedMathAimPos = target.hitPos();
        humanizedMathAimRefreshTick = currentTick + RandomUtil.nextInt(6, 11);
        return target.hitPos();
    }

    private boolean isValidHumanizedMathAim(PlacementTarget target, Vec3 candidate) {
        var player = mc.player;
        if (player == null || target == null || candidate == null)
            return false;
        if (player.getEyePosition().distanceToSqr(candidate) > placementReachSqr() + 1.0E-4D)
            return false;
        if (!isStablePlacementHit(candidate, target.neighbour(), target.side()))
            return false;

        float[] baseRotations = rotationsToHitPos(target.hitPos());
        float[] candidateRotations = rotationsToHitPos(candidate);
        Float movementYaw = movementFixMathInputYaw();
        if (movementYaw != null) {
            float baseError = mathMovementSignedAlignmentError(baseRotations[0], movementYaw);
            float candidateError = mathMovementSignedAlignmentError(candidateRotations[0], movementYaw);
            if (Math.abs(Mth.wrapDegrees(candidateError - baseError)) > MATH_HUMAN_AIM_ERROR_TOLERANCE)
                return false;
        }

        BlockHitResult hit = rotationManager.rayTraceBlocks(
                candidateRotations[0],
                candidateRotations[1],
                placementReach()
        );
        return isMatchingPlacementHit(hit, target);
    }

    private Float movementFixMathInputYaw() {
        Float inputYaw = scaffoldInputDirection();
        return inputYaw;
    }

    private static Vec3 offsetPlacementFaceHit(Vec3 hitPos, Direction side, double offsetA, double offsetB) {
        return switch (side.getAxis()) {
            case X -> hitPos.add(0.0D, offsetA, offsetB);
            case Y -> hitPos.add(offsetA, 0.0D, offsetB);
            case Z -> hitPos.add(offsetA, offsetB, 0.0D);
        };
    }

    private static Vec3 clampedPlacementFaceHit(Vec3 hitPos, BlockPos neighbour, Direction side) {
        double inset = MATH_SAFE_HIT_INSET + MATH_ALIGNED_RAY_EPSILON * 2.0D;
        double x = Mth.clamp(hitPos.x, neighbour.getX() + inset, neighbour.getX() + 1.0D - inset);
        double y = Mth.clamp(hitPos.y, neighbour.getY() + inset, neighbour.getY() + 1.0D - inset);
        double z = Mth.clamp(hitPos.z, neighbour.getZ() + inset, neighbour.getZ() + 1.0D - inset);

        if (side == Direction.EAST)
            x = neighbour.getX() + 1.0D;
        else if (side == Direction.WEST)
            x = neighbour.getX();
        else if (side == Direction.UP)
            y = neighbour.getY() + 1.0D;
        else if (side == Direction.DOWN)
            y = neighbour.getY();
        else if (side == Direction.SOUTH)
            z = neighbour.getZ() + 1.0D;
        else if (side == Direction.NORTH)
            z = neighbour.getZ();

        return new Vec3(x, y, z);
    }

    private double humanizedMathAimRadius(net.minecraft.client.player.LocalPlayer player) {
        double radius = player.onGround() ? MATH_HUMAN_AIM_GROUND_RADIUS : MATH_HUMAN_AIM_AIR_RADIUS;
        double horizontalSpeed = MovementUtil.getSpeed(
                player.getDeltaMovement().x,
                player.getDeltaMovement().z
        );
        double movementScale = 1.0D - Mth.clamp(horizontalSpeed / 0.28D, 0.0D, 1.0D) * 0.45D;
        if (isTellyMode())
            movementScale *= 0.75D;
        return radius * movementScale;
    }

    private static double triangularRandomOffset(double radius) {
        return (RandomUtil.nextDouble(-radius, radius) + RandomUtil.nextDouble(-radius, radius)) * 0.5D;
    }

    private float[] humanizedMathRotationSpeeds(float targetYaw, float targetPitch) {
        var player = mc.player;
        int currentTick = player != null ? player.tickCount : 0;
        if (humanizedMathSpeedTick == currentTick
                && Float.isFinite(humanizedMathYawSpeed)
                && Float.isFinite(humanizedMathPitchSpeed))
            return new float[]{humanizedMathYawSpeed, humanizedMathPitchSpeed};

        if (!jitter.getValue()) {
            humanizedMathSpeedBias = 0.0F;
            humanizedMathSpeedBiasRefreshTick = Integer.MIN_VALUE;
        } else if (currentTick >= humanizedMathSpeedBiasRefreshTick) {
            humanizedMathSpeedBias = (RandomUtil.nextFloat(0.0F, 1.0F)
                    + RandomUtil.nextFloat(0.0F, 1.0F) - 1.0F) * MATH_HUMAN_SPEED_BIAS;
            humanizedMathSpeedBiasRefreshTick = currentTick + RandomUtil.nextInt(5, 11);
        }

        float yawMin = (float) Math.min(minRotationSpeed.getValue(), maxRotationSpeed.getValue());
        float yawMax = (float) Math.max(minRotationSpeed.getValue(), maxRotationSpeed.getValue());
        float yawDifference = Math.abs(Mth.wrapDegrees(targetYaw - rotationManager.getClientYaw()));
        float desiredYawSpeed = humanizedDesiredRotationSpeed(
                yawMin, yawMax, yawDifference, 90.0F, 0.18F, humanizedMathSpeedBias
        );
        humanizedMathYawSpeed = approachHumanizedSpeed(
                humanizedMathYawSpeed,
                desiredYawSpeed,
                yawMin,
                Math.max(1.5F, (yawMax - yawMin) * 0.18F),
                Math.max(2.0F, (yawMax - yawMin) * 0.26F)
        );

        float[] pitchBounds = mathPitchSpeedBounds();
        float pitchDifference = Math.abs(targetPitch - rotationManager.getClientPitch());
        float desiredPitchSpeed = humanizedDesiredRotationSpeed(
                pitchBounds[0], pitchBounds[1], pitchDifference, 35.0F, 0.25F, humanizedMathSpeedBias
        );
        humanizedMathPitchSpeed = approachHumanizedSpeed(
                humanizedMathPitchSpeed,
                desiredPitchSpeed,
                pitchBounds[0],
                Math.max(0.75F, (pitchBounds[1] - pitchBounds[0]) * 0.22F),
                Math.max(1.0F, (pitchBounds[1] - pitchBounds[0]) * 0.32F)
        );
        humanizedMathSpeedTick = currentTick;
        return new float[]{humanizedMathYawSpeed, humanizedMathPitchSpeed};
    }

    private static float humanizedDesiredRotationSpeed(float min, float max, float difference,
                                                       float fullSpeedDifference, float minimumFraction,
                                                       float speedBias) {
        if (max <= 0.0F)
            return 0.0F;

        float progress = Mth.clamp(difference / fullSpeedDifference, 0.0F, 1.0F);
        float easedProgress = progress * progress * (3.0F - 2.0F * progress);
        float fraction = minimumFraction + (1.0F - minimumFraction) * easedProgress
                + speedBias;
        return Mth.clamp(min + (max - min) * fraction, min, max);
    }

    private static float approachHumanizedSpeed(float current, float target, float initial,
                                                float acceleration, float deceleration) {
        if (!Float.isFinite(current))
            current = initial;
        float difference = target - current;
        return current + Mth.clamp(difference, -deceleration, acceleration);
    }

    private static float[] mathPitchSpeedBounds() {
        Scaffold2 s = inst();
        if (s == null) return new float[]{0.0F, 0.0F};
        float min = (float) Math.min(s.minRotationSpeed.getValue(), s.maxRotationSpeed.getValue());
        float max = (float) Math.max(s.minRotationSpeed.getValue(), s.maxRotationSpeed.getValue());
        float safeMax = Math.min(max, 18.0F);
        if (safeMax <= 0.0F)
            return new float[]{0.0F, 0.0F};

        float safeMin = min > 18.0F
                ? Math.min(12.0F, safeMax)
                : Math.min(min, safeMax);

        return new float[]{safeMin, safeMax};
    }

    private void resetRotationHumanization() {
        humanizedMathAimTarget = null;
        humanizedMathAimPos = null;
        humanizedMathAimRefreshTick = Integer.MIN_VALUE;
        humanizedMathSpeedTick = Integer.MIN_VALUE;
        humanizedMathSpeedBiasRefreshTick = Integer.MIN_VALUE;
        humanizedMathYawSpeed = Float.NaN;
        humanizedMathPitchSpeed = Float.NaN;
        humanizedMathSpeedBias = 0.0F;
        rotationSpeedJitter.reset();
        mathPitchSpeedJitter.reset();
    }

    private float getScaffoldMovementDirection() {
        var player = mc.player;
        if (player == null) return 0.0f;

        return MovementUtil.getPlayerDirection();
    }

    private Float scaffoldInputDirection() {
        var player = mc.player;
        if (player == null)
            return null;

        Input input = player.input != null ? player.input.keyPresses : Input.EMPTY;
        return isMovingInput(input) ? getMovementDirection(player.getYRot(), input) : null;
    }

    private static float getMovementDirection(float facingYaw, Input input) {
        if (input == null)
            return facingYaw;

        float yaw = facingYaw;
        float forwardMultiplier;

        if (input.backward() && !input.forward()) {
            yaw += 180f;
            forwardMultiplier = -0.5f;
        } else if (input.forward() && !input.backward()) {
            forwardMultiplier = 0.5f;
        } else {
            forwardMultiplier = 1f;
        }

        if (input.left() && !input.right())
            yaw -= 90f * forwardMultiplier;
        if (input.right() && !input.left())
            yaw += 90f * forwardMultiplier;

        return (yaw % 360f + 360f) % 360f;
    }

    private void faceHitPos(Vec3 hitPos, float speed) {
        float[] rotations = rotationsToHitPos(hitPos);
        if (rotationMode.getValue().equalsIgnoreCase("math") && !isDownScaffoldActive())
            rotationManager.startRotation(rotations[0], rotations[1], speed, getMathPitchRotationSpeed());
        else
            rotationManager.startRotation(rotations[0], rotations[1], speed);
    }

    private float[] rotationsToHitPos(Vec3 hitPos) {
        var player = mc.player;
        if (player == null) return new float[]{0.0f, 0.0f};

        Vec3 diff = hitPos.subtract(player.getEyePosition());
        double xzDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float) (Mth.atan2(-diff.x, diff.z) * (180.0 / Math.PI));
        float pitch = (float) (-(Mth.atan2(diff.y, xzDist) * (180.0 / Math.PI)));

        yaw = Mth.wrapDegrees(yaw);
        pitch = Mth.clamp(pitch, -90.0f, 90.0f);

        return new float[]{yaw, pitch};
    }

    private void selectSlot(int slot) {
        var player = mc.player;
        if (player == null) return;
        if (slot < 0 || slot > 8 || player.getInventory().getSelectedSlot() == slot) return;
        player.getInventory().setSelectedSlot(slot);
    }

    private Integer findBlockSlot(Container inv) {
        var player = mc.player;
        if (player != null && isUsableScaffoldStack(player.getOffhandItem()))
            return -1;

        Integer maxStackSlot = null;
        int maxStackSize = 0;

        for (int i = 0; i < Math.min(9, inv.getContainerSize()); i++) {
            if (i < 0 || i > 8) continue;
            var stack = inv.getItem(i);
            if (isUsableScaffoldStack(stack)) {
                if (intelligentPicker.getValue()) {
                    if (stack.getCount() > maxStackSize) {
                        maxStackSize = stack.getCount();
                        maxStackSlot = i;
                    }
                } else return i;
            }
        }

        return maxStackSlot;
    }

    private boolean hasAroundBlock() {
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) return false;
        BlockPos base = player.blockPosition();

        for (int dy = 0; dy >= -2; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (isScaffoldSupport(level.getBlockState(base.offset(dx, dy, dz))))
                        return true;
                }
            }
        }

        return false;
    }

    private static boolean isPhysicalKeyDown(String keyName) {
        return InputConstants.isKeyDown(InputConstants.getKey(keyName).getValue());
    }

    private static boolean isSpeedModuleEnabled() {
        for (var feature : FeatureManager.getModules()) {
            if (feature instanceof com.dioxideliteng.module.movement.Speed)
                return feature.isEnabled();
        }
        return false;
    }

    // Resolve the active Scaffold2 instance so static helper methods can read settings.
    private static Scaffold2 inst() {
        for (var feature : FeatureManager.getModules()) {
            if (feature instanceof Scaffold2 s) return s;
        }
        return null;
    }

    // Krs parity (com.instrumentalist.krs.hacks.features.player.Scaffold#shouldSuppressTellyNormalJumpSprintHook):
    // read by the ported Sprint2 module (Krs Sprint parity).
    public static boolean shouldSuppressTellyNormalJumpSprintHook() {
        Scaffold2 s = inst();
        return s != null && s.isEnabled() && s.shouldSuppressTellyNormalJumpSprint();
    }

    // Krs parity (ModuleManager.getModuleState(Scaffold.class) && Scaffold.noSprint.get()):
    // read by the ported Sprint2 module (Krs Sprint parity).
    public static boolean isNoSprintEnabled() {
        Scaffold2 s = inst();
        return s != null && s.isEnabled() && s.noSprint.getValue();
    }

    public static int getHotbarStackSize() {
        return hotbarStackSize;
    }

    public static void setHotbarStackSize(int value) {
        hotbarStackSize = value;
    }

    public static boolean getWasTowering() {
        return wasTowering;
    }

    public static void setWasTowering(boolean value) {
        wasTowering = value;
    }

    public static boolean getStartedScaffold() {
        return startedScaffold;
    }

    public static void setStartedScaffold(boolean value) {
        startedScaffold = value;
    }
}
