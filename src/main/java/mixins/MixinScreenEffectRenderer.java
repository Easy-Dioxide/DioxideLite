package mixins;

import com.dioxideliteng.module.FeatureManager;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ScreenEffectRenderer.class})
public class MixinScreenEffectRenderer {
   @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
   private static void dioxideliteng$hideFireOverlay(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite texture, CallbackInfo ci) {
      if (FeatureManager.antiFire.isEnabled()) {
         ci.cancel();
      }
   }
}
