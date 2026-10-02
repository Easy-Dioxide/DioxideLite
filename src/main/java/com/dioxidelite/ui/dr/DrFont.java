package com.dioxidelite.ui.dr;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * DELTARUNE 主菜单位图字体（fnt_main，8bitoperator JVE，em=12）
 * 图集与字形表自游戏 data.win 导出：字形顶对齐绘制，shift 为横向步进宽，
 * 带 16px 高字形的是 g/j/p/q/y/_ 这类下延字符，其余统一 13px 行高
 */
public final class DrFont {
    private static final int BASE_EM = 12;
    private static final int FALLBACK_ADVANCE = 13;
    private static final int GLYPH_LINE_HEIGHT = 13;
    private static Image atlas;
    private static final Map<Integer, Glyph> glyphs = new HashMap<>();
    private static final Paint glyphPaint = new Paint();

    private record Glyph(int sx, int sy, int w, int h, int shift) {
    }

    static {
        load();
    }

    private DrFont() {
    }

    private static void load() {
        if (atlas != null) return;
        try (InputStream is = DrFont.class.getResourceAsStream("/assets/dioxide-lite/mainmenu/dr/font/fnt_main.png")) {
            if (is != null) {
                atlas = Image.makeFromEncoded(is.readAllBytes());
            }
        } catch (Exception ignored) {
        }
        try (InputStream is = DrFont.class.getResourceAsStream("/assets/dioxide-lite/mainmenu/dr/font/glyphs_fnt_main.csv")) {
            if (is == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length < 7) continue;
                try {
                    glyphs.put(Integer.parseInt(parts[0].trim()), new Glyph(
                            Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()),
                            Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                            Integer.parseInt(parts[5].trim())));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean ready() {
        load();
        return atlas != null && !glyphs.isEmpty();
    }

    public static float measureWidth(String text, float size) {
        load();
        float scale = size / BASE_EM;
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            Glyph g = glyphs.get((int) text.charAt(i));
            width += (g != null ? g.shift() : FALLBACK_ADVANCE) * scale;
        }
        return width;
    }

    /**
     * 超出 maxWidth 就从尾巴开始砍，砍到放得下为止，末尾补一个省略号。
     * 世界名这类玩家自己取的名字能塞几十个字，槽位框只有 210 宽，不砍就会顶出框外。
     *
     * 前缀宽度一次算完再用，别在循环里反复 substring 从头量，长名字会白算一大截。
     *
     * @param ellipsis 省略号本身按图集量宽，图集没有的点号回退到 TTF，宽是量得出来的
     */
    public static String truncate(String text, float size, float maxWidth, String ellipsis) {
        if (text == null || text.isEmpty()) return text;
        float scale = size / BASE_EM;
        int n = text.length();
        float[] prefix = new float[n + 1];
        for (int i = 0; i < n; i++) {
            Glyph g = glyphs.get((int) text.charAt(i));
            prefix[i + 1] = prefix[i] + (g != null ? g.shift() : FALLBACK_ADVANCE) * scale;
        }
        if (prefix[n] <= maxWidth) return text;
        float dots = measureWidth(ellipsis, size);
        // 前缀宽是单调的，砍到第一个放得下的长度就是最长的那条
        for (int end = n - 1; end > 0; end--) {
            if (prefix[end] + dots <= maxWidth) {
                return text.substring(0, end) + ellipsis;
            }
        }
        return ellipsis;
    }

    public static void drawShadowed(Canvas canvas, String text, float x, float y, float size, int argb) {
        // 对应游戏的 draw_text_shadow：黑字偏移 1 逻辑像素，再叠彩色字
        float offset = size / BASE_EM;
        draw(canvas, text, x + offset, y + offset, size, 0xFF000000);
        draw(canvas, text, x, y, size, argb);
    }

    public static void draw(Canvas canvas, String text, float x, float y, float size, int argb) {
        load();
        float scale = size / BASE_EM;
        if (atlas == null || glyphs.isEmpty()) {
            DrText.drawText(canvas, text, x, y + GLYPH_LINE_HEIGHT * scale, GLYPH_LINE_HEIGHT * scale, argb);
            return;
        }
        float pen = x;
        glyphPaint.setColor(argb);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Glyph g = glyphs.get((int) c);
            if (g == null) {
                // 图集没有的字形（中文等）回退到项目 TTF：基线对齐到 8bitoperator 的 y+13，按全宽步进
                DrText.drawText(canvas, String.valueOf(c), pen, y + GLYPH_LINE_HEIGHT * scale, GLYPH_LINE_HEIGHT * scale, argb);
                pen += FALLBACK_ADVANCE * scale;
                continue;
            }
            if (c != ' ') {
                canvas.drawImageRect(atlas,
                        Rect.makeXYWH(g.sx(), g.sy(), g.w(), g.h()),
                        Rect.makeXYWH(pen, y, g.w() * scale, g.h() * scale),
                        // 字形图集是像素图，放大时必须点采样：走 DEFAULT 的双线性会把边缘糊成半透明，
                        // 黑色描边和彩色字各糊一圈叠在一起，看着就是重影
                        DrBackdrop.NEAREST, glyphPaint, true);
            }
            pen += g.shift() * scale;
        }
    }
}
