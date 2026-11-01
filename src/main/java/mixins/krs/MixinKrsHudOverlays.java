package mixins.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.render.AntiBlind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AntiBlind's pumpkin overlay hook. KRS's other InGameHud hooks are not needed: its HUD event is raised by
 * DioxideLiteNG's {@code Events.RENDER_2D}, and its status-effect replacement belonged to KRS's own Interface module.
 */
@Mixin(Hud.class)
public abstract class MixinKrsHudOverlays {

    @Inject(at = @At("HEAD"), method = "extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", cancellable = true)
    private void krs$antiBlindOverlay(GuiGraphicsExtractor context, Identifier texture, float opacity, CallbackInfo ci) {
        if (ModuleManager.getModuleState(AntiBlind.class) && AntiBlind.pumpkin.get())
            ci.cancel();
    }
}
