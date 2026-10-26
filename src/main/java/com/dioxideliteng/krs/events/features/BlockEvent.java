package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockEvent extends EventArgument {
    public final BlockState blockState;
    public final BlockPos blockPos;
    public VoxelShape voxelShape;

    public BlockEvent(BlockState blockState, BlockPos blockPos, VoxelShape voxelShape) {
        this.blockState = blockState;
        this.blockPos = blockPos;
        this.voxelShape = voxelShape;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onBlock(this);
    }
}
