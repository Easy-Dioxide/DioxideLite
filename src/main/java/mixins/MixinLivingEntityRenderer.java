package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.util.krs.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> {
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
         // Krs parity (LivingEntityRendererMixin): render the local avatar with the
         // spoofed rotation. bodyRot is the absolute body yaw, yRot the head yaw
         // relative to the body, xRot the pitch - all interpolated between the
         // previous and current tick values maintained by postMotionVisualTick().
         float bodyYaw = RotationManager.INSTANCE.getInterpolatedBodyYaw(partialTick);
         float headYaw = RotationManager.INSTANCE.getInterpolatedYaw(partialTick);
         renderState.bodyRot = bodyYaw;
         renderState.yRot = Mth.wrapDegrees(headYaw - bodyYaw);
         renderState.xRot = RotationManager.INSTANCE.getInterpolatedPitch(partialTick);
      } else {
         renderState.xRot = Mth.rotLerp(partialTick, Events.ROTATION.getPreviousPitch(), Events.ROTATION.getPitch());
      }
   }
}
