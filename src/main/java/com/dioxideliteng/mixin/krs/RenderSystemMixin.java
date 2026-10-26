package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.screen.CustomTitleScreen;
import com.dioxideliteng.krs.screen.NanoVGClickGuiScreen;
import com.dioxideliteng.krs.utils.nanovg.NanoVGManager;
import com.dioxideliteng.krs.utils.render.DebugOverlayRenderer;
import com.dioxideliteng.krs.utils.render.GraphicsApiCompatibility;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dioxideliteng.krs.imguimc.imgui.ImguiLoader;

@Mixin(Minecraft.class)
public abstract class RenderSystemMixin implements IMinecraft {

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"
            )
    )
    private void krs$renderCompatibilityOverlaysBeforeSurfaceBlit(boolean renderLevel, CallbackInfo ci) {
        if (GraphicsApiCompatibility.usesCompatibilityRenderer())
            krs$renderLegacyOverlays();
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;present()V"
            )
    )
    private void krs$renderOpenGlOverlaysBeforePresent(boolean renderLevel, CallbackInfo ci) {
        if (!GraphicsApiCompatibility.usesCompatibilityRenderer())
            krs$renderLegacyOverlays();
    }

    private static void krs$renderLegacyOverlays() {
        NanoVGClickGuiScreen.renderDetachedIfNeeded();

        if (GraphicsApiCompatibility.usesCompatibilityRenderer()) {
            if (KrsClient.nanoVgManager != null
                    && (KrsClient.nanoVgManager.hasQueuedRenderers()
                    || CustomTitleScreen.isStartupLoadBarVisible())) {
                GraphicsApiCompatibility.renderOffscreenLayer(
                        GraphicsApiCompatibility.Layer.NANO_VG,
                        () -> {
                            KrsClient.nanoVgManager.renderQueued();
                            CustomTitleScreen.renderStartupLoadBarIfNeeded(KrsClient.nanoVgManager);
                        },
                        KrsClient.nanoVgManager::discardRenderQueue,
                        KrsClient.nanoVgManager.hasWorldAnchoredRenderers()
                );
            }
        } else if (KrsClient.nanoVgManager != null) {
            KrsClient.nanoVgManager.renderQueued();
            CustomTitleScreen.renderStartupLoadBarIfNeeded(KrsClient.nanoVgManager);
        }

        if (NanoVGManager.shouldRenderBelowDebugOverlay() && mc.gameRenderer instanceof DebugOverlayRenderer debugOverlayRenderer) {
            debugOverlayRenderer.krs$renderDebugOverlayOnTop();
        }

        if (GraphicsApiCompatibility.usesCompatibilityRenderer()) {
            if (ImguiLoader.shouldRenderFrame())
                GraphicsApiCompatibility.renderOffscreenLayer(
                        GraphicsApiCompatibility.Layer.IMGUI,
                        ImguiLoader::onFrameRender
                );
        } else {
            ImguiLoader.onFrameRender();
        }
    }
}
