package com.dioxidelite.ui.screen;

import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.UiTheme;
import com.dioxidelite.render.SkijaRenderer;
import net.minecraft.resources.Identifier;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.types.Rect;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Paint;

/**
 * Full-bleed Spotlight overlay that replaces the vanilla {@code LoadingOverlay}
 * (startup + resource reload). Drawn on top of the presented frame, so the
 * dirt/logo/progress bar underneath is fully covered while the vanilla overlay
 * keeps owning its own reload-completion lifecycle.
 */
public final class LoadingScreenDrawer {

    private LoadingScreenDrawer() {
    }

    /**
     * @param progress reload progress in {@code [0, 1]}, or a negative value when
     *                 the true progress is not yet known (renders an indeterminate
     *                 sweep instead of a filled bar).
     */
    public static void draw(Canvas canvas, float width, float height, float progress) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        ScreenBackdrop.draw(canvas, width, height, 176);

        float centerX = width * 0.5F;
        float logoSize = Math.min(76.0F, Math.max(52.0F, width * 0.075F));
        float logoY = height * 0.5F - logoSize - 28.0F;
        drawLogo(canvas, centerX, logoY + logoSize * 0.5F, logoSize);

        String subtitle = "LOADING CLIENT RESOURCES";
        float subtitleTracking = 2.4F;
        float subtitleWidth = UiControls.brandWidth(subtitle, 7.2F, subtitleTracking);
        UiControls.brand(canvas, subtitle, centerX - subtitleWidth * 0.5F,
                logoY + logoSize + 10.0F, 12.0F, UiControls.TEXT_MUTED, 7.2F, subtitleTracking);

        drawProgressBar(canvas, width, centerX, logoY + logoSize + 40.0F, progress);
    }

    private static void drawLogo(Canvas canvas, float centerX, float centerY, float size) {
        try (SkijaRenderer.BorrowedImage borrowed = SkijaRenderer.borrowTexture(
                Identifier.fromNamespaceAndPath("dioxide-lite", "textures/hud/dioxide_logo.png"))) {
            if (borrowed == null) return;
            Image image = borrowed.image();
            Rect src = Rect.makeXYWH(0, 0, image.getWidth(), image.getHeight());
            Rect dst = Rect.makeXYWH(centerX - size * 0.5F, centerY - size * 0.5F, size, size);
            canvas.drawImageRect(image, src, dst, new Paint());
        } catch (Throwable ignored) {
            // Logo is decorative; startup should remain functional if the asset is unavailable.
        }
    }

    private static void drawProgressBar(Canvas canvas, float width, float centerX, float y, float progress) {
        float barWidth = Math.min(268.0F, Math.max(160.0F, width * 0.34F));
        float barHeight = 4.0F;
        float barX = centerX - barWidth * 0.5F;
        float radius = barHeight * 0.5F;
        int accent = UiTheme.accent();

        SkijaUi.rounded(canvas, barX, y, barWidth, barHeight, radius, UiControls.CARD_HOVER);

        if (progress < 0.0F) {
            // Indeterminate: a short segment sweeping left to right.
            float segment = barWidth * 0.32F;
            float travel = barWidth - segment;
            float phase = (System.currentTimeMillis() % 1400L) / 1400.0F;
            float eased = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
            SkijaUi.rounded(canvas, barX + travel * eased, y, segment, barHeight, radius, accent);
        } else {
            float clamped = Math.max(0.0F, Math.min(1.0F, progress));
            if (clamped > 0.0F) {
                SkijaUi.rounded(canvas, barX, y, Math.max(barHeight, barWidth * clamped), barHeight,
                        radius, accent);
            }
            String percent = Math.round(clamped * 100.0F) + "%";
            float percentWidth = SkijaUi.textWidth(percent, 7.6F);
            SkijaUi.text(canvas, percent, centerX - percentWidth * 0.5F, y + 12.0F, 12.0F,
                    UiControls.TEXT_MUTED, 7.6F);
        }
    }
}
