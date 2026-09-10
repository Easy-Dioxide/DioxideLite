package com.dioxidelite.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.dioxidelite.client.modules.impl.Render.motionblur.MotionBlurManager;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MotionBlurLevelRendererMixin {
    @Unique private final Matrix4f dioxide_lite$prevModelView = new Matrix4f();
    @Unique private final Matrix4f dioxide_lite$prevProjection = new Matrix4f();
    @Unique private final Matrix4f dioxide_lite$scratchModelView = new Matrix4f();
    @Unique private final Matrix4f dioxide_lite$scratchProjection = new Matrix4f();
    @Unique private double dioxide_lite$prevCamX;
    @Unique private double dioxide_lite$prevCamY;
    @Unique private double dioxide_lite$prevCamZ;
    @Unique private boolean dioxide_lite$previousFrameReady = false;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void dioxide_lite$onRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        boolean blurActive = MotionBlurManager.shouldRun();
        var camPos = camera.position();
        double cx = camPos.x();
        double cy = camPos.y();
        double cz = camPos.z();

        if (!blurActive) {
            MotionBlurManager.clearFrameAllocator();
            dioxide_lite$rememberCurrentFrameState(modelViewMatrix, projectionMatrix, cx, cy, cz);
            return;
        }

        MotionBlurManager.captureAllocator(resourceAllocator);
        MotionBlurManager.beginFrame();
        dioxide_lite$scratchModelView.set(modelViewMatrix);
        dioxide_lite$scratchProjection.set(projectionMatrix);

        if (!dioxide_lite$previousFrameReady) {
            MotionBlurManager.setFrameMotionBlur(dioxide_lite$scratchModelView, dioxide_lite$scratchModelView, dioxide_lite$scratchProjection, dioxide_lite$scratchProjection, 0.0f, 0.0f, 0.0f);
            dioxide_lite$rememberCurrentFrameState(dioxide_lite$scratchModelView, dioxide_lite$scratchProjection, cx, cy, cz);
            return;
        }

        MotionBlurManager.setFrameMotionBlur(
                dioxide_lite$scratchModelView,
                dioxide_lite$prevModelView,
                dioxide_lite$scratchProjection,
                dioxide_lite$prevProjection,
                (float) (cx - dioxide_lite$prevCamX),
                (float) (cy - dioxide_lite$prevCamY),
                (float) (cz - dioxide_lite$prevCamZ));
        dioxide_lite$rememberCurrentFrameState(dioxide_lite$scratchModelView, dioxide_lite$scratchProjection, cx, cy, cz);
    }

    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void dioxide_lite$beforeSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (!MotionBlurManager.shouldRun()) return;
        if (dioxide_lite$shouldUseSpecialSingleBlur()) {
            MotionBlurManager.applyF5EntityRideBlur();
            return;
        }
        MotionBlurManager.applyPreEntityBlur();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void dioxide_lite$onRenderLevelTail(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        if (MotionBlurManager.shouldRun() && !dioxide_lite$shouldUseSpecialSingleBlur()) {
            MotionBlurManager.applyPostRenderVelocityOnly();
        }
    }

    @Unique
    private void dioxide_lite$rememberCurrentFrameState(Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, double cx, double cy, double cz) {
        dioxide_lite$prevModelView.set(modelViewMatrix);
        dioxide_lite$prevProjection.set(projectionMatrix);
        dioxide_lite$prevCamX = cx;
        dioxide_lite$prevCamY = cy;
        dioxide_lite$prevCamZ = cz;
        dioxide_lite$previousFrameReady = true;
    }

    @Unique
    private boolean dioxide_lite$shouldUseSpecialSingleBlur() {
        Minecraft client = Minecraft.getInstance();
        if (client.options.getCameraType() != CameraType.FIRST_PERSON) return true;
        return client.player != null && client.player.isPassenger();
    }
}
