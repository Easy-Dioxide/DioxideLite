package mixins;

import com.dioxideliteng.module.visual.Rotations;
import com.dioxideliteng.util.krs.GuiEntityRenderGuard;
import com.dioxideliteng.util.krs.RotationManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Krs parity (PlayerEntityRendererMixin): rotation render hooks of the ported
 * Rotations module (com.instrumentalist.krs.hacks.features.render.Rotations).
 *
 * <p>Direct mode (module enabled + "Vanilla" off): while the rotation manager
 * is rotating, the local avatar's render state is overwritten at the tail of
 * extractRenderState - bodyRot the absolute body yaw, yRot the head yaw
 * relative to the body, xRot the pitch. While returning, the body yaw follows
 * the returning animation.
 *
 * <p>The cape body yaw follows the rotation manager whenever the module is
 * enabled, so the cape swings with the spoofed body rotation.
 */
@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("RETURN")
   )
   private void dioxideliteng$clientSideRotations(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo info) {
      if (avatar != Minecraft.getInstance().player) {
         return;
      }

      if (GuiEntityRenderGuard.isActive()) {
         return;
      }

      if (RotationManager.INSTANCE.isRotating() && Rotations.isModuleEnabled() && !Rotations.shouldUseVanilla()) {
         float headYaw = RotationManager.INSTANCE.getInterpolatedYaw(partialTick);
         if (RotationManager.INSTANCE.isReturning()) {
            float bodyYaw = RotationManager.INSTANCE.getInterpolatedReturningBodyYaw(partialTick);
            state.yRot = Mth.wrapDegrees(headYaw - bodyYaw);
            state.bodyRot = bodyYaw;
         } else {
            state.yRot = 0.0F;
            state.bodyRot = headYaw;
         }
         state.xRot = RotationManager.INSTANCE.getInterpolatedPitch(partialTick);
      }
   }

   @ModifyExpressionValue(
      method = "extractCapeState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F"
      )
   )
   private float dioxideliteng$rotationCapeYaw(float original, Avatar avatar, AvatarRenderState state, float partialTick) {
      return dioxideliteng$getCapeBodyYaw(avatar, partialTick, original);
   }

   @Unique
   private static float dioxideliteng$getCapeBodyYaw(Avatar avatar, float partialTick, float fallback) {
      if (GuiEntityRenderGuard.isActive()
            || avatar != Minecraft.getInstance().player
            || !RotationManager.INSTANCE.isRotating()
            || !Rotations.isModuleEnabled())
         return fallback;

      if (Rotations.shouldUseVanilla())
         return RotationManager.INSTANCE.getInterpolatedBodyYaw(partialTick);

      if (RotationManager.INSTANCE.isReturning())
         return RotationManager.INSTANCE.getInterpolatedReturningBodyYaw(partialTick);

      return RotationManager.INSTANCE.getInterpolatedYaw(partialTick);
   }
}
