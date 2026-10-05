package com.dioxidelite.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.dioxidelite.module.modules.render.advanced.FogBlur;
import com.dioxidelite.module.modules.render.advanced.NoHurtCamera;
import com.dioxidelite.module.modules.render.advanced.NoRender;
import com.dioxidelite.module.modules.render.advanced.PostProcessing;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.ui.screen.VanillaScreenTheme;
import com.dioxidelite.util.client.ViewBobbingSuppressor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "extractOptions", at = @At("TAIL"))
    private void DioxideLite$disableViewBobbing(CallbackInfo ci) {
        NoRender noRender = NoRender.INSTANCE;
        boolean noRenderBob = noRender.isEnabled() && noRender.handBob.get();
        if (noRenderBob || ViewBobbingSuppressor.isSuppressed()) {
            minecraft.gameRenderer.getGameRenderState().optionsRenderState.bobView = false;
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$disableHurtCamera(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        NoRender noRender = NoRender.INSTANCE;
        if ((noRender.isEnabled() && noRender.hurtCamera.get()) || NoHurtCamera.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void DioxideLite$renderThemedBackdrop(DeltaTracker deltaTracker, boolean renderLevel,
                                            CallbackInfo ci) {
        if (VanillaScreenTheme.applies(minecraft.screen)) {
            SkijaRenderer.renderVanillaScreenTheme();
        }
    }

    /**
     * [v2.2.5 补全] PostProcessing 与 FogBlur 的真实实现：在世界渲染完成后、GUI 渲染前，
     * 对主渲染目标应用原版 blur 后处理链（processBlurEffect），实现真实全屏模糊。
     * 强度按模块设置近似：
     * <ul>
     *   <li>PostProcessing.Blur：blurRadius 1..20 → 处理 1..3 次（半径越大越糊）；</li>
     *   <li>PostProcessing.Bloom：在 Blur 基础上再叠加一次模糊，得到柔和的泛光感；</li>
     *   <li>FogBlur：启用即整体雾状模糊，Distance/Fade 作为强度参考（默认至少 1 次）。</li>
     * </ul>
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void DioxideLite$applyPostProcessing(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PostProcessing pp = PostProcessing.INSTANCE;
        FogBlur fb = FogBlur.INSTANCE;
        boolean ppBlur = pp.isEnabled() && pp.isBlur();
        boolean fog = fb.isEnabled() && fb.shouldBlur();
        if (!ppBlur && !fog) return;

        int passes = 1;
        if (ppBlur) {
            int radius = Math.max(1, pp.getBlurRadius());
            passes = Math.max(1, Math.min(3, (int) Math.ceil(radius / 7.0F)));
        }
        for (int i = 0; i < passes; i++) {
            minecraft.gameRenderer.processBlurEffect();
        }
        if (pp.isEnabled() && pp.isBloom()) {
            minecraft.gameRenderer.processBlurEffect();
        }
    }
}
