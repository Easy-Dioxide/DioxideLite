package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.visual.Rotations;
import com.dioxideliteng.util.krs.GuiEntityRenderGuard;
import com.dioxideliteng.util.krs.RotationManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Krs parity (LivingEntityRendererMixin): rotation render hooks of the ported
 * Rotations module (com.instrumentalist.krs.hacks.features.render.Rotations).
 *
 * <p>Vanilla mode (module enabled + "Vanilla" on): the values vanilla extracts
 * for the local player - body yaw (solveBodyRot), head yaw (Mth.rotLerp) and
 * pitch (LivingEntity#getXRot) - are substituted with the rotation manager's
 * interpolated values, so the avatar renders with the spoofed rotation through
 * vanilla state computation.
 *
 * <p>Direct mode (module enabled + "Vanilla" off) overwrites the render state
 * at the AvatarRenderer tail instead; see MixinAvatarRenderer.
 */
@Mixin({LivingEntityRenderer.class})
public class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> {
   @ModifyExpressionValue(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;solveBodyRot(Lnet/minecraft/world/entity/LivingEntity;FF)F"
      )
   )
   private float dioxideliteng$vanillaBodyYaw(float original, LivingEntity entity, LivingEntityRenderState state, float partialTick) {
      if (dioxideliteng$shouldApplyVanillaRotation(entity))
         return RotationManager.INSTANCE.getInterpolatedBodyYaw(partialTick);

      return original;
   }

   @ModifyExpressionValue(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F",
         ordinal = 0
      )
   )
   private float dioxideliteng$vanillaHeadYaw(float original, LivingEntity entity, LivingEntityRenderState state, float partialTick) {
      if (dioxideliteng$shouldApplyVanillaRotation(entity))
         return RotationManager.INSTANCE.getInterpolatedYaw(partialTick);

      return original;
   }

   @ModifyExpressionValue(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"
      )
   )
   private float dioxideliteng$vanillaPitch(float original, LivingEntity entity, LivingEntityRenderState state, float partialTick) {
      if (dioxideliteng$shouldApplyVanillaRotation(entity))
         return RotationManager.INSTANCE.getInterpolatedPitch(partialTick);

      return original;
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;xRot:F",
         shift = Shift.AFTER
      )}
   )
   private void dioxideliteng$extractManagedRotation(LivingEntity target, LivingEntityRenderState renderState, float partialTick, CallbackInfo callback) {
      if (target != Minecraft.getInstance().player) {
         return;
      }

      if (RotationManager.INSTANCE.isRotating()) {
         // Krs parity: while rotating with the module enabled, the vanilla-mode
         // hooks above already substituted the extracted values, and the
         // direct-mode overwrite runs at AvatarRenderer#extractRenderState
         // RETURN (MixinAvatarRenderer). With the module disabled the avatar
         // renders with vanilla rotations, as in Krs.
         return;
      }

      renderState.xRot = Mth.rotLerp(partialTick, Events.ROTATION.getPreviousPitch(), Events.ROTATION.getPitch());
   }

   @Unique
   private boolean dioxideliteng$shouldApplyVanillaRotation(LivingEntity entity) {
      return !GuiEntityRenderGuard.isActive()
            && RotationManager.INSTANCE.isRotating()
            && Rotations.shouldUseVanilla()
            && entity == Minecraft.getInstance().player;
   }
}
