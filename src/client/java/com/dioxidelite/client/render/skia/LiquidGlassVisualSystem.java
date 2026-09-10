package com.dioxidelite.client.render.skia;

import com.dioxidelite.Config;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Central visual material layer for DioxideLite v1.7.
 *
 * The material model is adapted from the user-provided LiquidGlassShader reference:
 * translucent material, local blur, optical edge, adaptive tint and a restrained
 * chromatic/refraction cue. The implementation is native to DioxideLite/Skija and
 * does not copy the reference project's Minecraft 1.8 rendering code.
 */
public final class LiquidGlassVisualSystem {
    private static long frameBucket = Long.MIN_VALUE;
    private static int blurBudget = 8;
    private LiquidGlassVisualSystem() {}

    public static boolean enabled() {
        return Config.liquidGlassAllVisuals && !Config.performanceMode;
    }

    public static boolean enabledWithoutBlur() {
        return Config.liquidGlassAllVisuals;
    }

    public static void renderSurface(GuiGraphics graphics, float x, float y, float w, float h,
                                     float radius, float alpha) {
        if (graphics == null || !enabledWithoutBlur() || w <= 1f || h <= 1f) return;
        resetBudgetIfNeeded();
        Minecraft mc = Minecraft.getInstance();
        Canvas canvas = SkiaRenderer.beginRegion(Math.round(x), Math.round(y), Math.max(1, Math.round(w)), Math.max(1, Math.round(h)));
        if (canvas == null) return;
        try {
            if (enabled() && blurBudget > 0) {
                blurBudget--;
                LiquidGlassRenderer.drawSurface(canvas, mc, x, y, w, h, radius,
                        Math.min(1f, alpha));
            } else {
                drawLightweightGlass(canvas, x, y, w, h, radius, alpha);
            }
        } finally {
            SkiaRenderer.endRegion(graphics);
        }
    }

    /** Draw a material panel behind an inventory/container's vanilla contents. */
    public static void renderContainer(GuiGraphics graphics, int width, int height) {
        if (!enabledWithoutBlur()) return;
        float marginX = Math.max(18f, width * 0.12f);
        float marginY = Math.max(12f, height * 0.12f);
        float panelW = Math.max(120f, width - marginX * 2f);
        float panelH = Math.max(100f, height - marginY * 2f);
        renderSurface(graphics, marginX, marginY, panelW, panelH,
                Math.min(Config.glassRadius + 2f, 22f), .82f);
    }

    /** Chat glass is deliberately compact so it never covers the whole screen. */
    public static void renderChat(GuiGraphics graphics, int width, int height, boolean focused) {
        if (!enabledWithoutBlur()) return;
        float panelW = Math.min(width * .72f, 420f);
        float panelH = Math.min(height * (focused ? .40f : .28f), focused ? 180f : 130f);
        float x = 8f;
        float y = height - panelH - 8f;
        renderSurface(graphics, x, y, panelW, panelH,
                Math.min(Config.glassRadius, 16f), focused ? .88f : .68f);
    }

    /** Generic dock for custom HUD widgets: shared material, not a forced full-screen overlay. */
    public static void renderHudDock(GuiGraphics graphics, float x, float y, float w, float h, float alpha) {
        if (!enabledWithoutBlur()) return;
        renderSurface(graphics, x, y, w, h, Math.min(Config.glassRadius, 14f), alpha);
    }


    private static void resetBudgetIfNeeded() {
        long bucket = System.nanoTime() / 16_666_667L;
        if (bucket != frameBucket) {
            frameBucket = bucket;
            blurBudget = 8;
        }
    }

    private static void drawLightweightGlass(Canvas c, float x, float y, float w, float h,
                                             float radius, float alpha) {
        Paint fill = new Paint().setAntiAlias(true);
        // Even without a blur slot, retain a genuinely translucent glass body.
        fill.setColor(withAlpha(0x101720, .34f * alpha));
        c.drawRRect(RRect.makeXYWH(x, y, w, h, radius), fill);
        fill.setColor(withAlpha(0xD9ECFF, .07f * Config.glassHighlight * alpha));
        c.drawRRect(RRect.makeXYWH(x + 1f, y + 1f, Math.max(0f, w - 2f), Math.max(0f, h - 2f), Math.max(1f, radius - 1f)), fill);
        fill.setMode(io.github.humbleui.skija.PaintMode.STROKE);
        fill.setStrokeWidth(.9f);
        fill.setColor(withAlpha(0xEAF5FF, .22f * alpha));
        c.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, Math.max(0f, w - 1f), Math.max(0f, h - 1f), Math.max(1f, radius - .5f)), fill);
        fill.close();
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
