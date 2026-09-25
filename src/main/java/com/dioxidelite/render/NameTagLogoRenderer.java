package com.dioxidelite.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.modules.player.IrcModule;
import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.module.modules.render.NameTags;
import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
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
            if (pos.distanceToSqr(camPos) > PROJECTION_FAR_SQ) continue;

            Vector3f projected = WorldToScreen.getWorldPositionToScreen(
                    new Vec3(pos.x, anchorY, pos.z));
            if (projected == null || projected.z < 0.0F || projected.z > 1.0F) continue;

            float gx = projected.x;
            float gy = projected.y;
            float size = LegendWatch.INSTANCE.clientLogoSize().get().floatValue();
            float tagScale = NameTags.INSTANCE.scale.get().floatValue();
            float boxWidth = NameTags.INSTANCE.getTagBoxWidth(player);
            float leftEdge = gx - (boxWidth * 0.5F) * tagScale;
            float drawSize = size * tagScale;
            drawLogo(event.canvas(), leftEdge - drawSize - 4.0F * tagScale,
                    gy - drawSize * 0.5F - 3.0F * tagScale, drawSize);
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
