package com.dioxidelite.client.render.skia;

import com.dioxidelite.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * DioxideLite's shared visual language.
 *
 * The visual reference is the uploaded minimal design reference: restrained
 * monochrome surfaces, hairline cyan/blue accents, large negative space,
 * outline-first controls, compact HUD blocks and motion that feels physical
 * instead of flashy. This class intentionally implements the design language
 * rather than copying the reference client's assets or source.
 */
public final class DioxideLiteVisuals {
    public static final int WHITE = 0xF5F8FF;
    public static final int MUTED = 0x9DA8B8;
    public static final int CYAN = 0x78CFFF;
    public static final int CYAN_BRIGHT = 0xA7E5FF;
    public static final int BLACK = 0x070A0F;

    private DioxideLiteVisuals() {}

    public static void glass(Canvas canvas, float x, float y, float w, float h, float radius, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0) return;
        LiquidGlassRenderer.drawSurface(canvas, null, x, y, w, h, radius, alpha);
    }

    /** A cheap card for dense UI lists. It does not invoke framebuffer blur. */
    public static void card(Canvas canvas, float x, float y, float w, float h, float radius,
                            float alpha, boolean hovered, boolean selected) {
        if (Config.visualStyle == Config.VisualStyle.SIGNATURE || Config.liquidGlassAllVisuals) {
            LiquidGlassRenderer.drawSurface(canvas, null, x, y, w, h, radius, alpha * (hovered ? 1.0f : .82f));
        }
        Paint p = new Paint().setAntiAlias(true);
        int base = selected ? 0x13243A : 0x0C1119;
        float fillAlpha = alpha * (hovered ? 0.76f : 0.60f);
        p.setColor(withAlpha(base, fillAlpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), p);

        // A very thin top plane gives the card the same optical layering as the
        // reference without turning every row into a bright glass slab.
        p.setColor(withAlpha(0xFFFFFF, alpha * (hovered ? 0.075f : 0.035f)));
        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(x, y, w, h, radius), true);
        canvas.drawRect(Rect.makeXYWH(x + 1f, y + 1f, Math.max(0f, w - 2f), 1f), p);
        canvas.restore();

        p.setMode(PaintMode.STROKE);
        p.setStrokeWidth(selected ? 1.15f : 0.75f);
        p.setColor(withAlpha(selected ? CYAN_BRIGHT : 0xD7E4F5,
                alpha * (selected ? 0.62f : (hovered ? 0.20f : 0.10f))));
        canvas.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, Math.max(0f, w - 1f), Math.max(0f, h - 1f), radius), p);
        p.close();
    }

    public static void outline(Canvas canvas, float x, float y, float w, float h, float radius,
                               int rgb, float alpha, float stroke) {
        Paint p = new Paint().setAntiAlias(true);
        p.setMode(PaintMode.STROKE);
        p.setStrokeWidth(stroke);
        p.setColor(withAlpha(rgb, alpha));
        canvas.drawRRect(RRect.makeXYWH(x + stroke * .5f, y + stroke * .5f,
                Math.max(0f, w - stroke), Math.max(0f, h - stroke), radius), p);
        p.close();
    }

    public static void accentLine(Canvas canvas, float x, float y, float w, float alpha) {
        Paint p = new Paint().setAntiAlias(true);
        p.setColor(withAlpha(CYAN, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, w, 1.5f, 0.75f), p);
        p.close();
    }

    public static void dot(Canvas canvas, float x, float y, float radius, float alpha, boolean active) {
        Paint p = new Paint().setAntiAlias(true);
        p.setColor(withAlpha(active ? CYAN_BRIGHT : 0x8290A5, alpha));
        canvas.drawCircle(x, y, radius, p);
        if (active) {
            p.setColor(withAlpha(CYAN, alpha * 0.16f));
            canvas.drawCircle(x, y, radius * 2.8f, p);
        }
        p.close();
    }

    public static int text(float alpha) { return withAlpha(WHITE, alpha); }
    public static int muted(float alpha) { return withAlpha(MUTED, alpha); }
    public static int accent(float alpha) { return withAlpha(CYAN_BRIGHT, alpha); }

    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------
    // Native GuiGraphics variants (v1.8 render rework).
    // The ClickGUI no longer uses the Skia/OpenGL path on any platform:
    // every panel, card and accent is drawn with plain GuiGraphics fills
    // and outlines, which renders identically on Windows/Linux/macOS and
    // costs a fraction of the old per-frame Skia submission.
    // ------------------------------------------------------------------

    /** A glass-like panel without framebuffer blur: dark translucent fill + hairline border. */
    public static void glassFast(GuiGraphics g, int x, int y, int w, int h, float alpha,
                                 int baseRgb, float baseFillAlpha, int borderRgb, float borderAlpha) {
        if (w <= 0 || h <= 0 || alpha <= 0f) return;
        g.fill(x, y, x + w, y + h, withAlpha(baseRgb, baseFillAlpha * alpha));
        g.renderOutline(x, y, w, h, withAlpha(borderRgb, borderAlpha * alpha));
    }

    /** A cheap card for dense UI lists (native variant of {@link #card}). */
    public static void cardFast(GuiGraphics g, int x, int y, int w, int h, float alpha, boolean hovered, boolean selected) {
        if (w <= 0 || h <= 0 || alpha <= 0f) return;
        int base = selected ? 0x13243A : 0x0C1119;
        float fillAlpha = alpha * (hovered ? 0.76f : 0.60f);
        g.fill(x, y, x + w, y + h, withAlpha(base, fillAlpha));
        // Very thin top plane keeps the optical layering of the old glass cards.
        g.fill(x + 1, y + 1, x + w - 1, y + 2, withAlpha(0xFFFFFF, alpha * (hovered ? 0.075f : 0.035f)));
        int border = selected ? CYAN_BRIGHT : 0xD7E4F5;
        float borderAlpha = alpha * (selected ? 0.62f : (hovered ? 0.20f : 0.10f));
        g.renderOutline(x, y, w, h, withAlpha(border, borderAlpha));
    }

    /** Hairline outline (native variant of {@link #outline}). */
    public static void outlineFast(GuiGraphics g, int x, int y, int w, int h, int rgb, float alpha) {
        g.renderOutline(x, y, w, h, withAlpha(rgb, alpha));
    }

    /** Accent underline (native variant of {@link #accentLine}). */
    public static void accentLineFast(GuiGraphics g, int x, int y, int w, float alpha) {
        g.fill(x, y, x + w, y + 2, withAlpha(CYAN, alpha));
    }

    /** Status dot rendered as a small square block (native variant of {@link #dot}). */
    public static void dotFast(GuiGraphics g, int x, int y, int radius, float alpha, boolean active) {
        g.fill(x - radius, y - radius, x + radius, y + radius, withAlpha(active ? CYAN_BRIGHT : 0x8290A5, alpha));
        if (active) {
            int halo = Math.max(1, Math.round(radius * 2.8f));
            g.fill(x - halo, y - halo, x + halo, y + halo, withAlpha(CYAN, alpha * 0.16f));
        }
    }
}
