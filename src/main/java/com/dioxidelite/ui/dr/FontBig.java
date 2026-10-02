package com.dioxidelite.ui.dr;

/**
 * fnt_mainbig 的 draw_text_ext：字形从汉化版字体逐字导出
 * （第/章/2/3/4），步进宽 shift 汉字 26、数字 14，数字顶部相对下移 2px。
 */
final class FontBig {
    private static final int STEP_HAN = 26;
    private static final int STEP_NUM = 14;
    private static boolean loaded;
    private static Spr di;
    private static Spr zhang;
    private static Spr n2;
    private static Spr n3;
    private static Spr n4;

    private FontBig() {
    }

    private static void ensure() {
        if (loaded) {
            return;
        }
        loaded = true;
        di = Spr.of1("glyph_di", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/glyph/di.png");
        zhang = Spr.of1("glyph_zhang", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/glyph/zhang.png");
        n2 = Spr.of1("glyph_n2", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/glyph/n2.png");
        n3 = Spr.of1("glyph_n3", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/glyph/n3.png");
        n4 = Spr.of1("glyph_n4", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/glyph/n4.png");
    }

    /** draw_set_font(fnt_mainbig); draw_text_ext(x, y, "第N章", 10, 900)。 */
    static void drawChapterText(IntroScene scene, int chapter, double x, double y, float alpha) {
        if (alpha <= 0f || chapter < 2 || chapter > 5) {
            return;
        }
        ensure();
        Spr num = switch (chapter) {
            case 2 -> n2;
            case 3 -> n3;
            default -> n4;
        };
        drawGlyph(scene, di, x, y, alpha);
        drawGlyph(scene, num, x + STEP_HAN, y + 2, alpha);
        drawGlyph(scene, zhang, x + STEP_HAN + STEP_NUM, y, alpha);
    }

    private static void drawGlyph(IntroScene scene, Spr g, double x, double y, float alpha) {
        if (g == null || g.frameCount() == 0 || g.frame(0).length == 0) {
            return;
        }
        int[] px = g.frame(0);
        int w = g.frameW(0);
        int h = g.frameH(0);
        int ox = (int) Math.round(x);
        int oy = (int) Math.round(y);
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                scene.setPixel(ox + col, oy + row, px[row * w + col], alpha);
            }
        }
    }
}
