package com.dioxidelite.mixin;

import com.dioxidelite.ui.screen.BakeProgressWatcher;
import com.dioxidelite.ui.screen.VanillaScreenTheme;
import com.dioxidelite.ui.screen.VanillaButtonOverlay;
import com.dioxidelite.render.SkijaRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class VanillaScreenThemeMixin {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void DioxideLite$beginThemedFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                        float partialTick, CallbackInfo ci) {
        if (VanillaScreenTheme.applies((Screen) (Object) this)) {
            VanillaButtonOverlay.beginFrame();
            VanillaScreenTheme.beginFrame((Screen) (Object) this);
        }
    }

    /**
     * 屏幕每帧渲染完成后转发一次轮询。
     *
     * <p>{@code Screen} 是渲染入口的声明类，挂在这里才能覆盖所有界面；
     * 具体界面自己声明的方法（如 {@code OptionsScreen.init}）在父类里是找不到的。
     */
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void DioxideLite$pollBakeableScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                          float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof BakeProgressWatcher watcher) {
            watcher.dioxideLite$pollBake();
        }
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void DioxideLite$finishThemedFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
        if (VanillaScreenTheme.applies((Screen) (Object) this)) {
            VanillaButtonOverlay.endFrame();
            VanillaScreenTheme.drawTransition(graphics);
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$drawThemedBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo ci) {
        if (!VanillaScreenTheme.applies((Screen) (Object) this)) {
            return;
        }
        if (SkijaRenderer.hasFailed()) {
            VanillaScreenTheme.drawFallback(graphics);
        }
        ci.cancel();
    }
}
