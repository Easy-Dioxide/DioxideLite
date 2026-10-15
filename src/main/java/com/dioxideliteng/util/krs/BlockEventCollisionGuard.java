package com.dioxideliteng.util.krs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Guards the BlockEvent collision dispatch against re-entrancy: while the original
 * collision shape is being sampled, BlockEvent dispatch is bypassed so listeners see
 * the vanilla shape instead of a shape they themselves provided.
 */
public final class BlockEventCollisionGuard {
   private static final ThreadLocal<Boolean> BYPASS_BLOCK_EVENT = ThreadLocal.withInitial(() -> false);

   private BlockEventCollisionGuard() {
   }

   public static boolean isBypassingBlockEvent() {
      return BYPASS_BLOCK_EVENT.get();
   }

   public static VoxelShape getOriginalCollisionShape(BlockState blockState, CollisionGetter collisionGetter, BlockPos blockPos, CollisionContext context) {
      boolean previous = BYPASS_BLOCK_EVENT.get();
      BYPASS_BLOCK_EVENT.set(true);

      try {
         return context.getCollisionShape(blockState, collisionGetter, blockPos);
      } finally {
         BYPASS_BLOCK_EVENT.set(previous);
      }
   }
}
