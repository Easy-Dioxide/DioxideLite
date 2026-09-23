package com.dioxidelite.fabric.mixin.sodium;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/fabric/mixin/sodium/SodiumBlockRendererMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.dioxidelite.module.modules.render.Xray;
import com.dioxidelite.util.render.XraySectionCompilerHooks;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererMixin extends AbstractBlockRenderContext {

    @Unique
    private BlockAndTintGetter dioxidelite$originalLevel;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void dioxidelite$hookXrayTargetBlockViewHead(CallbackInfo ci, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
        Boolean decision = Xray.INSTANCE.getSodiumRenderDecision(state, pos);
        if (Boolean.TRUE.equals(decision)) {
            dioxidelite$originalLevel = this.level;
            this.level = XraySectionCompilerHooks.targetBlockView(this.level, pos, state);
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void dioxidelite$hookXrayTargetBlockViewReturn(CallbackInfo ci) {
        if (dioxidelite$originalLevel != null) {
            this.level = dioxidelite$originalLevel;
            dioxidelite$originalLevel = null;
        }
    }

    @Inject(method = "bufferQuad", at = @At("HEAD"))
    private void dioxidelite$hookXrayBufferQuadOpacity(MutableQuadViewImpl quad, float[] brightness, Material material, CallbackInfo ci) {
        if (!Xray.INSTANCE.shouldApplyWallOpacity(this.state, this.pos)) {
            return;
        }

        for (int vertex = 0; vertex < 4; vertex++) {
            quad.setColor(vertex, Xray.INSTANCE.applyWallAlpha(quad.baseColor(vertex)));
        }
    }

    @ModifyExpressionValue(method = "processQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forChunkLayer(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"), require = 0)
    private Material dioxidelite$hookXrayMaterial(Material material) {
        return Xray.INSTANCE.shouldRenderTranslucentWall(this.state, this.pos) ? DefaultMaterials.TRANSLUCENT : material;
    }
}
