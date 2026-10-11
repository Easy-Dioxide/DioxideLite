package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fires during collision/shape lookup for a block, allowing shape or state overrides. */
public class EventBlock extends Event {
   private BlockState blockState;
   private BlockPos blockPos;
   private VoxelShape voxelShape;

   public BlockState getBlockState() {
      return this.blockState;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public VoxelShape getVoxelShape() {
      return this.voxelShape;
   }

   public void setVoxelShape(VoxelShape voxelShape) {
      this.voxelShape = voxelShape;
   }

   public EventBlock reset(BlockState blockState, BlockPos blockPos, VoxelShape voxelShape) {
      this.blockState = blockState;
      this.blockPos = blockPos;
      this.voxelShape = voxelShape;
      return this;
   }
}
