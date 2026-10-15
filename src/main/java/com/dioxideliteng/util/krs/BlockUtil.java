package com.dioxideliteng.util.krs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;

/** Ported from Krs (com.instrumentalist.krs.utils.world.BlockUtil). */
public final class BlockUtil {
    public static final BlockUtil INSTANCE = new BlockUtil();

    private BlockUtil() {
    }

    public boolean isFullSurroundedBlock(BlockGetter world, BlockPos pos) {
        if (world == null) return true;

        int stage = 0;
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            var adjacentState = world.getBlockState(adjacentPos);
            if (!adjacentState.is(Blocks.AIR) && !adjacentState.is(Blocks.WATER) && !adjacentState.is(Blocks.LAVA))
                stage++;
            if (stage >= 6)
                return false;
        }

        return true;
    }
}
