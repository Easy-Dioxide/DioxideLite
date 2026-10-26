package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.level.Xray;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class SodiumBlockRendererMixin extends SodiumAbstractBlockRenderContextMixin {
    @ModifyExpressionValue(
            method = "processQuad",
            at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;getRenderType()Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"),
            require = 0
    )
    private ChunkSectionLayer krs$xrayForceTranslucentLayer(ChunkSectionLayer original) {
        if (krs$shouldApplyXrayOpacity()) {
            return ChunkSectionLayer.TRANSLUCENT;
        }

        return original;
    }

    @ModifyExpressionValue(
            method = "bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V",
            at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;baseColor(I)I"),
            require = 0
    )
    private int krs$xrayModifyQuadColor(int original) {
        if (krs$shouldApplyXrayOpacity()) {
            return original & Xray.getOpacityColorMask();
        }

        return original;
    }
}
