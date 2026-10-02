package com.dioxidelite.ui.dr;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * DR 版式专用文字：基线定位（与原型 drawText 的 y 语义一致），
 * 默认字体用项目自带的 UI 字体，图标用随 DR 资源打包的 Material Symbols。
 */
public final class DrText {

    public static final String DEFAULT = "dr-default";
    public static final String MATERIAL_SYMBOLS = "dr-material";

    private static final Map<String, Typeface> TYPEFACES = new HashMap<>();
    private static final Map<String, Font> FONTS = new HashMap<>();
    private static final Map<String, Float> WIDTHS = new HashMap<>();
    private static final Paint PAINT = new Paint().setAntiAlias(true);

    static {
        load(DEFAULT, "/assets/dioxide-lite/tritium/fonts/pf_normal.ttf");
        load(MATERIAL_SYMBOLS, "/assets/dioxide-lite/mainmenu/dr/font/MaterialSymbolsRounded.ttf");
    }

    private DrText() {
    }

    private static void load(String name, String resource) {
        try (InputStream is = DrText.class.getResourceAsStream(resource)) {
            if (is == null) {
                return;
            }
            try (Data data = Data.makeFromBytes(is.readAllBytes())) {
                Typeface typeface = FontMgr.getDefault().makeFromData(data);
                if (typeface != null) {
                    TYPEFACES.put(name, typeface);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Font font(String name, float size) {
        String key = name + '#' + Float.floatToIntBits(size);
        Font cached = FONTS.get(key);
        if (cached != null) {
            return cached;
        }
        Typeface typeface = TYPEFACES.getOrDefault(name, TYPEFACES.get(DEFAULT));
        Font font = typeface == null ? new Font() : new Font(typeface, size);
        font.setSize(size).setSubpixel(true).setHinting(FontHinting.SLIGHT).setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        FONTS.put(key, font);
        return font;
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb) {
        drawText(canvas, text, x, y, size, argb, DEFAULT);
    }

    public static void drawText(Canvas canvas, String text, float x, float y, float size, int argb, String fontName) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Font font = font(fontName, size);
        PAINT.setColor(argb);
        canvas.drawString(text, x, y, font, PAINT);
    }

    public static float measureTextWidth(String text, float size) {
        return measureTextWidth(text, size, DEFAULT);
    }

    public static float measureTextWidth(String text, float size, String fontName) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        if (text.length() <= 32) {
            String key = fontName + '#' + Float.floatToIntBits(size) + '#' + text;
            Float cached = WIDTHS.get(key);
            if (cached != null) {
                return cached;
            }
            float width = font(fontName, size).measureTextWidth(text);
            WIDTHS.put(key, width);
            return width;
        }
        return font(fontName, size).measureTextWidth(text);
    }

    public static float getLineHeight(float size) {
        FontMetrics metrics = font(DEFAULT, size).getMetrics();
        return -metrics.getAscent() + metrics.getDescent();
    }

    /** 渲染器关闭时释放原生字体资源。 */
    public static synchronized void close() {
        FONTS.values().forEach(Font::close);
        FONTS.clear();
        WIDTHS.clear();
        TYPEFACES.values().forEach(Typeface::close);
        TYPEFACES.clear();
    }
}
