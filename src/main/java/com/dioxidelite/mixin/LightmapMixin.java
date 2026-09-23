package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/LightmapMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.dioxidelite.module.modules.render.FullBright;
import com.dioxidelite.module.modules.render.Xray;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public class LightmapMixin {

    @Final
    @Shadow
    private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void dioxidelite$hookLightmap(LightmapRenderState renderState, CallbackInfo ci) {
        if (Xray.INSTANCE.isEnabled() || FullBright.INSTANCE.isGammaMode()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, -1);
            ci.cancel();
        }
    }
}
