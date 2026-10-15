package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.player.AutoTool;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PlayerUtil;
import com.dioxideliteng.util.krs.RotationManager;
import com.dioxideliteng.util.krs.ToolUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.Nuker).
 * Krs parity: scans a sphere around the player for breakable blocks (or placeable
 * air / note blocks) and destroys or places them at Nuke Speed. Normal mode scans
 * below the player too, Floor mode only at/above foot level. Single Block mines one
 * block at a time with rotations; multi mode drains a distance-sorted queue with
 * packet nukes. AutoTool swaps (spoofed) while breaking, and every state is
 * restored on disable.
 * Notable adaptations vs the original:
 *  - Client.rotationManager replaced with RotationManager.INSTANCE.
 *  - ModuleManager.getModuleState(AutoTool.class) replaced with an AutoTool lookup
 *    (the native AutoTool has no FeatureManager field).
 *  - player.swing(InteractionHand.MAIN_HAND) replaced with the 26.3 three-arg
 *    swing(hand, SwingAnimation.DEFAULT, true) (ServerboundSwingPacket is gone).
 *  - Krs static settings became instance settings; wasBreaking stays static
 *    because getWasBreaking() is part of the static module surface.
 */
public class Nuker extends Feature {
    private static boolean wasBreaking = false;

    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", new String[]{"Normal", "Floor"});
    private final ModeSetting clickMode = new ModeSetting("Click Mode", this, "Break", new String[]{"Break", "Place"});
    private final NumberSetting range = new NumberSetting("Range", this, 4.0, 2.0, 6.0, 1.0);
    private final BooleanSetting singleBlock = new BooleanSetting("Single Block", this, false);
    private final BooleanSetting rotations = new BooleanSetting("Rotations", this, true);
    private final NumberSetting nukeSpeed = new NumberSetting("Nuke Speed", this, 10.0, 1.0, 10.0, 1.0);
    private final BooleanSetting noteBlockOnly = new BooleanSetting("Note Block Only", this, false);

    private final MSTimer nukeTimer = new MSTimer();
    private BlockPos currentBlock = null;
    private final ArrayDeque<BlockPos> blockQueue = new ArrayDeque<>(256);
    private final ArrayList<BlockPos> potentialBlocks = new ArrayList<>(256);
    private long breakStartTime = 0L;
    private int originalSlot = -1;

    private final RotationManager rotationManager = RotationManager.INSTANCE;

    public Nuker() {
        super("Nuker", Category.MISC);
        this.rotations.setVisible(this.singleBlock::getValue);
        this.nukeSpeed.setVisible(() -> !this.singleBlock.getValue());
    }

    @Override
    public void onDisable() {
        blockQueue.clear();
        breakStartTime = 0;
        currentBlock = null;
        if (wasBreaking) {
            PlayerUtil.INSTANCE.stopDestroyBlock();
            this.rotationManager.stopRotation();
        }
        wasBreaking = false;
        if (mc.player != null)
            restoreOriginalSlot(mc.player);
        originalSlot = -1;
        PlayerUtil.INSTANCE.stopSpoof();
    }

    @Override
    public void onEnable() {
    }

    private void restoreOriginalSlot(LocalPlayer player) {
        if (originalSlot != -1) {
            if (originalSlot != player.getInventory().getSelectedSlot())
                player.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
    }

    private boolean shouldHandleBlock(ClientLevel level, BlockPos targetPos, boolean breaking, boolean placing, boolean noteOnly) {
        var state = level.getBlockState(targetPos);

        if (breaking)
            return (!noteOnly || state.getBlock() instanceof NoteBlock) && !state.isAir() && !level.getFluidState(targetPos).is(Fluids.WATER);

        if (!placing) return false;
        return noteOnly ? state.getBlock() instanceof NoteBlock : state.isAir();
    }

    private void collectPotentialBlocks(ClientLevel level, BlockPos playerPos, Vec3 playerPosVec, int startY, int rangeValue, boolean breaking, boolean placing, boolean noteOnly, boolean skipCloseAirPlace) {
        potentialBlocks.clear();
        int rangeSquared = rangeValue * rangeValue;

        for (int x = -rangeValue; x <= rangeValue; x++) {
            for (int y = startY; y <= rangeValue; y++) {
                for (int z = -rangeValue; z <= rangeValue; z++) {
                    if (x * x + y * y + z * z > rangeSquared) continue;

                    BlockPos targetPos = playerPos.offset(x, y, z);
                    if (skipCloseAirPlace && placing && !noteOnly && playerPosVec != null) {
                        double dx = playerPosVec.x - (targetPos.getX() + 0.5);
                        double dy = playerPosVec.y - (targetPos.getY() + 0.5);
                        double dz = playerPosVec.z - (targetPos.getZ() + 0.5);
                        if (dx * dx + dy * dy + dz * dz < 3.0) continue;
                    }

                    if (shouldHandleBlock(level, targetPos, breaking, placing, noteOnly))
                        potentialBlocks.add(targetPos);
                }
            }
        }
    }

    private boolean isWithinRange(LocalPlayer player, BlockPos pos, int rangeValue) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= rangeValue * rangeValue;
    }

    private void selectBestTool(LocalPlayer player, BlockPos pos) {
        if (!isAutoToolEnabled()) return;

        int bestToolSlot = ToolUtil.INSTANCE.findBestTool(pos);
        if (bestToolSlot != -1 && bestToolSlot != player.getInventory().getSelectedSlot()) {
            if (originalSlot == -1)
                originalSlot = player.getInventory().getSelectedSlot();
            PlayerUtil.INSTANCE.doSpoof(originalSlot);
            player.getInventory().setSelectedSlot(bestToolSlot);
        }
    }

