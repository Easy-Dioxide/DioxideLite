package com.dioxidelite.client.render.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.Data;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DioxideLite font facade backed by Minecraft's native font atlas.
 *
 * The TTF files remain the same visual assets as the previous Liquid Glass UI,
 * but glyph rasterization is delegated to Minecraft/Blaze3D so ImmediatelyFast
 * can batch the resulting GUI/text work instead of forcing a second rasterizer.
 */
public final class FontRenderer {
    public static final String DEFAULT = "harmony";
    public static final String ICON = "icon";
    public static final String MATERIAL_SYMBOLS = "material_symbols";

    private static final Identifier HARMONY = Identifier.fromNamespaceAndPath("dioxide-lite", "harmony");
    private static final Identifier ICON_FONT = Identifier.fromNamespaceAndPath("dioxide-lite", "icon");
    private static final Identifier MATERIAL = Identifier.fromNamespaceAndPath("dioxide-lite", "material_symbols");
    private static final Map<String, Identifier> FONT_IDS = Map.of(
            DEFAULT, HARMONY,
            ICON, ICON_FONT,
            MATERIAL_SYMBOLS, MATERIAL
    );
    private static final Map<String, Float> WIDTH_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Typeface> SKIA_TYPEFACES = new HashMap<>();
    private static final Map<String, Font> SKIA_FONTS = new HashMap<>();
    private static final FontMgr SKIA_FONT_MGR = FontMgr.getDefault();
    private static final Paint SKIA_TEXT_PAINT = new Paint().setAntiAlias(true);

    // [v1.8 ADDITION] Cached line metrics. See getLineHeight() below for the original.
    // Format: {lineHeight, ascent, descent}. Keyed the same way as `fonts`.
    private static final Map<String, float[]> metricsCache = new HashMap<>();

    static {
        registerSkiaFont(DEFAULT, "/fonts/harmony.ttf");
        registerSkiaFont(ICON, "/fonts/icon.ttf");
        registerSkiaFont(MATERIAL_SYMBOLS, "/fonts/MaterialSymbolsRounded.ttf");
    }

    private FontRenderer() {}

    public static void registerFromResources(String name, String resourcePath) {
        // Kept as a compatibility hook for older modules. Native Minecraft font providers
        // are declared in assets/dioxide-lite/font/*.json and loaded by ResourceManager.
        WIDTH_CACHE.clear();
    }

    public static void registerFromFile(String name, Path path) {
        // Runtime font-file registration is intentionally not supported by the native path.
        // Resource-pack fonts are used instead so there is no CPU-side TTF rasterizer.
        WIDTH_CACHE.clear();
    }


    private static void registerSkiaFont(String name, String resourcePath) {
        try (InputStream in = FontRenderer.class.getResourceAsStream(resourcePath)) {
            if (in == null) return;
            SKIA_TYPEFACES.put(name, SKIA_FONT_MGR.makeFromData(Data.makeFromBytes(in.readAllBytes())));
        } catch (IOException ignored) {
        }
    }

    private static Font skiaFont(String name, float size) {
        String key = name + '#' + Float.floatToIntBits(size);
        Font f = SKIA_FONTS.get(key);
        if (f != null) return f;
        Typeface face = SKIA_TYPEFACES.getOrDefault(name, SKIA_TYPEFACES.get(DEFAULT));
        if (face == null) return null;
        f = new Font(face, size);
        f.setSubpixel(true);
        f.setHinting(FontHinting.SLIGHT);
        f.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        SKIA_FONTS.put(key, f);
        return f;
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb) {
        drawText(canvas, text, x, y, size, argb, DEFAULT);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb, String fontName) {
        if (canvas == null || text == null || text.isEmpty()) return;
        Font f = skiaFont(fontName, size);
        if (f == null) return;
        SKIA_TEXT_PAINT.setColor(argb);
        canvas.drawString(text, x, y, f, SKIA_TEXT_PAINT);
    }

    // ==================================================================================
    // [v1.8 ADDITION] Measured centring helpers.
    //
    // Why: call sites used to centre text with hand-tuned pixel offsets, e.g. in
    // DioxideLiteSignatureClickGuiScreen.drawCenter():
    //     FontRenderer.drawText(c, "DioxideLite", cx - 37f, cy - 7f, 10f, ...);
    //     FontRenderer.drawText(c, NAV[selected].toUpperCase(), cx - 22f, cy + 18f, 7f, ...);
    // Those constants only match one string at one size, which is exactly why the
    // Signature centre label looked off-centre. These helpers measure instead of guessing.
    // ==================================================================================

    /** Draws {@code text} horizontally centred on {@code centerX}, using the default font. */
    public static void drawTextCentered(Canvas canvas, String text, float centerX, float baselineY,
                                        float size, int argb) {
        drawTextCentered(canvas, text, centerX, baselineY, size, argb, DEFAULT);
    }

    /** Draws {@code text} horizontally centred on {@code centerX}. */
    public static void drawTextCentered(Canvas canvas, String text, float centerX, float baselineY,
                                        float size, int argb, String fontName) {
        if (canvas == null || text == null || text.isEmpty()) return;
        float half = measureTextWidth(text, size, fontName) * 0.5f;
        drawText(canvas, text, centerX - half, baselineY, size, argb, fontName);
    }

