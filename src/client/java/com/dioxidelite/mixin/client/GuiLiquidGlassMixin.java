package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import com.dioxidelite.client.render.skia.LiquidGlassVisualSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Material plate for the vanilla hotbar/decorations, drawn before its contents. */
@Mixin(Gui.class)
public abstract class GuiLiquidGlassMixin {
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
    private void dioxide_lite$liquidGlassHotbar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Config.liquidGlassAllVisuals) return;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        float hotbarW = Math.min(190f, w * .72f);
        float x = (w - hotbarW) * .5f;
        float y = h - 24f;
        LiquidGlassVisualSystem.renderHudDock(graphics, x - 5f, y - 4f, hotbarW + 10f, 24f, .78f);
    }
}
