package mixins;

import com.dioxideliteng.event.EventBus;
import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.util.krs.BlockEventCollisionGuard;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Krs parity: BlockEvent dispatch during block collision iteration. The three
 * ModifyExpressionValue hooks keep the collision iterator alive for blocks whose
 * shape a listener replaced, exactly as Krs's BlockCollisionSpliteratorMixin does.
 */
@Mixin({BlockCollisions.class})
public abstract class MixinBlockCollisions {
   @Shadow
   @Final
   private BlockPos.MutableBlockPos pos;

   @Shadow
   @Final
   private CollisionContext context;

   @Shadow
   @Final
   private CollisionGetter collisionGetter;

   @Unique
   private BlockPos dioxideliteng$cachedBlockEventPos;

   @Unique
   private BlockState dioxideliteng$cachedBlockEventState;

   @Unique
   private boolean dioxideliteng$cachedBlockEventShape;

   @ModifyExpressionValue(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSuffocating(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
   private boolean dioxideliteng$keepShapeForSuffocationCheck(boolean original, @Local BlockState blockState) {
      return original || hasBlockEventShape(blockState);
   }

   @ModifyExpressionValue(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasLargeCollisionShape()Z"))
   private boolean dioxideliteng$keepShapeForLargeShapeCheck(boolean original, @Local BlockState blockState) {
      return original || hasBlockEventShape(blockState);
   }

   @ModifyExpressionValue(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
   private boolean dioxideliteng$keepShapeForMovingPistonCheck(boolean original, @Local BlockState blockState) {
      return original || hasBlockEventShape(blockState);
   }

   private boolean hasBlockEventShape(BlockState blockState) {
      if (this.pos == null || BlockEventCollisionGuard.isBypassingBlockEvent()) {
         return false;
      }

      EventBus bus = com.dioxideliteng.event.Events.bus();
      if (bus == null || !bus.hasListeners(EventBlock.class)) {
         return false;
      }

      if (blockState == this.dioxideliteng$cachedBlockEventState && this.dioxideliteng$cachedBlockEventPos != null && this.dioxideliteng$cachedBlockEventPos.equals(this.pos)) {
         return this.dioxideliteng$cachedBlockEventShape;
      }

      VoxelShape originalShape = BlockEventCollisionGuard.getOriginalCollisionShape(blockState, this.collisionGetter, this.pos, this.context);
      EventBlock event = new EventBlock(blockState, this.pos.immutable(), originalShape);
      event.call();

      this.dioxideliteng$cachedBlockEventState = blockState;
      this.dioxideliteng$cachedBlockEventPos = this.pos.immutable();
      this.dioxideliteng$cachedBlockEventShape = !event.isCancelled()
         && event.getVoxelShape() != null
         && event.getVoxelShape() != originalShape
         && !event.getVoxelShape().isEmpty();
      return this.dioxideliteng$cachedBlockEventShape;
   }
}
