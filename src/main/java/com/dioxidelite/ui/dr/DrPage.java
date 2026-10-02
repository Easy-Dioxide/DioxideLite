package com.dioxidelite.ui.dr;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * 子页面（单人 / 多人 / Via）的 DR 版式：底图 + 槽位框列表 + 底部文案行 + 心形。
 *
 * 几何沿用 gml_Object_DEVICE_MENU_Draw_0 的槽位写法：
 *   BOX_X=55、BOX_Y=55、XL=210、YL=40、YS=5，行距 YL+YS=45，一列自上往下排。
 * 原版槽位固定 3 个，这里把同一条行距往下续，一屏放得下几行就放几行，
 * 超出的部分交给调用方滚动（原版没有滚动，这是为了 N 条数据必然要加的一层）。
 *
 * 每行内容按 DRAW 里 draw_text_shadow(BOX_X1+25, BOX_Y1+5, ...) 的三栏排布：
 *   第一行：名字左对齐 (25,5)，右侧尾随文本右对齐 (180,5)
 *   第二行：副标题居中宽度 180 (25,22)
 *
 * 底部文案沿用 Draw 里写死的落点，(54/135/204, 190) 与 (135/204, 210)，
 * 心形在第三行/第四行之间跳，落点照 updateHeart 那一套。
 */
public final class DrPage {
    // 与 DrMenu 同一套槽位参数
    public static final float BOX_X = 55f;
    public static final float BOX_Y = 55f;
    public static final float XL = 210f;
    public static final float YL = 40f;
    public static final float YS = 5f;
    public static final float ROW_H = YL + YS;

    public static final float FONT = 12f;
    public static final float HEART_W = 9f;

    // 第一行可用宽度：原版 draw_text_shadow 的落点是 BOX_X1+25，尾随文本右对齐到 BOX_X1+180，
    // 中间留 6 做间隔，名字的余量就是从 25 到 180 减去尾随文本再减间隔
    private static final float NAME_W = 180f - 25f;
    private static final float TRAILING_W = 70f;
    private static final float NAME_GAP = 6f;
    private static final String ELLIPSIS = "..";
    // 心形贴着框左沿会有一半压在框描边上，原版落在 65（框内 10），页面里保持同样的内缩
    private static final float HEART_X = 65f;
    // 底栏最后一栏的右边界：视口 320 宽，心形要占掉落点左边 10 再加自身 9，右边留点余量
    private static final float VIEW_RIGHT_EDGE = 300f;
    private static final float ACTION_GAP = 4f;

    // 底栏两行的 y，取自 Draw 里的 190 / 210
    public static final float FOOT_Y1 = 190f;
    public static final float FOOT_Y2 = 210f;

    /** 一行槽位的内容 */
    public record Row(String title, String trailing, String subtitle) {
    }

    /** 底部文案；x 是原版写死的落点，行号决定心形落在 190 还是 210 */
    public record Action(String text, float x, int line) {
    }

    private final DrBackdrop backdrop = new DrBackdrop();

    private float heartX = 75f;
    private float heartY = 110f;
    private float heartCurX = 75f;
    private float heartCurY = 75f;

    public void reset() {
        backdrop.reset();
        heartCurX = 75f;
        heartCurY = 75f;
    }

    public void tick() {
        backdrop.tick();
    }

    /** 底图铺满整屏并裁剪，之后坐标才是 320x240 逻辑坐标系，调用方自行 save/restore */
    public void renderBackdrop(Canvas canvas, int screenW, int screenH, int a) {
        backdrop.render(canvas, screenW, screenH, a);
    }

    public static float scale(int screenW, int screenH) {
        return DrBackdrop.scale(screenW, screenH);
    }

    public static float originX(int screenW, float s) {
        return DrBackdrop.originX(screenW, s);
    }

    public static float originY(int screenH, float s) {
        return DrBackdrop.originY(screenH, s);
    }

