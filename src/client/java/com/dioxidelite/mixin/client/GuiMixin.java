package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import com.dioxidelite.client.modules.impl.Tool.TitleDetector;
import com.dioxidelite.client.modules.impl.Render.NotificationOverlay;
import com.dioxidelite.client.modules.impl.Combat.HitMarkerRenderer;
import com.dioxidelite.client.modules.impl.Render.KeystrokesRenderer;
import com.dioxidelite.client.modules.impl.Render.ArmorHudRenderer;
import com.dioxidelite.client.modules.impl.Render.PotionStatusRenderer;
import com.dioxidelite.client.modules.impl.Render.TargetHudRenderer;
import com.dioxidelite.client.modules.impl.Render.FallDamagePredictor;
import com.dioxidelite.client.modules.impl.Render.DiggingStatusRenderer;
import com.dioxidelite.client.modules.impl.Render.DynamicIslandRenderer;
import com.dioxidelite.client.modules.impl.Render.HudEditOverlay;
import com.dioxidelite.client.modules.impl.Render.DioxideLiteVisualOverlay;
import com.dioxidelite.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.dioxidelite.client.render.skia.SkiaRenderer;
import com.dioxidelite.client.render.skia.SkiaScreen;
import com.dioxidelite.client.render.skia.LiquidGlassVisualSystem;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Component title, CallbackInfo ci) {
        TitleDetector.check(title != null ? title.getString() : null, null);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Component subtitle, CallbackInfo ci) {
        TitleDetector.check(null, subtitle != null ? subtitle.getString() : null);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        Canvas canvas = null;

        boolean skiaScreenOpen = mc.screen instanceof SkiaScreen;
        if (!skiaScreenOpen && NotificationOverlay.getInstance().needsStandaloneCanvas()) {
            int[] bounds = NotificationOverlay.getInstance().getCanvasBounds(guiWidth, guiHeight);
            if (bounds != null) {
                canvas = SkiaRenderer.beginRegion(bounds[0], bounds[1], bounds[2], bounds[3]);
            }
        }

        if (!skiaScreenOpen) {
            NotificationOverlay.getInstance().render(guiGraphics, canvas);
        }
        HitMarkerRenderer.getInstance().render(guiGraphics);
        TargetHudRenderer.getInstance().render(guiGraphics);
        DynamicIslandRenderer.getInstance().render(guiGraphics);
        FallDamagePredictor.getInstance().render(guiGraphics);
        DiggingStatusRenderer.getInstance().render(guiGraphics);
        KeystrokesRenderer.getInstance().render(guiGraphics);
        ArmorHudRenderer.getInstance().render(guiGraphics);
        PotionStatusRenderer.getInstance().render(guiGraphics);
        DioxideLiteVisualOverlay.getInstance().render(guiGraphics);
        HudEditOverlay.getInstance().render(guiGraphics, canvas);

        if (canvas != null) {
            SkiaRenderer.endRegion(guiGraphics);
        }

        BlockCountDisplayRenderer.getInstance().render(guiGraphics, null);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void dioxide_lite$hideVanillaPotionEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PotionStatusRenderer.getInstance().shouldHideVanillaEffects()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void dioxide_lite$hideVignette(GuiGraphics guiGraphics, @Nullable Entity entity, CallbackInfo ci) {
        if (Config.hideVignette) {
            ci.cancel();
        }
    }
}
