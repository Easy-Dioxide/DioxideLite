package com.dioxidelite.client.render.nativeui;

import net.minecraft.client.Minecraft;
import com.dioxidelite.client.render.font.FontRenderer;
import net.minecraft.client.gui.GuiGraphics;

/** Lightweight UI primitives backed directly by Minecraft's GuiGraphics renderer. */
public final class NativeRender {
    private NativeRender() {}
    public static int alpha(int color, float a) {
        int base = (color >>> 24) & 255;
        int out = Math.max(0, Math.min(255, Math.round(base * Math.max(0f, Math.min(1f, a)))));
        return (out << 24) | (color & 0xFFFFFF);
    }
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int outline, boolean outlineOnly) {
        if (!outlineOnly) g.fill(x, y, x + w, y + h, fill);
        if (((outline >>> 24) & 255) > 0) g.renderOutline(x, y, Math.max(1, w), Math.max(1, h), outline);
    }
    public static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) g.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, color);
        else if (y1 == y2) g.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, color);
    }
    public static void text(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, FontRenderer.component(text), x, y, color, false);
    }
    public static int textWidth(String text) { return Minecraft.getInstance().font.width(text == null ? "" : text); }
    public static void centered(GuiGraphics g, String text, int cx, int y, int color) {
        text(g, text, cx - textWidth(text) / 2, y, color);
    }
    public static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    public static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
}
