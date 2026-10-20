package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.movement.KeepSprint;
import com.dioxideliteng.module.movement.MovementFix;
import com.dioxideliteng.util.krs.MovementUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public class MixinPlayer {
   @Redirect(
      method = {"causeExtraKnockback"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 dioxideliteng$preserveSprintMotion(Vec3 position, double xMultiplier, double yMultiplier, double zMultiplier) {
      KeepSprint keepSprint = FeatureManager.keepSprint;
      double sprintMultiplier = keepSprint.getSprintMotionMultiplier();
      return position.multiply(sprintMultiplier, yMultiplier, sprintMultiplier);
   }

   // Krs parity: UpdateEvent at Player#aiStep HEAD (cancellable), including the
   // MovementUtil.fallTicks bookkeeping Krs performs at the same point.
   @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$updateEvent(CallbackInfo callback) {
      if ((Object) this instanceof LocalPlayer) {
         if (((Player) (Object) this).onGround()) {
            MovementUtil.fallTicks = 0;
         } else {
            MovementUtil.fallTicks++;
         }

         Events.UPDATE.call();
         if (Events.UPDATE.isCancelled()) {
            callback.cancel();
         }
      }
   }

   // Krs parity: BlockEdgeEvent at Player#isStayingOnGroundSurface RETURN; cancelling
   // the event keeps the player on the block surface instead of walking off the edge.
   @Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true)
   private void dioxideliteng$blockEdge(CallbackInfoReturnable<Boolean> callback) {
      if ((Object) this instanceof LocalPlayer) {
         Events.BLOCK_EDGE.call();
         if (Events.BLOCK_EDGE.isCancelled()) {
            callback.setReturnValue(true);
         }
      }
   }

   // Krs parity: Reach module hooks at Player#blockInteractionRange / Player#entityInteractionRange.
   @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
   private double dioxideliteng$blockReach(double original) {
      return (Object) this instanceof LocalPlayer
            ? com.dioxideliteng.module.combat.Reach.hookBlockReach(original)
            : original;
   }

   @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
   private double dioxideliteng$entityReach(double original) {
      return (Object) this instanceof LocalPlayer
            ? com.dioxideliteng.module.combat.Reach.hookEntityReach(original)
            : original;
   }

   // Krs parity: MovementFix swimming look-vector replacement at Player#travel
   // (Krs PlayerEntityMixin movementFixSwimmingLookVector). While the Movement Fix
   // module is active and the rotation manager owns the rotation, the swimming
   // movement direction follows the spoofed rotation instead of the look angle.
   @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
   private Vec3 dioxideliteng$movementFixSwimmingLookVector(Vec3 original) {
      if ((Object) this instanceof LocalPlayer && MovementFix.shouldFixMovement() && ((Player) (Object) this).isSwimming())
         return MovementFix.getMovementLookVector();

      return original;
   }
}
