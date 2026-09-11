package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.client.render.font.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Lightweight Liquid Glass visual layer using Minecraft's native GUI pipeline. */
public final class DioxideGlassPainter {
    public static final int WHITE = 0xF2F7FA;
    public static final int MUTED = 0x9BA9AE;
    public static final int CYAN = 0x79D9FF;
    public static final int MINT = 0x63E2C2;
    private DioxideGlassPainter() {}

    public static void glass(GuiGraphics g, int x, int y, int w, int h, float alpha, boolean active) {
        int a = clamp(Math.round(255 * alpha));
        // Layered translucent material: no framebuffer capture, so the world remains visible.
        g.fill(x + 2, y + 3, x + w + 2, y + h + 3, argb(a * .10f, 0x02070B));
        g.fill(x, y, x + w, y + h, argb(a * (active ? .43f : .32f), 0x0C141B));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, argb(a * .12f, 0xD8F5FF));
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, argb(a * .06f, 0x5B7885));
        g.renderOutline(x, y, w, h, argb(a * (active ? .54f : .27f), active ? CYAN : 0x7B8F99));
    }

    public static void selectedRail(GuiGraphics g, int x, int y, int w, int h, float alpha, boolean hover) {
        int a = clamp(Math.round(255 * alpha));
        g.fill(x, y, x + w, y + h, argb(a * (hover ? .12f : .08f), 0xC6EFFF));
        g.fill(x, y, x + 2, y + h, argb(a * (hover ? .90f : .66f), CYAN));
        g.fill(x + 2, y, x + 4, y + 1, argb(a * .28f, 0xFFFFFF));
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, argb(a * .12f, CYAN));
    }

    public static void module(GuiGraphics g, int x, int y, int w, int h, float alpha, boolean hover) {
        int a = clamp(Math.round(255 * alpha));
        g.fill(x + 1, y + 2, x + w + 1, y + h + 2, argb(a * .07f, 0x000000));
        g.fill(x, y, x + w, y + h, argb(a * (hover ? .17f : .095f), 0xA7DFFF));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, argb(a * (hover ? .16f : .08f), 0xEFFFFF));
        g.renderOutline(x, y, w, h, argb(a * (hover ? .48f : .18f), hover ? CYAN : 0x7C919B));
    }

    public static void text(GuiGraphics g, String s, int x, int y, float size, int color, float alpha) {
        int c = withAlpha(color, alpha);
        FontRenderer.drawText(g, s, x, y, size, c, FontRenderer.DEFAULT, true);
    }

    public static void accentText(GuiGraphics g, String s, float x, float y, float size, int color, float alpha) {
        FontRenderer.drawGlowText(g, s, x, y, size, withAlpha(color, alpha), FontRenderer.DEFAULT);
    }

    public static void icon(GuiGraphics g, String s, int x, int y, float size, int color, float alpha, String font) {
        FontRenderer.drawText(g, s, x, y, size, withAlpha(color, alpha), font);
    }

    public static void centered(GuiGraphics g, String s, int x, int y, int color, float alpha) {
        FontRenderer.drawCenteredText(g, s, x, y, 10f, withAlpha(color, alpha), FontRenderer.DEFAULT, true);
    }

    public static void centered(GuiGraphics g, String s, int x, int y, float size, int color, float alpha) {
        FontRenderer.drawCenteredText(g, s, x, y, size, withAlpha(color, alpha), FontRenderer.DEFAULT, true);
    }

    public static int withAlpha(int rgb, float alpha) {
        return (clamp(Math.round(255f * alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int argb(float alpha255, int rgb) {
        return (clamp(Math.round(alpha255)) << 24) | (rgb & 0xFFFFFF);
    }

    private static int clamp(int a) { return Math.max(0, Math.min(255, a)); }
}
