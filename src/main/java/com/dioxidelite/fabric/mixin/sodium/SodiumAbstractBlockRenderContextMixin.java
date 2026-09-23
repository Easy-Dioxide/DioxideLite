package com.dioxidelite.fabric.mixin.sodium;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/fabric/mixin/sodium/SodiumAbstractBlockRenderContextMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.dioxidelite.module.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class SodiumAbstractBlockRenderContextMixin {

    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void dioxidelite$hookXrayShouldDrawSide(Direction face, CallbackInfoReturnable<Boolean> cir) {
        Boolean decision = Xray.INSTANCE.getSodiumRenderDecision(this.state, this.pos);
        if (decision != null) {
            cir.setReturnValue(decision);
        }
    }

    @Inject(method = "isFaceCulled", at = @At("HEAD"), cancellable = true)
    private void dioxidelite$hookXrayIsFaceCulled(Direction face, CallbackInfoReturnable<Boolean> cir) {
        Boolean decision = Xray.INSTANCE.getSodiumRenderDecision(this.state, this.pos);
        if (decision != null) {
            cir.setReturnValue(!decision);
        }
    }

    @ModifyExpressionValue(method = "renderQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;setRenderType(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;"), require = 0)
    private MutableQuadViewImpl dioxidelite$hookXrayRenderType(MutableQuadViewImpl quad) {
        return Xray.INSTANCE.shouldRenderTranslucentWall(this.state, this.pos) ? quad.setRenderType(ChunkSectionLayer.TRANSLUCENT) : quad;
    }
}
