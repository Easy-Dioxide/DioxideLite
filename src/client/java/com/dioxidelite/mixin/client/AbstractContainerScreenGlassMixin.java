package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import com.dioxidelite.client.render.skia.LiquidGlassVisualSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Places the glass material after the vanilla container background but before slots/widgets. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenGlassMixin {
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void dioxide_lite$liquidGlassContainer(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Config.liquidGlassAllVisuals) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        LiquidGlassVisualSystem.renderContainer(graphics, screen.width, screen.height);
    }
}