    private void resetBreaking(LocalPlayer player, boolean stopDestroying) {
        if (stopDestroying)
            PlayerUtil.INSTANCE.stopDestroyBlock();

        restoreOriginalSlot(player);
        PlayerUtil.INSTANCE.stopSpoof();
        currentBlock = null;
        breakStartTime = 0L;
        blockQueue.clear();
        wasBreaking = false;
    }

    private void handleMultiBlock(LocalPlayer player, ClientLevel level, BlockPos playerPos, int startY, int rangeValue, boolean breaking, boolean placing, boolean noteOnly) {
        if (currentBlock != null && breaking)
            PlayerUtil.INSTANCE.stopDestroyBlock();

        currentBlock = null;
        breakStartTime = 0L;

        collectPotentialBlocks(level, playerPos, null, startY, rangeValue, breaking, placing, noteOnly, false);

        if (potentialBlocks.isEmpty()) {
            resetBreaking(player, breaking);
            this.rotationManager.stopRotation();
            return;
        }

        potentialBlocks.sort((a, b) -> Double.compare(a.distSqr(playerPos), b.distSqr(playerPos)));
        blockQueue.clear();
        blockQueue.addAll(potentialBlocks);

        if (!this.nukeTimer.hasTimePassed(1000L / ((long) this.nukeSpeed.getValue() + 3)))
            return;

        boolean handledBlock = false;
        var gameMode = mc.gameMode;
        if (gameMode == null) return;

        while (!blockQueue.isEmpty()) {
            BlockPos targetPos = blockQueue.poll();
            if (!shouldHandleBlock(level, targetPos, breaking, placing, noteOnly))
                continue;

            BlockHitResult hitResult = PlayerUtil.INSTANCE.blockHitResult(targetPos);

            if (breaking) {
                selectBestTool(player, targetPos);
                PlayerUtil.INSTANCE.nukeBlockWithPacket(hitResult);
                handledBlock = true;
            } else if (placing) {
                if (gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult).consumesAction()) {
                    player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                    handledBlock = true;
                }
            }
        }

        if (handledBlock) {
            wasBreaking = true;
            PlayerUtil.INSTANCE.doSpoof(originalSlot);
        } else {
            resetBreaking(player, breaking);
            this.rotationManager.stopRotation();
        }

        this.nukeTimer.reset();
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.ROTATION)
            return;

        var player = mc.player;
        var level = mc.level;
        var gameMode = mc.gameMode;
        if (player == null || level == null || gameMode == null) return;

        if (wasBreaking)
            PlayerUtil.INSTANCE.doSpoof(originalSlot);

        int rangeValue = (int) this.range.getValue();
        BlockPos playerPos = player.blockPosition();
        int startY = this.mode.is("Normal") ? -rangeValue : 0;
        boolean breaking = this.clickMode.is("Break");
        boolean placing = this.clickMode.is("Place");
        boolean noteOnly = this.noteBlockOnly.getValue();
        boolean single = this.singleBlock.getValue();

        if (!single) {
            handleMultiBlock(player, level, playerPos, startY, rangeValue, breaking, placing, noteOnly);
            return;
        }

        BlockPos current = currentBlock;
        if (current != null) {
            if (!isWithinRange(player, current, rangeValue) || !shouldHandleBlock(level, current, breaking, placing, noteOnly)) {
                resetBreaking(player, breaking);
                current = null;
            } else if (breaking && System.currentTimeMillis() - breakStartTime > 5000L) {
                resetBreaking(player, true);
                current = null;
            } else if (placing && !level.getBlockState(current).isAir()) {
                resetBreaking(player, false);
                current = null;
            }
        }

        if (current == null) {
            collectPotentialBlocks(level, playerPos, single ? player.position() : null, startY, rangeValue, breaking, placing, noteOnly, single);

            if (potentialBlocks.isEmpty()) {
                resetBreaking(player, breaking);
                this.rotationManager.stopRotation();
                return;
            }

            potentialBlocks.sort((a, b) -> Double.compare(a.distSqr(playerPos), b.distSqr(playerPos)));
            currentBlock = potentialBlocks.get(0);
            current = currentBlock;
            breakStartTime = System.currentTimeMillis();
        }

        if (current == null || !shouldHandleBlock(level, current, breaking, placing, noteOnly))
            return;

        wasBreaking = true;
        PlayerUtil.INSTANCE.doSpoof(originalSlot);

        if (single && this.rotations.getValue())
            this.rotationManager.faceBlock(current, 180f);

        BlockHitResult hitResult = PlayerUtil.INSTANCE.blockHitResult(current);

        if (breaking) {
            selectBestTool(player, current);

            if (single || this.nukeTimer.hasTimePassed(1000L / ((long) this.nukeSpeed.getValue() + 3))) {
                PlayerUtil.INSTANCE.destroyBlock(hitResult);
                this.nukeTimer.reset();
            }

            if (level.getBlockState(current).isAir()) {
                currentBlock = null;
                breakStartTime = 0L;
            }
        } else if (placing) {
            if (gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult).consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                currentBlock = null;
                breakStartTime = 0L;
            }
        }
    }

    public static boolean getWasBreaking() {
        return wasBreaking;
    }

    private static boolean isAutoToolEnabled() {
        List<Feature> modules = FeatureManager.getModules();
        for (Feature module : modules) {
            if (module instanceof AutoTool)
                return module.isEnabled();
        }
        return false;
    }
}
