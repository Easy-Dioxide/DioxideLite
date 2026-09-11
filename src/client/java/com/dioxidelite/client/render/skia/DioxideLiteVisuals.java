package com.dioxidelite.client.render.skia;

import com.dioxidelite.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

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
}
