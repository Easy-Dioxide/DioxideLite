package com.dioxidelite.fabric.mixin.sodium;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/fabric/mixin/sodium/SodiumFluidRendererMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.dioxidelite.module.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public class SodiumFluidRendererMixin {

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;scaleRGB(IF)I"), require = 0)
    private int dioxidelite$hookXrayFluidOpacity(int color, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true, ordinal = 0) BlockPos pos, @Local(argsOnly = true) FluidState fluidState) {
        BlockState xrayState = state != null ? state : fluidState.createLegacyBlock();
        return Xray.INSTANCE.shouldApplyWallOpacity(xrayState, pos) ? Xray.INSTANCE.applyWallAlpha(color) : color;
    }
}
