package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.Xray).
 * Krs parity: while enabled only xray blocks are drawn; non-visible blocks are
 * either fully hidden or, when Block Opacity > 0 and no Iris shader pack is in
 * use, drawn translucent. Bypass Anti Xray additionally hides surrounded ore
 * blocks so anti-xray plugins keep seeing them. All mixin hooks
 * (hookTransparentOre / shouldDrawSide / isVisible / isOpacityMode /
 * getOpacityColorMask / getOpacityFloat) keep their Krs static surface.
 * Notable adaptations vs the original:
 *  - Krs static settings with onChanged overrides become instance settings; the
 *    change reaction (refreshIrisShaderPackState + reloadChunks) is reproduced by
 *    watching cached values on Events.TICK, since DioxideLite settings have no
 *    onChanged callback.
 *  - Krs ModuleManager.getModuleState(Xray.class) inside reloadChunks replaced
 *    with the static enabled flag (same semantics as Krs's own volatile flag).
 */
public class Xray extends Feature {
    private static volatile Xray instance;
    private static volatile boolean enabled;
    private static volatile boolean irisShaderPackInUse;

    private final BooleanSetting bypassAntiXray = new BooleanSetting("Bypass Anti Xray", this, false);
    private final NumberSetting opacity = new NumberSetting("Block Opacity", this, 0.2, 0.0, 0.99, 0.01);

    private boolean cachedBypassAntiXray;
    private double cachedOpacity;

    public Xray() {
        super("Xray", Category.MISC);
        instance = this;
        this.cachedBypassAntiXray = this.bypassAntiXray.getValue();
        this.cachedOpacity = this.opacity.getValue();
    }

    @Override
    public void onDisable() {
        enabled = false;
        if (mc.level == null) return;
        mc.levelExtractor.allChanged();
    }

    @Override
    public void onEnable() {
        enabled = true;
        refreshIrisShaderPackState();
        this.cachedBypassAntiXray = this.bypassAntiXray.getValue();
        this.cachedOpacity = this.opacity.getValue();
        if (mc.level == null) return;
        mc.levelExtractor.allChanged();
    }

    public static boolean hookTransparentOre(BlockState blockState, boolean original) {
        if (enabled) {
            if (isVisible(blockState, null)) return true;
            return isOpacityMode() && original;
        }
        return original;
    }

    public static Boolean shouldDrawSide(BlockState blockState, BlockPos pos) {
        return shouldDrawSide(blockState, pos, mc.level);
    }

    public static Boolean shouldDrawSide(BlockState blockState, BlockPos pos, BlockGetter level) {
        if (!enabled) return null;

        if (isVisible(blockState, pos, level)) return true;
        if (isOpacityMode()) return null;

        return false;
    }

    public static boolean isVisible(BlockState blockState, BlockPos pos) {
        return isVisible(blockState, pos, mc.level);
    }

    public static boolean isVisible(BlockState blockState, BlockPos pos, BlockGetter level) {
        if (!isXrayBlock(blockState.getBlock())) return false;
        Xray self = instance;
        if (self == null || !self.bypassAntiXray.getValue() || pos == null || level == null) return true;
        return BlockUtil.INSTANCE.isFullSurroundedBlock(level, pos);
    }

    private static boolean isXrayBlock(Block block) {
        return block == Blocks.COAL_ORE
                || block == Blocks.COPPER_ORE
                || block == Blocks.DIAMOND_ORE
                || block == Blocks.EMERALD_ORE
                || block == Blocks.GOLD_ORE
                || block == Blocks.IRON_ORE
                || block == Blocks.LAPIS_ORE
                || block == Blocks.REDSTONE_ORE
                || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.COAL_BLOCK
                || block == Blocks.COPPER_BLOCK.weathering().unaffected()
                || block == Blocks.DIAMOND_BLOCK
                || block == Blocks.EMERALD_BLOCK
                || block == Blocks.GOLD_BLOCK
                || block == Blocks.IRON_BLOCK
                || block == Blocks.LAPIS_BLOCK
                || block == Blocks.REDSTONE_BLOCK
                || block == Blocks.RAW_COPPER_BLOCK
                || block == Blocks.RAW_GOLD_BLOCK
                || block == Blocks.RAW_IRON_BLOCK
                || block == Blocks.ANCIENT_DEBRIS
                || block == Blocks.NETHER_GOLD_ORE
                || block == Blocks.NETHER_QUARTZ_ORE
                || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.QUARTZ_BLOCK
                || block == Blocks.CHEST
                || block == Blocks.DISPENSER
                || block == Blocks.DROPPER
                || block == Blocks.ENDER_CHEST
                || block == Blocks.HOPPER
                || block == Blocks.TRAPPED_CHEST
                || Blocks.DYED_SHULKER_BOX.asList().contains(block)
                || block == Blocks.SHULKER_BOX
                || block == Blocks.BEACON
                || block == Blocks.CRAFTING_TABLE
                || block == Blocks.ENCHANTING_TABLE
                || block == Blocks.FURNACE
                || block == Blocks.FLOWER_POT
                || block == Blocks.JUKEBOX
                || block == Blocks.LODESTONE
                || block == Blocks.RESPAWN_ANCHOR
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.BARREL
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.BREWING_STAND
                || block == Blocks.CARTOGRAPHY_TABLE
                || block == Blocks.COMPOSTER
                || block == Blocks.FLETCHING_TABLE
                || block == Blocks.GRINDSTONE
                || block == Blocks.LECTERN
                || block == Blocks.LOOM
                || block == Blocks.SMITHING_TABLE
                || block == Blocks.SMOKER
                || block == Blocks.STONECUTTER
                || block == Blocks.CAULDRON
                || block == Blocks.LAVA_CAULDRON
                || block == Blocks.WATER_CAULDRON
                || block == Blocks.LAVA
                || block == Blocks.WATER
                || block == Blocks.END_PORTAL
                || block == Blocks.END_PORTAL_FRAME
                || block == Blocks.NETHER_PORTAL
                || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK
                || block == Blocks.BOOKSHELF
                || block == Blocks.CLAY
                || block == Blocks.DRAGON_EGG
                || block == Blocks.FIRE
                || block == Blocks.SPAWNER
                || block == Blocks.TNT;
    }

    public static boolean isOpacityMode() {
        Xray self = instance;
        return enabled && self != null && self.opacity.getValue() > 0f && !irisShaderPackInUse;
    }

    public static int getOpacityColorMask() {
        Xray self = instance;
        float value = self == null ? 0f : (float) self.opacity.getValue();
        return ((int) (Math.max(0f, Math.min(1f, value)) * 255f) << 24) | 0xFFFFFF;
    }

    public static float getOpacityFloat() {
        Xray self = instance;
        float value = self == null ? 0f : (float) self.opacity.getValue();
        return Math.max(0f, Math.min(1f, value));
    }

    private static void reloadChunks() {
        if (mc.level != null && enabled)
            mc.levelExtractor.allChanged();
    }

    private static void refreshIrisShaderPackState() {
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance").invoke(null);
            irisShaderPackInUse = (Boolean) irisApi.getMethod("isShaderPackInUse").invoke(instance);
        } catch (Throwable ignored) {
            irisShaderPackInUse = false;
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.TICK)
            return;

        Xray self = instance;
        if (self == null)
            return;

        // Krs reacted in BooleanValue/FloatValue#onChanged; DioxideLite settings
        // have no change callback, so the same reaction runs on detected changes.
        boolean bypass = self.bypassAntiXray.getValue();
        double opacityValue = self.opacity.getValue();
        if (bypass != self.cachedBypassAntiXray || opacityValue != self.cachedOpacity) {
            self.cachedBypassAntiXray = bypass;
            self.cachedOpacity = opacityValue;
            refreshIrisShaderPackState();
            reloadChunks();
        }
    }
}
