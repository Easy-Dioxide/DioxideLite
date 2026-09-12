package com.dioxidelite.mixin;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.accessor.MinecraftSessionAccessor;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.ui.SkijaScreen;
import com.dioxidelite.ui.clickgui.PopClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Connects DioxideLite's visual runtime to Minecraft's client tick/render loop.
 * No gameplay automation is installed here: only visual event dispatch,
 * title branding, Skija screen rendering and HUD overlay rendering are kept.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftSessionAccessor {

    @Mutable
    @Final
    @Shadow
    private User user;

    @Override
    public void DioxideLite$setUser(User user) {
        this.user = user;
    }

    @ModifyArg(
            method = "updateTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;setTitle(Ljava/lang/String;)V"))
    private String DioxideLite$title(String title) {
        return DioxideLite.NAME + " " + DioxideLite.VERSION;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void DioxideLite$preTick(CallbackInfo ci) {
        EventBus.INSTANCE.post(new TickEvent.Pre());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void DioxideLite$postTick(CallbackInfo ci) {
        EventBus.INSTANCE.post(new TickEvent.Post());
    }

    @Inject(
            method = "renderFrame(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V",
                    shift = At.Shift.AFTER))
    private void DioxideLite$renderSkija(boolean advanceGameTime, CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        Overlay overlay = minecraft.getOverlay();

        if (overlay instanceof LoadingOverlay loading) {
            float progress = loading instanceof LoadingOverlayAccessor accessor
                    ? accessor.DioxideLite$getCurrentProgress()
                    : -1.0F;
            SkijaRenderer.renderLoading(progress);
        } else if (minecraft.screen instanceof SkijaScreen skijaScreen) {
            // Pop GUI needs the normal HUD snapshot for its blurred background.
            if (minecraft.screen instanceof PopClickGuiScreen) {
                SkijaRenderer.renderOverlay();
            }
            SkijaRenderer.render(skijaScreen);
        } else if (minecraft.screen == null) {
            SkijaRenderer.renderOverlay();
        }
    }
}
