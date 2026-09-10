package com.dioxidelite.mixin.client;

import com.dioxidelite.client.modules.impl.Render.motionblur.MotionBlurManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MotionBlurGameRendererMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void dioxide_lite$afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        MotionBlurManager.applyTemporalBlur();
        MotionBlurManager.clearFrameAllocator();
    }
}
