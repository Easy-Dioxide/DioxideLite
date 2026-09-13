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
    private static final Paint LOGO_PAINT = new Paint().setAntiAlias(true);

    private NameTagLogoRenderer() {
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getUser() == null) return;
        if (!LegendWatch.INSTANCE.clientLogoEnabled()
                && !LegendWatch.INSTANCE.ircLogoEnabled()) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        double farSq = PROJECTION_FAR_SQ;
        float fovDeg = camera.getFov();
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) fovDeg *= 1.28F;
        final int screenWidth = mc.getWindow().getScreenWidth();
        final int screenHeight = mc.getWindow().getScreenHeight();
        final double guiScale = event.guiScale() <= 0.0 ? 1.0 : event.guiScale();
        final Matrix4f viewRot = camera.getViewRotationMatrix(new Matrix4f());
        final float focal = (screenHeight * 0.5F)
                / (float) Math.tan(Math.toRadians(fovDeg) * 0.5F);
        final Vector3f rel = new Vector3f();

        for (Player player : mc.level.players()) {
            if (player == null || player.isRemoved()) continue;
            String clean = LegendSuffixUtil.cleanUsername(player.getName().getString());
            boolean local = player == mc.player;
            boolean show = local
                    ? LegendWatch.INSTANCE.clientLogoEnabled()
                    : LegendWatch.INSTANCE.ircLogoEnabled() && IrcModule.isIrcUser(clean);
            if (!show) continue;

            Vec3 pos = player.position();
            double anchorY = pos.y + player.getDimensions(player.getPose()).height() + 0.55;
            if (pos.distanceToSqr(camPos) > farSq) continue;

            rel.set((float) (pos.x - camPos.x), (float) (anchorY - camPos.y),
                    (float) (pos.z - camPos.z));
            float rx = rel.x, ry = rel.y, rz = rel.z;
            rel.x = viewRot.m00() * rx + viewRot.m01() * ry + viewRot.m02() * rz;
            rel.y = viewRot.m10() * rx + viewRot.m11() * ry + viewRot.m12() * rz;
            rel.z = viewRot.m20() * rx + viewRot.m21() * ry + viewRot.m22() * rz;
            float depth = -rel.z;
            if (depth < 0.15F) continue;

            float gx = (float) (screenWidth * 0.5 + (rel.x / depth) * focal) / (float) guiScale;
            float gy = (float) (screenHeight * 0.5 - (rel.y / depth) * focal) / (float) guiScale;
            float size = LegendWatch.INSTANCE.clientLogoSize().get().floatValue();
            net.minecraft.network.chat.Component displayed =
                    LegendSuffixUtil.appendIfLegendary(player.getName(), player.getName().getString());
            float textHalfWidth = mc.font.width(displayed) * 0.5F;
            drawLogo(event.canvas(), gx - textHalfWidth - size - 6.0F,
                    gy - size * 0.5F - 3.0F, size);
        }
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
            LOGO_PAINT.setImageFilter(null).setAlpha(255);
            canvas.drawImageRect(image, src, dst, SamplingMode.MITCHELL, LOGO_PAINT, true);
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
