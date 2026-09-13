package com.dioxidelite.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.modules.player.IrcModule;
import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Paints the client logo (the same high-resolution texture the Dynamic Island
 * uses) next to name tags through the shared Skija canvas.
 *
 * <p>Vanilla name tags render through the sprite-text pipeline and cannot
 * display the custom bitmap glyph, so instead of injecting a glyph into the
 * name tag component the logo is projected from the entity's head anchor into
 * GUI space and drawn here every frame. It shows for the local player in third
 * person ({@code Client Logo} in Legend Watch, on by default) and for IRC
 * online users ({@code IRC Logo}, on by default).</p>
 */
public final class NameTagLogoRenderer {

    public static final NameTagLogoRenderer INSTANCE = new NameTagLogoRenderer();

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath(
            "dioxide-lite", "textures/hud/dioxide_logo.png");

    /** Beyond this squared distance the logo is not painted (matches vanilla's ~16 block name tag range). */
    private static final double PROJECTION_FAR_SQ = 16.0 * 16.0;

    private SkijaRenderer.BorrowedImage cachedLogo;

    private NameTagLogoRenderer() {
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getUser() == null) {
            return;
        }
        if (!LegendWatch.INSTANCE.clientLogoEnabled()
                && !LegendWatch.INSTANCE.ircLogoEnabled()) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        double farSq = PROJECTION_FAR_SQ;

        for (Player player : mc.level.players()) {
            if (player == null || player.isRemoved()) {
                continue;
            }
            String clean = LegendSuffixUtil.cleanUsername(player.getName().getString());
            boolean local = player == mc.player;
            boolean show = local
                    ? LegendWatch.INSTANCE.clientLogoEnabled()
                    : LegendWatch.INSTANCE.ircLogoEnabled() && IrcModule.isIrcUser(clean);
            if (!show) {
                continue;
            }
            Vec3 pos = player.position();
            double anchorY = pos.y + player.getDimensions(player.getPose()).height() + 0.55;
            double distSq = pos.distanceToSqr(camPos);
            if (distSq > farSq) {
                continue;
            }

            float fovDeg = camera.getFov();
            // Vanilla expands the FOV while in third person (the projection used
            // for name tags / the world), so mirror that or the projected logo
            // lands above the vanilla name tag.
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                fovDeg *= 1.28F;
            }
            double[] screen = project(camera, camPos, pos.x, anchorY, pos.z,
                    mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(),
                    fovDeg, event.guiScale());
            if (screen == null) {
                continue;
            }
            float gx = (float) screen[0];
            float gy = (float) screen[1];
            float size = LegendWatch.INSTANCE.clientLogoSize().get().floatValue();
            // Measure the exact text rendered on the vanilla name tag (original
            // name + any Legend suffix) so the logo always sits left of the
            // whole text box (projected gx is the text centre), never overlapping.
            net.minecraft.network.chat.Component displayed =
                    LegendSuffixUtil.appendIfLegendary(player.getName(), player.getName().getString());
            float textHalfWidth = mc.font.width(displayed) * 0.5F;
            // Place the logo just left of the name tag text, nudged up ~3px so
            // its optical centre lines up with the text baseline area.
            drawLogo(event.canvas(), gx - textHalfWidth - size - 6.0F,
                    gy - size * 0.5F - 3.0F, size);
        }
    }

    /** Projects a world anchor into GUI-scaled screen coordinates using the
     *  camera's view-rotation matrix. Returns {@code null} when behind the camera. */
    private static double[] project(Camera camera, Vec3 camPos, double wx, double wy, double wz,
                                    int screenWidth, int screenHeight, float fovDeg, double guiScale) {
        Vector3f rel = new Vector3f(
                (float) (wx - camPos.x),
                (float) (wy - camPos.y),
                (float) (wz - camPos.z));
        Matrix4f viewRot = camera.getViewRotationMatrix(new Matrix4f());
        // World -> camera-space rotation (x right, y up, -z forward).
        float x = rel.x, y = rel.y, z = rel.z;
        rel.x = viewRot.m00() * x + viewRot.m01() * y + viewRot.m02() * z;
        rel.y = viewRot.m10() * x + viewRot.m11() * y + viewRot.m12() * z;
        rel.z = viewRot.m20() * x + viewRot.m21() * y + viewRot.m22() * z;

        float depth = -rel.z;
        if (depth < 0.15F) {
            return null;
        }
        float fov = (float) Math.toRadians(fovDeg);
        float focal = (screenHeight * 0.5F) / (float) Math.tan(fov * 0.5F);
        double sx = screenWidth * 0.5 + (rel.x / depth) * focal;
        double sy = screenHeight * 0.5 - (rel.y / depth) * focal;
        if (guiScale <= 0.0) {
            guiScale = 1.0;
        }
        return new double[]{sx / guiScale, sy / guiScale};
    }

    private void drawLogo(Canvas canvas, float x, float y, float size) {
        try {
            if (cachedLogo == null) {
                cachedLogo = SkijaRenderer.borrowTexture(LOGO);
            }
            if (cachedLogo == null) {
                return;
            }
            Image image = cachedLogo.image();
            Rect src = Rect.makeXYWH(0, 0, image.getWidth(), image.getHeight());
            Rect dst = Rect.makeXYWH(x, y, size, size);
            try (Paint paint = new Paint().setAntiAlias(true)) {
                canvas.drawImageRect(image, src, dst, SamplingMode.LINEAR, paint, true);
            }
        } catch (Throwable ignored) {
            if (cachedLogo != null) {
                try {
                    cachedLogo.close();
                } catch (Throwable ignored2) {
                    // ignore
                }
                cachedLogo = null;
            }
        }
    }
}
