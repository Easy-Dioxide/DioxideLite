package com.dioxidelite.client.render.nativeui;

import com.dioxidelite.Config;
import net.minecraft.client.gui.GuiGraphics;

/**
 * DioxideLite native glass material. No FBO capture, shader blur, Skia or direct OpenGL calls.
 * Every primitive goes through Minecraft's GuiGraphics/GUI RenderPipeline path.
 */
public final class NativeGlassVisualSystem {
    private NativeGlassVisualSystem() {}

    public static void renderSurface(GuiGraphics g, float x, float y, float w, float h, float radius, float alpha) {
        render(g, x, y, w, h, alpha, true);
    }

    public static void renderHudDock(GuiGraphics g, float x, float y, float w, float h, float alpha) {
        render(g, x, y, w, h, alpha, false);
    }

    public static void renderChat(GuiGraphics g, int w, int h, boolean compact) {
        float panelH = compact ? 72f : 96f;
        render(g, 8, h - panelH - 8, w - 16, panelH, .72f, false);
    }

    public static void renderContainer(GuiGraphics g, int w, int h) {
        float pw = Math.min(320f, w - 32f);
        float ph = Math.min(220f, h - 32f);
        render(g, (w-pw)/2f, (h-ph)/2f, pw, ph, .46f, true);
    }

    private static void render(GuiGraphics g, float x, float y, float w, float h, float alpha, boolean strong) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.max(1, Math.round(w)), ih = Math.max(1, Math.round(h));
        float opacity = Math.max(.10f, Math.min(1f, Config.glassOpacity));
        float highlight = Math.max(0f, Math.min(1f, Config.glassHighlight));
        boolean reduced = RenderPerformance.reducedEffects();

        // Two primary quads are the cheap path. They preserve the transparent LiquidGlass look.
        g.fill(ix + 2, iy + 2, ix + iw + 2, iy + ih + 2, NativeRender.alpha(0x22000000, alpha * opacity));
        g.fill(ix, iy, ix + iw, iy + ih,
                NativeRender.alpha(strong ? 0x5C0B1219 : 0x48080F15, alpha * opacity));

        if (reduced) {
            g.renderOutline(ix, iy, iw, ih,
                    NativeRender.alpha(0xFF78CFFF, alpha * (strong ? .30f : .20f) * highlight));
            return;
        }

        g.fill(ix + 1, iy + 1, ix + iw - 1, iy + 2,
                NativeRender.alpha(0xCDEBFAFF, alpha * .14f * highlight));
        g.fill(ix + 1, iy + ih - 2, ix + iw - 1, iy + ih - 1,
                NativeRender.alpha(0x233B4A55, alpha * .22f));
        g.renderOutline(ix, iy, iw, ih,
                NativeRender.alpha(0xFF78CFFF, alpha * (strong ? .42f : .26f) * highlight));
        if (iw > 80 && ih > 40) {
            g.fill(ix + 12, iy + 9, ix + iw - 12, iy + 10,
                    NativeRender.alpha(0xB8FFFFFF, alpha * .02f * highlight));
        }
    }
}