    /**
     * 画一列槽位框。visible 是可视区在逻辑坐标里的上下界，超出就跳过不画，
     * 但 index 计数照走，好让调用方拿到的下标仍然是完整列表里的真实下标。
     *
     * @param top    第一行框的顶端 y，主菜单是 BOX_Y
     * @param offset 列表滚动量，向上滚为正
     */
    public void drawRows(Canvas canvas, List<Row> rows, float top, float offset, float viewTop, float viewBottom,
                         int selected, int hovered, int a) {
        if (a <= 0) return;
        int colA = colA();
        int colB = colB();
        boolean cleared = DrBackdrop.fountain();
        // TYPE=1（有喷泉）描 2px 且下边压在 BOX_Y2，TYPE=0 描 1px
        float t = cleared ? 2f : 1f;
        for (int i = 0; i < rows.size(); i++) {
            float y1 = top + ROW_H * i - offset;
            if (y1 + YL < viewTop || y1 > viewBottom) continue;
            float y2 = y1 + YL - 1f;
            DrBackdrop.fill(canvas, DrBackdrop.argb(Math.round(128f * (a / 255f)), 0x000000), BOX_X, y1, XL, y2 - y1);
            // 选中行整框换成 COL_B，其余 COL_A
            int border = (hovered == i || selected == i) ? colB : colA;
            float by = y1 - t;
            float bh = (y2 - y1) + t * 2f;
            DrBackdrop.fill(canvas, DrBackdrop.argb(a, border), BOX_X - t, by, XL + t * 2f, t);
            DrBackdrop.fill(canvas, DrBackdrop.argb(a, border), BOX_X - t, cleared ? y2 : y1 + YL, XL + t * 2f, t);
            DrBackdrop.fill(canvas, DrBackdrop.argb(a, border), BOX_X - t, by, t, bh);
            DrBackdrop.fill(canvas, DrBackdrop.argb(a, border), BOX_X + XL, by, t, bh);

            Row row = rows.get(i);
            int textColor = (hovered == i || selected == i) ? colB : colA();
            // 第一行只放名字，右侧尾随文本占多宽得先量出来，名字才好在剩下的地方裁。
            // 世界名是玩家自己取的，几十个字顶出 210 宽的框是常态，两个都得按剩余宽度砍
            boolean hasTrailing = row.trailing() != null && !row.trailing().isBlank();
            String trailing = hasTrailing ? DrFont.truncate(row.trailing(), FONT, TRAILING_W, ELLIPSIS) : null;
            float trailingW = hasTrailing ? DrFont.measureWidth(trailing, FONT) : 0f;
            float titleMax = hasTrailing ? NAME_W - trailingW - NAME_GAP : NAME_W;
            DrFont.drawShadowed(canvas, DrFont.truncate(row.title(), FONT, titleMax, ELLIPSIS), BOX_X + 25f, y1 + 5f,
                    FONT, DrBackdrop.argb(a, textColor));
            if (hasTrailing) {
                DrFont.drawShadowed(canvas, trailing, BOX_X + 180f - trailingW, y1 + 5f, FONT,
                        DrBackdrop.argb(a, textColor));
            }
            if (row.subtitle() != null && !row.subtitle().isBlank()) {
                // 原版是 draw_text_shadow_width(..., 180) 居中裁切：先砍到 180 宽再居中摆
                String subtitle = DrFont.truncate(row.subtitle(), FONT, NAME_W, ELLIPSIS);
                float w = DrFont.measureWidth(subtitle, FONT);
                DrFont.drawShadowed(canvas, subtitle, BOX_X + 25f + (NAME_W - w) * 0.5f, y1 + 22f, FONT,
                        DrBackdrop.argb(a, textColor));
            }
        }
    }

