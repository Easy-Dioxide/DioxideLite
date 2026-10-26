package com.dioxideliteng.mixin.krs;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(PipelineCache.class)
public interface PipelineCacheAccessor {

    @Accessor("cache")
    Map<RenderPipeline, CompiledRenderPipeline> krs$getCache();
}
