package com.dioxideliteng.krs.hacks.features.player;

import com.dioxideliteng.krs.events.features.TickEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.render.BlockHighlight;
import com.dioxideliteng.krs.utils.render.RenderColors;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.dioxideliteng.krs.utils.input.Keys;

public class AirPlace extends Module {
    private final BlockHighlight blockHighlight = new BlockHighlight(RenderColors.AIR_PLACE);

    public AirPlace() {
        super("Air Place", ModuleCategory.Player, Keys.KEY_UNKNOWN, false, true);
    }

    public static HitResult createPlacementHit(HitResult original, LocalPlayer player) {
        if (!ModuleManager.getModuleState(AirPlace.class)
                || original == null
                || original.getType() != HitResult.Type.MISS
                || !(original instanceof BlockHitResult miss)
                || player == null
                || !(player.getMainHandItem().getItem() instanceof BlockItem)
                && !(player.getOffhandItem().getItem() instanceof BlockItem)) {
            return original;
        }

        BlockPos target = BlockPos.containing(miss.getLocation());
        return new BlockHitResult(miss.getLocation(), miss.getDirection(), target, false);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        blockHighlight.release();
    }

    @Override
    public void onWorld(WorldEvent event) {
        blockHighlight.release();
    }

    @Override
    public void onTick(TickEvent event) {
        if (mc.level == null || mc.gui.screen() != null) {
            blockHighlight.release();
            return;
        }

        HitResult placementHit = createPlacementHit(mc.hitResult, mc.player);
        if (placementHit == mc.hitResult || !(placementHit instanceof BlockHitResult blockHit)
                || !mc.level.getBlockState(blockHit.getBlockPos()).canBeReplaced()) {
            blockHighlight.release();
            return;
        }

        blockHighlight.setTarget(blockHit.getBlockPos());
    }
}
