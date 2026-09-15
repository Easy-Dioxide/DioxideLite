package com.dioxidelite.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.modules.player.IrcModule;
import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector4d;

/**
 * Draws the logo from the final projected name-tag anchor. Unlike the old
 * world-position/focal-length calculation, this uses the same world-to-screen
 * projection path as the client HUD, so camera rotation cannot make the logo
 * drift independently of the name tag.
 */
public final class NameTagLogoRenderer {
    public static final NameTagLogoRenderer INSTANCE = new NameTagLogoRenderer();
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("dioxide-lite", "textures/hud/dioxide_logo.png");
    private static final Paint PAINT = new Paint().setAntiAlias(false);
    private SkijaRenderer.BorrowedImage cachedLogo;

    private NameTagLogoRenderer() {}

    @Listen
    private void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!LegendWatch.INSTANCE.clientLogoEnabled() && !LegendWatch.INSTANCE.ircLogoEnabled()) return;

        for (Player player : mc.level.players()) {
            if (player == null || player.isRemoved()) continue;
            boolean local = player == mc.player;
            if (local && mc.options.getCameraType() == CameraType.FIRST_PERSON) continue;
            String clean = LegendSuffixUtil.cleanUsername(player.getName().getString());
            boolean show = local
                    ? LegendWatch.INSTANCE.clientLogoEnabled()
                    : LegendWatch.INSTANCE.ircLogoEnabled() && IrcModule.isIrcUser(clean);
            if (!show) continue;

            double distanceSq = player.distanceToSqr(mc.gameRenderer.getMainCamera().position());
            if (distanceSq > 256.0) continue;

            // Project a thin box around the actual name-tag attachment height.
            // Using a box instead of a single point keeps the anchor stable in
            // third person when the camera rolls/rotates around the player.
            double top = player.getY() + player.getBbHeight() + 0.52;
            AABB anchor = new AABB(player.getX() - 0.01, top - 0.01, player.getZ() - 0.01,
                    player.getX() + 0.01, top + 0.01, player.getZ() + 0.01);
            Vector4d bounds = WorldToScreen.projectAbsoluteAABBOn2D(anchor);
            if (bounds == null) continue;

            float centerX = (float)((bounds.x + bounds.z) * 0.5);
            float centerY = (float)((bounds.y + bounds.w) * 0.5);
            float size = LegendWatch.INSTANCE.clientLogoSize().get().floatValue();
            var displayed = LegendSuffixUtil.appendIfLegendary(player.getName(), player.getName().getString());
            float textWidth = mc.font.width(displayed);

            // The logo is laid out as a sibling of the name text. It is never
            // independently transformed after this point: rotation only changes
            // the projected name-tag anchor itself.
            float x = centerX - textWidth * 0.5f - size - 4.0f;
            float y = centerY - size * 0.5f;
            drawLogo(event.canvas(), x, y, size);
        }
    }

    private void drawLogo(Canvas canvas, float x, float y, float size) {
        try {
            if (cachedLogo == null) cachedLogo = SkijaRenderer.borrowTexture(LOGO);
            if (cachedLogo == null) return;
            Image image = cachedLogo.image();
            Rect src = Rect.makeXYWH(0, 0, image.getWidth(), image.getHeight());
            Rect dst = Rect.makeXYWH(x, y, size, size);
            PAINT.setAlpha(255).setImageFilter(null);
            canvas.drawImageRect(image, src, dst, SamplingMode.DEFAULT, PAINT, true);
        } catch (Throwable ignored) {
            if (cachedLogo != null) {
                try { cachedLogo.close(); } catch (Throwable ignored2) {}
                cachedLogo = null;
            }
        }
    }
}
