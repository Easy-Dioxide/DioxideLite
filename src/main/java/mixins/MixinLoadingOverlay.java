package mixins;

import com.dioxideliteng.ui.loading.DioxideLiteNGLoadingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.dioxideliteng.util.render.NVGRenderer;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

   @Shadow
   @Final
   private ReloadInstance reload;

   @Shadow private long fadeOutStart;
   @Shadow private long fadeInStart;
   @Shadow @Final private boolean fadeIn;

   @Inject(
      method = {"extractRenderState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$customLoadingScreen(CallbackInfo callbackInfo) {
      Minecraft mc = Minecraft.getInstance();
      if (!NVGRenderer.isAvailable()) return;
      // Keep vanilla tick's completion callback, error handling and screen init intact.
      if (this.fadeIn && this.fadeInStart == -1L) this.fadeInStart = Util.getMillis();
      boolean done = this.reload.isDone();
      DioxideLiteNGLoadingState.progress = this.reload.getActualProgress();
      DioxideLiteNGLoadingState.done = done;

      if (this.fadeOutStart != -1L && Util.getMillis() - this.fadeOutStart > 400L) mc.gui.setOverlay(null);
      callbackInfo.cancel();
   }
}
