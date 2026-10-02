package com.dioxidelite.ui.dr;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterMipmap;
import io.github.humbleui.skija.FilterMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.MipmapMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;

/**
 * 主菜单与各子页面共用的 DR 底图：暗门、喷泉波浪。
 *
 * 数值全部来自 gml_Object_DEVICE_MENU_Draw_0 开头那两段：
 *   TYPE=1&&SUBTYPE=0 → 暗门叠 5 层（43,48 / 47,48 / 43,52 / 47,52 呼吸 + 45,50 固定 0.25）
 *   BGMADE=1&&SUBTYPE=1 → 190 行波浪 + 三层剪影，BG_ALPHA 按 0.04-BG_ALPHA/14 逼近 0.5
 *
 * 视口固定 320x240 等比铺满，宽高比交给调用方裁剪。子页面和主菜单尺寸一致，
 * 所以这里把换算也一并收着，省得两边各写一份。
 */
public final class DrBackdrop {
    public static final int VIEW_W = 320;
    public static final int VIEW_H = 240;

    // skija 没有现成的 NEAREST 常量，像素图放大必须走 FilterMipmap 才不会发虚
    public static final SamplingMode NEAREST = new FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE);

    private static final float BG_MAGNITUDE = 6f;
    private static final float BG_ALPHA_MAX = 0.5f;
    private static final int WAVE_ROWS = 190;
    private static final float WAVE_H = 240f;
    // 原版每帧 +1，游戏跑 30fps，这里按真实 dt 折算回 30 步进
    private static final float FPS = 30f;

    // spr_giantdarkdoor 的 origin（ch4 data.win 实测 w=180 h=155 origin=(35,0)）
    private static final float DOOR_ORIGIN_X = 35f;
    private static final float DOOR_ORIGIN_Y = 0f;

    private long lastMs;
    private float bgSiner;
    private float animSiner;
    private float bgAlpha;
    private float lastStep;

    public void reset() {
        lastMs = 0L;
        bgSiner = 0f;
        animSiner = 0f;
        bgAlpha = 0f;
        lastStep = 0f;
    }

    /** 上一次 tick 推进的 30fps 步数，调用方按同一节奏走自己的计时器 */
    public float lastStep() {
        return lastStep;
    }

    /** 每帧推进；两个页面各自持有一个实例，互不影响 */
    public void tick() {
        long now = System.currentTimeMillis();
        if (lastMs <= 0L) lastMs = now;
        float dt = Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;
        float step = dt * FPS;
        lastStep = step;
        bgSiner += step;
        animSiner += step;
        if (fountain()) {
            bgAlpha = Math.min(BG_ALPHA_MAX, bgAlpha + (0.04f - bgAlpha / 14f) * step);
        } else {
            bgAlpha = 0f;
        }
    }

    public static boolean fountain() {
        return DrThemeState.mainUIBackgroundMode == DrThemeState.MainUIBackgroundMode.FOUNTAIN;
    }

    public static boolean door() {
        return DrThemeState.mainUIBackgroundMode == DrThemeState.MainUIBackgroundMode.DOOR;
    }

    /** 320x240 等比铺满，返回大于等于 1 的倍率 */
    public static float scale(int screenW, int screenH) {
        float fit = Math.min(screenW / (float) VIEW_W, screenH / (float) VIEW_H);
        return Math.max(1f, fit);
    }

    public static float originX(int screenW, float s) {
        return (screenW - VIEW_W * s) * 0.5f;
    }

    public static float originY(int screenH, float s) {
        return (screenH - VIEW_H * s) * 0.5f;
    }

    /**
     * 画底图。调用前 canvas 尚在屏幕坐标系，这里自己铺满屏幕并裁剪，
     * 再平移缩放到 320x240 逻辑坐标系。
     */
    public void render(Canvas canvas, int screenW, int screenH, int a) {
        float s = scale(screenW, screenH);
        float ox = originX(screenW, s);
        float oy = originY(screenH, s);
        canvas.save();
        try {
            fill(canvas, 0xFF000000, 0f, 0f, screenW, screenH);
            // 等比铺满会在某个方向溢出，裁在窗口内，别画到窗口外去
            canvas.clipRect(Rect.makeXYWH(0f, 0f, screenW, screenH));
            canvas.translate(ox, oy);
            canvas.scale(s, s);
            drawDoor(canvas, a);
            drawFountain(canvas, a);
        } finally {
            canvas.restore();
        }
    }

    /**
     * 第 4 章 TYPE=1&&SUBTYPE=0 的暗门底：spr_giantdarkdoor 第 1 帧按 2 倍放大叠 5 层。
     * <br>四张（43,48 / 47,48 / 43,52 / 47,52）alpha 呼吸，中心那张（45,50）固定 0.25 压在最上面。
     * <br>注意 GML 里 43,48 这些是 <b>origin 落点</b>，该精灵的 origin 是 (35,0) 不是中心，
     * 所以左上角要减掉 origin*scale，直接用坐标当左上角会整体右偏 70 像素。
     */
    private void drawDoor(Canvas canvas, int a) {
        if (!door() || a <= 0) return;
        Image img = DrTheme.door(1);
        if (img == null) return;
        float w = img.getWidth() * 2f;
        float h = img.getHeight() * 2f;
        float left = DOOR_ORIGIN_X * 2f;
        float top = DOOR_ORIGIN_Y * 2f;
        double breath = 0.03 + Math.sin(bgSiner / 20.0) * 0.04;
        try (Paint paint = new Paint()) {
            paint.setColor(argb(Math.round(255f * (float) breath * (a / 255f)), 0xFFFFFF));
            canvas.drawImageRect(img, Rect.makeWH(img.getWidth(), img.getHeight()),
                    Rect.makeXYWH(43f - left, 48f - top, w, h), NEAREST, paint, true);
            canvas.drawImageRect(img, Rect.makeWH(img.getWidth(), img.getHeight()),
                    Rect.makeXYWH(47f - left, 48f - top, w, h), NEAREST, paint, true);
            canvas.drawImageRect(img, Rect.makeWH(img.getWidth(), img.getHeight()),
                    Rect.makeXYWH(43f - left, 52f - top, w, h), NEAREST, paint, true);
            canvas.drawImageRect(img, Rect.makeWH(img.getWidth(), img.getHeight()),
                    Rect.makeXYWH(47f - left, 52f - top, w, h), NEAREST, paint, true);
            paint.setColor(argb(Math.round(255f * 0.25f * (a / 255f)), 0xFFFFFF));
            canvas.drawImageRect(img, Rect.makeWH(img.getWidth(), img.getHeight()),
                    Rect.makeXYWH(45f - left, 50f - top, w, h), NEAREST, paint, true);
        }
    }

    private void drawFountain(Canvas canvas, int a) {
        if (!fountain() || a <= 0) return;
        Image bg = DrTheme.menuBg();
        if (bg == null) return;
        try (Paint paint = new Paint()) {
            paint.setColor(argb(Math.round(255f * bgAlpha * 0.8f * (a / 255f)), 0xFFFFFF));
            for (int i = 0; i < WAVE_ROWS; i++) {
                float minus = BG_MAGNITUDE * (i / WAVE_H) * 1.3f;
                float mag = minus > BG_MAGNITUDE ? 0f : BG_MAGNITUDE - minus;
                float offset = (float) Math.sin((i / 8f) + (bgSiner / 30f)) * mag;
                float y = (-10f + i) - bgAlpha * 20f;
                drawRow(canvas, bg, i, offset, y, paint);
                drawRow(canvas, bg, i, -offset, y, paint);
            }
        }
        // IMAGE_MENU_ANIMATION 三层剪影，alpha 0.46 / 0.56 / 0.7
        float[] alphas = {0.46f, 0.56f, 0.7f};
        float y = (10f - bgAlpha * 20f) + WAVE_H - 70f;
        try (Paint paint = new Paint()) {
            for (int k = 0; k < 3; k++) {
                Image frame = DrTheme.wave((int) ((animSiner / 12f) + k * 0.4f));
                if (frame == null) continue;
                paint.setColor(argb(Math.round(255f * bgAlpha * alphas[k] * (a / 255f)), 0xFFFFFF));
                canvas.drawImageRect(frame, Rect.makeWH(frame.getWidth(), frame.getHeight()),
                        Rect.makeXYWH(0f, y, VIEW_W, 70f), NEAREST, paint, true);
            }
        }
    }

    private void drawRow(Canvas canvas, Image bg, int row, float x, float y, Paint paint) {
        canvas.drawImageRect(bg, Rect.makeXYWH(0f, (float) row, VIEW_W, 1f),
                Rect.makeXYWH(x, y, VIEW_W, 1f), NEAREST, paint, true);
    }

    public static void fill(Canvas canvas, int argb, float x, float y, float w, float h) {
        try (Paint paint = new Paint()) {
            paint.setColor(argb);
            canvas.drawRect(Rect.makeXYWH(x, y, w, h), paint);
        }
    }

    public static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