    /** 底部文案行；每行按原版落点从左往右写，选中项换 COL_B 并把心形挪过去 */
    public void drawActions(Canvas canvas, List<Action> actions, int activeIndex, int a) {
        if (a <= 0) return;
        int colA = colA();
        int colB = colB();
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            DrFont.drawShadowed(canvas, action.text(), action.x(), action.line() == 0 ? FOOT_Y1 : FOOT_Y2,
                    FONT, DrBackdrop.argb(a, i == activeIndex ? colB : colA));
        }
        if (activeIndex >= 0 && activeIndex < actions.size()) {
            Action action = actions.get(activeIndex);
            heartX = action.x() - 10f;
            heartY = (action.line() == 0 ? FOOT_Y1 : FOOT_Y2) + 5f;
        }
    }

    /** 心形吸附到某个槽位行，用在列表内选中态 */
    public void heartOnRow(int index, float top, float offset) {
        if (index < 0) return;
        heartX = HEART_X;
        heartY = 72f + ROW_H * index - offset;
    }

    public void heartOnXY(float x, float y) {
        heartX = x;
        heartY = y;
    }

    /** 把心形落点夹在可视区内，滚动时别让它跑到列表外面去 */
    public void clampHeart(float viewTop, float viewBottom) {
        heartY = Math.max(viewTop, Math.min(viewBottom - HEART_W, heartY));
    }

    /** 照搬 Draw 末尾的插值吸附：差 2px 内直接贴合，否则每次追一半 */
    public void drawHeart(Canvas canvas, int a) {
        if (Math.abs(heartX - heartCurX) <= 2f) heartCurX = heartX;
        if (Math.abs(heartY - heartCurY) <= 2f) heartCurY = heartY;
        heartCurX += (heartX - heartCurX) / 2f;
        heartCurY += (heartY - heartCurY) / 2f;
        Image heart = DrTheme.heart();
        if (heart == null || a <= 0) return;
        try (Paint paint = new Paint()) {
            paint.setAlphaf(a / 255f);
            canvas.drawImageRect(heart, Rect.makeWH(HEART_W, HEART_W),
                    Rect.makeXYWH(heartCurX, heartCurY, HEART_W, HEART_W),
                    DrBackdrop.NEAREST, paint, true);
        }
    }

    /** 把窗口坐标换算成 320x240 逻辑坐标 */
    public float toLogicalX(double screenX, int screenW) {
        float s = scale(screenW, 1);
        return (float) ((screenX - originX(screenW, s)) / s);
    }

    /** 命中第几行；返回 -1 表示没落在任何一行上 */
    public static int rowAt(float lx, float ly, int count, float top, float offset, float viewTop, float viewBottom) {
        for (int i = 0; i < count; i++) {
            float y1 = top + ROW_H * i - offset;
            if (y1 + YL < viewTop || y1 > viewBottom) continue;
            if (lx >= BOX_X && lx <= BOX_X + XL && ly >= y1 && ly <= y1 + YL) return i;
        }
        return -1;
    }

    /** 命中第几个底部文案 */
    public static int actionAt(float lx, float ly, List<Action> actions) {
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            float y = action.line() == 0 ? FOOT_Y1 : FOOT_Y2;
            float w = DrFont.measureWidth(action.text(), FONT);
            if (lx >= action.x() - 4f && lx <= action.x() + w + 4f && ly >= y - 3f && ly <= y + 16f) return i;
        }
        return -1;
    }

    public static int colA() {
        if (DrBackdrop.fountain()) return DrTheme.CLEARED_ACCENT;
        if (DrBackdrop.door()) return DrTheme.UNCLEARED_ACCENT;
        return DrTheme.current().accent();
    }

    public static int colB() {
        if (DrBackdrop.fountain()) return DrTheme.CLEARED_HIGHLIGHT;
        if (DrBackdrop.door()) return DrTheme.UNCLEARED_HIGHLIGHT;
        return DrTheme.current().highlight();
    }

    /**
     * 底栏一共两行，按原版落点排：第一行 (54 / 135 / 204, 190)，
     * 第二行 (54 / 135 / 204, 210) —— Draw 里的 CHFILE 就落在 (54,210)。
     *
     * 原版的槽位文案都是两个词以内的短词，204 那栏才放得下；换成 MC 的「服务器列表」这种
     * 四字标签，204 + 宽 70 已经顶到 274，右边只剩 46 给心形和裁边。所以每一列都量一遍，
     * 超出它到下一栏之间的可用宽度就往左推，宁可贴紧也不要和邻居挤在一起。
     */
    public static List<Action> layoutActions(String... texts) {
        List<Action> actions = new ArrayList<>();
        float[] slots = {54f, 135f, 204f};
        int slot = 0;
        for (String text : texts) {
            if (slot >= slots.length * 2) break;
            float y = slot < slots.length ? 0f : 1f;
            float x = slots[slot % slots.length];
            float w = DrFont.measureWidth(text, FONT);
            // 下一栏的落点就是本栏的右边界，减 4 留个缝；最后一栏拿不到邻居，
            // 只好拿视口右沿减掉心形占的那点位置
            float limit = slot % slots.length == slots.length - 1
                    ? VIEW_RIGHT_EDGE : slots[slot % slots.length + 1] - ACTION_GAP;
            if (x + w > limit) {
                x = Math.max(BOX_X, limit - w);
            }
            actions.add(new Action(text, x, (int) y));
            slot++;
        }
        return actions;
    }
}
