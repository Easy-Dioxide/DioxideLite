package com.dioxidelite.client.render.skia;

import com.dioxidelite.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;

/**
 * Lightweight Liquid Glass surface renderer.
 *
 * It deliberately keeps the effect GPU/Skia friendly: a cached framebuffer blur
 * plus translucent layers, a hairline rim and a moving specular band. This is
 * an original implementation inspired by the general visual language of
 * translucent glass, not a copy of any third-party implementation.
 */
public final class LiquidGlassRenderer {
    private LiquidGlassRenderer() {}

    public static void drawSurface(Canvas canvas, Minecraft client,
                                   float x, float y, float w, float h, float radius,
                                   float alpha) {
        if (canvas == null) return;

        Config.VisualStyle style = Config.visualStyle;
        int fill;
        int rim;
        int inner;
        switch (style) {
            case MINIMAL -> {
                fill = withAlpha(0x10131A, 0.94f * alpha);
                rim = withAlpha(0xFFFFFF, 0.10f * alpha);
                inner = withAlpha(0xFFFFFF, 0.035f * alpha);
            }
            case RISE_CLEAN -> {
                fill = withAlpha(0x11151F, 0.92f * alpha);
                rim = withAlpha(0xDDE8FF, 0.16f * alpha);
                inner = withAlpha(0xFFFFFF, 0.045f * alpha);
            }
            case AURORA, SIGNATURE -> {
                fill = withAlpha(0x0A1018, 0.74f * alpha);
                rim = withAlpha(0xD7EEFF, 0.30f * alpha);
                inner = withAlpha(0x8DD8FF, 0.065f * alpha);
            }
            default -> {
                // Liquid Glass must read as a material, not as an opaque dark panel.
                // Keep the body low-alpha so the captured backdrop remains visible.
                float glassBody = 0.26f + 0.14f * Config.glassOpacity;
                fill = withAlpha(0x111827, glassBody * alpha);
                rim = withAlpha(0xEAF6FF, 0.34f * Config.glassHighlight * alpha);
                inner = withAlpha(0xDCEBFF, 0.055f * alpha);
            }
        }

        if (!Config.performanceMode && client != null && style != Config.VisualStyle.MINIMAL) {
            float strength = Config.glassBlur * (style == Config.VisualStyle.LIQUID_GLASS ? 1.0f : 0.65f);
            SkiaBlurRenderer.getInstance().render(
                    client, x, y, w, h, radius,
                    withAlpha(0xAFC7E8, 0.13f * alpha), strength
            );
        }

        Paint p = new Paint().setAntiAlias(true);
        RRect rr = RRect.makeXYWH(x, y, w, h, radius);
        p.setColor(fill);
        canvas.drawRRect(rr, p);

        // Soft inner refraction plate.
        p.setColor(inner);
        canvas.drawRRect(RRect.makeXYWH(x + 1.0f, y + 1.0f, Math.max(0f, w - 2f),
                Math.max(0f, h - 2f), Math.max(2f, radius - 1f)), p);

        // Fine glass rim.
        p.setMode(PaintMode.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(rim);
        canvas.drawRRect(rr, p);

        // Top specular band. Kept intentionally subtle so it reads as glass
        // rather than a bright gradient panel.
        p.setMode(PaintMode.FILL);
        p.setColor(withAlpha(0xFFFFFF,
                0.035f * Config.glassHighlight * alpha));
        canvas.save();
        canvas.clipRRect(rr, true);
        canvas.drawRect(Rect.makeXYWH(x + 2f, y + 1f, Math.max(0f, w - 4f), Math.min(2.5f, h)), p);

        // Optical highlight: a pair of very soft plates rather than a hard
        // gradient. The result reads as a thin glass sheet while remaining
        // inexpensive on older GPUs.
        float sheen = (float) ((Math.sin(System.nanoTime() / 1_000_000_000.0 * 0.75) + 1.0) * 0.5);
        float sheenW = Math.max(16f, w * 0.18f);
        float sheenX = x - sheenW + (w + sheenW * 2f) * sheen;
        p.setColor(withAlpha(Config.visualStyle == Config.VisualStyle.AURORA ? 0x9EDCFF : 0xFFFFFF,
                0.018f * Config.glassHighlight * alpha));
        canvas.drawRect(Rect.makeXYWH(sheenX, y, sheenW, h), p);
        canvas.restore();

        // Bottom rim is intentionally weaker than the top rim, matching the
        // reference's restrained, outline-first hierarchy.
        p.setMode(PaintMode.STROKE);
        p.setStrokeWidth(0.65f);
        p.setColor(withAlpha(0x8FBFE0, 0.11f * alpha));
        canvas.drawRRect(RRect.makeXYWH(x + .75f, y + .75f, Math.max(0f, w - 1.5f), Math.max(0f, h - 1.5f),
                Math.max(1f, radius - .75f)), p);
        p.close();
    }

    public static void drawPill(Canvas canvas, float x, float y, float w, float h, float radius,
                                int tint, float alpha) {
        Paint p = new Paint().setAntiAlias(true);
        p.setColor(withAlpha(tint & 0xFFFFFF, alpha));
        canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), p);
        p.setMode(PaintMode.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(withAlpha(0xFFFFFF, 0.12f * alpha));
        canvas.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, Math.max(0f, w - 1f), Math.max(0f, h - 1f),
                Math.max(1f, radius - .5f)), p);
        p.close();
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
