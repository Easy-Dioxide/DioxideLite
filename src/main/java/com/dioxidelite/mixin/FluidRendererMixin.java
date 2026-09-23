package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/FluidRendererMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.dioxidelite.module.modules.render.Xray;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin {

    @ModifyExpressionValue(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;scaleRGB(IF)I"))
    private int dioxidelite$hookXrayFluidOpacity(int color, BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState) {
        return Xray.INSTANCE.shouldApplyWallOpacity(blockState, pos) ? Xray.INSTANCE.applyWallAlpha(color) : color;
    }
}
