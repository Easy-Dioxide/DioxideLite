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

    public static float measureSkiaTextWidth(String text, float size, String fontName) {
        Font f = skiaFont(fontName, size);
        return f == null || text == null ? 0f : f.measureTextWidth(text);
    }

    public static Component component(String text) {
        return component(text, DEFAULT);
    }

    public static Component component(String text, String fontName) {
        Identifier id = FONT_IDS.getOrDefault(fontName, HARMONY);
        return Component.literal(text == null ? "" : text).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(id)));
    }

    public static void drawText(GuiGraphics graphics, String text, float x, float y, float size, int argb) {
        drawText(graphics, text, x, y, size, argb, DEFAULT, false);
    }

    public static void drawText(GuiGraphics graphics, String text, float x, float y, float size, int argb, String fontName) {
        drawText(graphics, text, x, y, size, argb, fontName, false);
    }

    /**
     * Native-font text with an optional very small depth shadow.  The shadow is
     * deliberately a second normal GUI draw rather than a blur pass, so it stays
     * cheap and friendly to Minecraft's batching path.
     */
    public static void drawText(GuiGraphics graphics, String text, float x, float y, float size, int argb, String fontName, boolean shadow) {
        Minecraft mc = Minecraft.getInstance();
        String value = text == null ? "" : text;
        float scale = Math.max(.5f, size / 9f);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        Component component = component(value, fontName);
        if (shadow) {
            int sa = ((argb >>> 24) * 38 / 255) << 24;
            graphics.drawString(mc.font, component, 1, 1, sa, false);
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

    public static float getLineHeight(float size, String fontName) {
        return Minecraft.getInstance().font.lineHeight * Math.max(.5f, size / 9f);
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
