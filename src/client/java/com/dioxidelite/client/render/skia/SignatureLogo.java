package com.dioxidelite.client.render.skia;

import com.dioxidelite.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

/**
 * DioxideLite brand mark renderer for the Signature ClickGUI theme.
 *
 * <p>Design grid is 100x100 and every constant below is expressed on that grid, so the
 * in-game render and <code>logo/dioxidelite-mark.svg</code> are geometrically identical.
 *
 * <p>The mark is built from four layers:
 * <ol>
 *   <li>a glow halo: one blurred filled circle, alpha driven by {@code Config.signatureLogoGlow};</li>
 *   <li>an outer ring, echoing the radial ClickGUI ring;</li>
 *   <li>the "D" bowl: the right half of an oval ring, produced with {@code clipRect} + {@code drawOval};</li>
 *   <li>the "D" stem: a capsule that butts flat against the bowl's clipped ends.</li>
 * </ol>
 *
 * <p>Every primitive used here is already used elsewhere in this codebase, so no new Skija
 * API surface is introduced. All colours are multiplied by
 * {@code Config.signatureLogoOpacity}, which is what makes the transparency adjustable
 * from the Theme page.
 *
 * <p>Threading: draw on the Minecraft render thread only. The glow uses a Skia
 * {@link ImageFilter} which must not be shared across threads.
 */
public final class SignatureLogo {

    /** Ring colour, matching DioxideLiteSignatureClickGuiScreen.ACCENT. */
    private static final int ACCENT = 0xFF3ED6B4;
    /** Monogram colour, matching the theme's TEXT colour family. */
    private static final int GLYPH = 0xFFEAF8FF;

    /** Design grid width/height. */
    private static final float GRID = 100f;
    /** Outer ring radius and stroke. */
    private static final float RING_RADIUS = 33f;
    private static final float RING_STROKE = 3f;
    /** "D" bowl: oval centre on x, radius, stroke. */
    private static final float BOWL_CX = 40.5f;
    private static final float BOWL_R = 19f;
    private static final float GLYPH_STROKE = 6.5f;
    /** "D" stem capsule. */
    private static final float STEM_X = 37.25f;
    private static final float STEM_W = 6.5f;
    private static final float STEM_Y = 27.75f;
    private static final float STEM_H = 44.5f;
    /** Glow halo radius and blur sigma, on the design grid. */
    private static final float GLOW_RADIUS = 26f;
    private static final float GLOW_SIGMA = 9f;
    private static final float GLOW_ALPHA = 0.55f;

    private SignatureLogo() {}

    /** Draw the mark centred on (cx, cy); {@code halfSize} is half the 100x100 grid extent. */
    public static void draw(Canvas canvas, float cx, float cy, float halfSize) {
        draw(canvas, cx, cy, halfSize, 1f);
    }

    /**
     * Draw the mark centred on (cx, cy).
     *
     * @param halfSize half the mark's extent in pixels, i.e. 15f renders a 30px-wide mark
     * @param alpha    caller alpha (theme fade-in); multiplied by the configured opacity
     */
    public static void draw(Canvas canvas, float cx, float cy, float halfSize, float alpha) {
        if (canvas == null || halfSize <= 0f) return;
        if (!Config.signatureLogoEnabled) return;

        float opacity = clamp01(Config.signatureLogoOpacity) * clamp01(alpha);
        if (opacity <= 0.002f) return;

        // design grid -> pixels
        final float k = (halfSize * 2f) / GRID;
        final float glow = clamp01(Config.signatureLogoGlow);

        Paint p = new Paint().setAntiAlias(true);
        try {
            // ---- Layer 0: glow halo -------------------------------------------------
            // Skipped entirely at glow == 0, so "no glow" costs nothing.
            if (glow > 0.002f) {
                ImageFilter blur = null;
                try {
                    blur = ImageFilter.makeBlur(GLOW_SIGMA * k, GLOW_SIGMA * k,
                            FilterTileMode.CLAMP, null, (Rect) null);
                    p.setImageFilter(blur);
                    p.setColor(withAlpha(ACCENT, GLOW_ALPHA * glow * opacity));
                    canvas.drawCircle(cx, cy, GLOW_RADIUS * k, p);
                } finally {
                    p.setImageFilter(null);
                    if (blur != null) blur.close();
                }
            }

            // ---- Layer 1: outer ring ------------------------------------------------
            p.setMode(PaintMode.STROKE);
            p.setStrokeWidth(RING_STROKE * k);
            p.setColor(withAlpha(ACCENT, 0.92f * opacity));
            canvas.drawCircle(cx, cy, RING_RADIUS * k, p);

            // ---- Layer 2: "D" bowl --------------------------------------------------
            // Right half of the oval ring. The clip plane sits exactly on the bowl's
            // vertical axis, which is also the stem's centre line, so the bowl's flat
            // cut ends land inside the stem and the two shapes merge into one "D".
            canvas.save();
            try {
                canvas.clipRect(Rect.makeXYWH(
                        cx + (BOWL_CX - 50f) * k,
                        cy + (STEM_Y - 50f) * k,
                        (BOWL_R + GLYPH_STROKE / 2f) * k,
                        STEM_H * k));
                p.setMode(PaintMode.STROKE);
                p.setStrokeWidth(GLYPH_STROKE * k);
                p.setColor(withAlpha(GLYPH, opacity));
                canvas.drawOval(Rect.makeXYWH(
                        cx + (BOWL_CX - 50f - BOWL_R) * k,
                        cy - BOWL_R * k,
                        BOWL_R * 2f * k,
                        BOWL_R * 2f * k), p);
            } finally {
                canvas.restore();
            }

            // ---- Layer 3: "D" stem --------------------------------------------------
            p.setMode(PaintMode.FILL);
            p.setColor(withAlpha(GLYPH, opacity));
            canvas.drawRRect(RRect.makeXYWH(
                    cx + (STEM_X - 50f) * k,
                    cy + (STEM_Y - 50f) * k,
                    STEM_W * k,
                    STEM_H * k,
                    STEM_W / 2f * k), p);
        } finally {
            p.close();
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** Multiplies an ARGB colour's alpha channel by {@code a}. Mirrors the theme's helper. */
    private static int withAlpha(int color, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * a)));
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
