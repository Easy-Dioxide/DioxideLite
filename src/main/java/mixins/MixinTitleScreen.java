package mixins;

import com.dioxideliteng.ui.mainmenu.screen.DioxideLiteNGTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$showTerminalTitle(CallbackInfo callbackInfo) {
      Minecraft mc = Minecraft.getInstance();
      if (!(mc.gui.screen() instanceof DioxideLiteNGTitleScreen)) {
         mc.gui.setScreen(new DioxideLiteNGTitleScreen());
      }
      callbackInfo.cancel();
   }
}
