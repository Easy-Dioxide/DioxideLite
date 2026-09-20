package com.dioxidelite.fabric.mixin.sodium;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/fabric/mixin/sodium/SodiumOcclusionCullerMixin.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.module.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = OcclusionCuller.class, remap = false)
public class SodiumOcclusionCullerMixin {

    @ModifyVariable(method = "findVisible", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean dioxidelite$hookXrayDisableOcclusionCulling(boolean useOcclusionCulling) {
        return Xray.INSTANCE.shouldDisableChunkOcclusion() ? false : useOcclusionCulling;
    }
}
