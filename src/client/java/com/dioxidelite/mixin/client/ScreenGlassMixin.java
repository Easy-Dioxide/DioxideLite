package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import com.dioxidelite.client.render.skia.LiquidGlassVisualSystem;
import com.dioxidelite.client.render.skia.SkiaScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared glass material for vanilla-style screens. Skia screens and the main menu opt out. */
@Mixin(Screen.class)
public abstract class ScreenGlassMixin {
    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void dioxide_lite$screenGlass(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Config.liquidGlassAllVisuals) return;
        Screen screen = (Screen) (Object) this;
        if (screen instanceof SkiaScreen || screen instanceof TitleScreen) return;
        if (screen.width <= 120 || screen.height <= 80) return;
        float marginX = Math.max(24f, screen.width * .10f);
        float marginY = Math.max(18f, screen.height * .12f);
        LiquidGlassVisualSystem.renderSurface(graphics, marginX, marginY,
                screen.width - marginX * 2f, screen.height - marginY * 2f,
                Math.min(Config.glassRadius, 18f), .34f);
    }
}
