package com.dioxidelite.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.VanillaHudRenderEvent;
import com.dioxidelite.render.OverlayGuiRenderer;
import com.dioxidelite.ui.clickgui.PopClickGuiScreen;
import com.dioxidelite.ui.hud.EpsilonHudModule;
import com.dioxidelite.ui.hud.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Shadow
    @Final
    private SubmitNodeCollector submitNodeCollector;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Unique
    private GuiRenderState DioxideLite$overlayRenderState;

    @Unique
    private OverlayGuiRenderer DioxideLite$overlayRenderer;

    @Inject(method = "render", at = @At("TAIL"))
    private void DioxideLite$renderVanillaHud(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        // The mixin also applies to OverlayGuiRenderer; only the game's renderer
        // is allowed to create and invoke the secondary pass.
        if (((GuiRenderer) (Object) this).getClass() != GuiRenderer.class) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean hudEditor = minecraft.screen instanceof HudEditorScreen;
        boolean popClickGui = minecraft.screen instanceof PopClickGuiScreen;
        if (minecraft.level == null || minecraft.player == null
                || minecraft.screen != null && !hudEditor && !popClickGui) return;

        if (DioxideLite$overlayRenderState == null || DioxideLite$overlayRenderer == null) {
            DioxideLite$overlayRenderState = new GuiRenderState();
            DioxideLite$overlayRenderer = new OverlayGuiRenderer(
                    DioxideLite$overlayRenderState,
                    bufferSource,
                    submitNodeCollector,
                    featureRenderDispatcher
            );
        }

        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(
                minecraft, DioxideLite$overlayRenderState, mouseX, mouseY);
        try {
            VanillaHudRenderEvent event = new VanillaHudRenderEvent(
                    graphics,
                    minecraft.getDeltaTracker(),
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight()
            );
            if (hudEditor) {
                EventBus.INSTANCE.postTo(event, subscriber -> subscriber instanceof EpsilonHudModule);
            } else {
                EventBus.INSTANCE.post(event);
            }
            DioxideLite$overlayRenderer.render(fogBuffer);
        } finally {
            DioxideLite$overlayRenderer.endFrame();
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void DioxideLite$closeVanillaHudRenderer(CallbackInfo ci) {
        if (((GuiRenderer) (Object) this).getClass() != GuiRenderer.class) return;
        if (DioxideLite$overlayRenderer != null) {
            DioxideLite$overlayRenderer.close();
            DioxideLite$overlayRenderer = null;
            DioxideLite$overlayRenderState = null;
        }
    }
}
