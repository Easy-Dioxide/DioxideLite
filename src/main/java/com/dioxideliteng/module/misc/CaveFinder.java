package com.dioxideliteng.module.misc;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.CaveFinder).
 * Krs parity: while active, terrain pipelines (solid_terrain / cutout_terrain) are
 * rendered with front-face culling flipped, turning the world inside out so caves
 * and hollow spaces stand out. Toggling reloads all chunks so the change takes
 * effect immediately.
 * Notable adaptations vs the original:
 *  - reloadVulkanPipelines() is dropped: GpuDevice in 26.3 has no
 *    clearPipelineCache(), and the Krs GraphicsApiCompatibility guard only existed
 *    for that call. reloadChunks() is kept verbatim (mc.levelExtractor.allChanged()).
 */
public class CaveFinder extends Feature {
    private static volatile boolean active;

    public CaveFinder() {
        super("Cave Finder", Category.MISC);
    }

    @Override
    public void onEnable() {
        active = true;
        reloadChunks();
    }

    @Override
    public void onDisable() {
        active = false;
        reloadChunks();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean shouldCullFront(RenderPipeline pipeline) {
        if (!active || pipeline == null || pipeline.getLocation() == null)
            return false;

        String path = pipeline.getLocation().getPath();
        return path.endsWith("pipeline/solid_terrain")
                || path.endsWith("pipeline/cutout_terrain");
    }

    private void reloadChunks() {
        if (mc.level != null)
            mc.levelExtractor.allChanged();
    }
}
