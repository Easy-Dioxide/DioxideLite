package com.dioxidelite.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;

/**
 * Projects world-space positions into GUI-scaled screen space (top-left origin).
 * The result uses the same logical coordinate system as Render2DEvent and the
 * shared Skija canvas; callers must not divide it by the GUI scale again.
 */
public final class WorldToScreen {

    private static final Minecraft mc = Minecraft.getInstance();

    private WorldToScreen() {
    }

    public static Vector3f getWorldPositionToScreen(Vec3 pos) {
        if (mc.level == null || pos == null) return null;
        CameraRenderState cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f viewProjection = new Matrix4f(cameraState.projectionMatrix).mul(cameraState.viewRotationMatrix);
        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        float rx = (float) (pos.x - camera.x);
        float ry = (float) (pos.y - camera.y);
        float rz = (float) (pos.z - camera.z);
        Vector4f clip = new Vector4f(rx, ry, rz, 1.0F).mul(viewProjection);
        if (!Float.isFinite(clip.x) || !Float.isFinite(clip.y) || !Float.isFinite(clip.z)
                || !Float.isFinite(clip.w) || clip.w <= 0.0001F) {
            return null;
        }
        float invW = 1.0F / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;
        float ndcZ = clip.z * invW;
        float guiWidth = mc.getWindow().getGuiScaledWidth();
        float guiHeight = mc.getWindow().getGuiScaledHeight();
        return new Vector3f(
                (ndcX + 1.0F) * 0.5F * guiWidth,
                (1.0F - ndcY) * 0.5F * guiHeight,
                ndcZ);
    }

    public static Vector4d getEntityPositionsOn2D(LivingEntity target, float tickDelta) {
        final Vec3 position = interpolate(target, tickDelta);
        final float width = target.getBbWidth() / 2f;
        final float height = target.getBbHeight() + (target.isCrouching() ? 0.1f : 0.2f);

        final AABB boundingBox = new AABB(
                position.x - width, position.y, position.z - width,
                position.x + width, position.y + height, position.z + width
        );

        return projectAbsoluteAABBOn2D(boundingBox);
    }

    public static Vector4d projectAbsoluteAABBOn2D(AABB absoluteBoundingBox) {
        if (absoluteBoundingBox == null || mc.level == null) return null;
        CameraRenderState cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f viewProjection = new Matrix4f(cameraState.projectionMatrix).mul(cameraState.viewRotationMatrix);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        float guiWidth = mc.getWindow().getGuiScaledWidth();
        float guiHeight = mc.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0.0F || guiHeight <= 0.0F) return null;

        Vector4d result = null;
        for (int i = 0; i < 8; i++) {
            float x = (float) (((i & 1) == 0 ? absoluteBoundingBox.minX : absoluteBoundingBox.maxX) - cameraPos.x);
            float y = (float) (((i & 2) == 0 ? absoluteBoundingBox.minY : absoluteBoundingBox.maxY) - cameraPos.y);
            float z = (float) (((i & 4) == 0 ? absoluteBoundingBox.minZ : absoluteBoundingBox.maxZ) - cameraPos.z);
            Vector4f clip = new Vector4f(x, y, z, 1.0F).mul(viewProjection);
            if (!Float.isFinite(clip.x) || !Float.isFinite(clip.y) || !Float.isFinite(clip.z)
                    || !Float.isFinite(clip.w) || clip.w <= 0.0001F) continue;
            float invW = 1.0F / clip.w;
            float sx = (clip.x * invW + 1.0F) * 0.5F * guiWidth;
            float sy = (1.0F - clip.y * invW) * 0.5F * guiHeight;
            float sz = clip.z * invW;
            if (!Float.isFinite(sx) || !Float.isFinite(sy) || !Float.isFinite(sz) || sz < -1.0F || sz > 1.0F) continue;
            if (result == null) result = new Vector4d(sx, sy, sx, sy);
            else {
                result.x = Math.min(result.x, sx); result.y = Math.min(result.y, sy);
                result.z = Math.max(result.z, sx); result.w = Math.max(result.w, sy);
            }
        }
        return result;
    }

    public static Vector4d projectEntity(final int[] viewport, final Matrix4f matrix, final AABB absoluteBoundingBox, final Vec3 cameraPos) {
        if (viewport == null || matrix == null || absoluteBoundingBox == null || cameraPos == null) return null;
        // Legacy callers are kept source-compatible, but use the same guarded
        // homogeneous projection rules as the main GUI-space path.
        Vector4d result = null;
        Vector4f clip = new Vector4f();
        for (int i = 0; i < 8; i++) {
            float x = (float) (((i & 1) == 0 ? absoluteBoundingBox.minX : absoluteBoundingBox.maxX) - cameraPos.x);
            float y = (float) (((i & 2) == 0 ? absoluteBoundingBox.minY : absoluteBoundingBox.maxY) - cameraPos.y);
            float z = (float) (((i & 4) == 0 ? absoluteBoundingBox.minZ : absoluteBoundingBox.maxZ) - cameraPos.z);
            clip.set(x, y, z, 1.0F).mul(matrix);
            if (!Float.isFinite(clip.x) || !Float.isFinite(clip.y) || !Float.isFinite(clip.z) || !Float.isFinite(clip.w) || clip.w <= 0.0001F) continue;
            float invW = 1.0F / clip.w;
            double sx = (clip.x * invW + 1.0F) * 0.5 * viewport[2];
            double sy = (1.0 - clip.y * invW) * 0.5 * viewport[3];
            double sz = clip.z * invW;
            if (sz < -1.0 || sz > 1.0 || !Double.isFinite(sx) || !Double.isFinite(sy)) continue;
            if (result == null) result = new Vector4d(sx, sy, sx, sy);
            else { result.x = Math.min(result.x, sx); result.y = Math.min(result.y, sy); result.z = Math.max(result.z, sx); result.w = Math.max(result.w, sy); }
        }
        return result;
    }

    public static Vector4d projectEntity(final int[] viewport, final Matrix4f matrix, final AABB absoluteBoundingBox) {
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        return projectEntity(viewport, matrix, absoluteBoundingBox, cameraPos);
    }

    public static Vec3 interpolate(LivingEntity entity, float tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
        return new Vec3(x, y, z);
    }
}