    /**
     * Draws {@code text} centred on both axes. The baseline is derived from the real font
     * metrics, so the optical centre matches {@code centerY} regardless of size.
     */
    public static void drawTextCenteredBoth(Canvas canvas, String text, float centerX, float centerY,
                                            float size, int argb, String fontName) {
        if (canvas == null || text == null || text.isEmpty()) return;
        float[] m = metrics(fontName, size);
        float ascent = m[1];    // negative
        float descent = m[2];   // positive
        drawTextCentered(canvas, text, centerX, centerY - (ascent + descent) * 0.5f, size, argb, fontName);
    }

    public static void drawSegmented(Canvas canvas, Segment[] segments, float x, float y) {
        float cursor = x;
        for (Segment seg : segments) {
            drawText(canvas, seg.text, cursor, y, seg.size, seg.argb, seg.fontName);
            cursor += measureTextWidth(seg.text, seg.size, seg.fontName) + seg.gap;
        }
        graphics.drawString(mc.font, component, 0, 0, argb, false);
        graphics.pose().popMatrix();
    }

    public static void drawCenteredText(GuiGraphics graphics, String text, float centerX, float y, float size, int argb) {
        drawCenteredText(graphics, text, centerX, y, size, argb, DEFAULT, true);
    }

    public static void drawCenteredText(GuiGraphics graphics, String text, float centerX, float y, float size, int argb, String fontName, boolean shadow) {
        float width = measureTextWidth(text, size, fontName);
        drawText(graphics, text, centerX - width * .5f, y, size, argb, fontName, shadow);
    }

    /**
     * Lightweight accent glow: two low-alpha offset glyph passes plus the crisp
     * foreground glyph. No framebuffer, shader, or Gaussian blur is involved.
     */
    public static void drawGlowText(GuiGraphics graphics, String text, float x, float y, float size, int argb, String fontName) {
        int alpha = argb >>> 24;
        int glow = (Math.max(0, Math.min(72, alpha / 3)) << 24) | (argb & 0xFFFFFF);
        drawText(graphics, text, x - .55f, y, size, glow, fontName, false);
        drawText(graphics, text, x + .55f, y, size, glow, fontName, false);
        drawText(graphics, text, x, y, size, argb, fontName, false);
    }

    public static void drawCenteredGlowText(GuiGraphics graphics, String text, float centerX, float y, float size, int argb, String fontName) {
        float width = measureTextWidth(text, size, fontName);
        drawGlowText(graphics, text, centerX - width * .5f, y, size, argb, fontName);
    }

    public static float measureTextWidth(String text, float size) {
        return measureTextWidth(text, size, DEFAULT);
    }

    public static float measureTextWidth(String text, float size, String fontName) {
        String value = text == null ? "" : text;
        String key = fontName + '#' + Float.floatToIntBits(size) + '#' + value;
        Float cached = WIDTH_CACHE.get(key);
        if (cached != null) return cached;
        float scale = Math.max(.5f, size / 9f);
        float width = Minecraft.getInstance().font.width(component(value, fontName)) * scale;
        if (value.length() <= 48) WIDTH_CACHE.put(key, width);
        return width;
    }

    public static float getLineHeight(float size) {
        return getLineHeight(size, DEFAULT);
    }

    // [v1.8 CHANGE] Original implementation allocated a FontMetrics object on every call:
    //     FontMetrics m = makeFont(fontName, size).getMetrics();
    //     return -m.getAscent() + m.getDescent();
    // getLineHeight() is called from per-row layout code, so this ran many times per frame.
    // Now served from `metricsCache`; identical return value.
    public static float getLineHeight(float size, String fontName) {
        return metrics(fontName, size)[0];
    }

    /** Returns {lineHeight, ascent, descent} for the given font and size, cached. */
    private static float[] metrics(String fontName, float size) {
        String key = fontKey(fontName, size);
        float[] cached = metricsCache.get(key);
        if (cached != null) return cached;
        FontMetrics m = makeFont(fontName, size).getMetrics();
        float[] value = {-m.getAscent() + m.getDescent(), m.getAscent(), m.getDescent()};
        metricsCache.put(key, value);
        return value;
    }

    private static String fontKey(String fontName, float size) {
        return fontName + '#' + Float.floatToIntBits(size);
    }

    private static String widthKey(String fontName, float size, String text) {
        return fontKey(fontName, size) + '#' + text;
    }

    private static void clearCaches(String fontName) {
        fonts.keySet().removeIf(key -> key.startsWith(fontName + '#'));
        widthCache.keySet().removeIf(key -> key.startsWith(fontName + '#'));
        // [v1.8 ADDITION] keep metrics cache in sync when a font is (re)registered.
        metricsCache.keySet().removeIf(key -> key.startsWith(fontName + '#'));
    }

    public static class Segment {
        public final String text;
        public final float size;
        public final int argb;
        public final String fontName;
        public final float gap;

        public Segment(String text, float size, int argb) { this(text, size, argb, DEFAULT, 0f); }
        public Segment(String text, float size, int argb, String fontName) { this(text, size, argb, fontName, 0f); }
        public Segment(String text, float size, int argb, String fontName, float gap) {
            this.text = text; this.size = size; this.argb = argb; this.fontName = fontName; this.gap = gap;
        }
    }
}
