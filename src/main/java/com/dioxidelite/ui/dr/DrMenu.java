package com.dioxidelite.ui.dr;

import com.dioxidelite.DioxideLite;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import org.lwjgl.glfw.GLFW;

/**
 * DELTARUNE 各章主菜单（DEVICE_MENU）的一比一复刻
 *
 * 全部数值都来自 UTMT 反编译出的 gml_Object_DEVICE_MENU_{Create,Step,Draw}_0：
 *   Create：XL=210 YL=40 YS=5、HEARTX/HEARTY=75/110、HEARTXCUR/HEARTYCUR=75/75、ONEBUFFER=2
 *   Step  ：MENU_NO 状态机、MENUCOORD[8] 的上下左右跳转、音效、输入缓冲 ONEBUFFER/TWOBUFFER
 *   Draw  ：喷泉 190 行波浪 + 三层剪影、槽位框描边、底部五处文案、心形插值吸附
 *
 * 槽位与底部文案换成了 MC 的入口，坐标、配色、心形落点、导航规则一处没改
 */
public final class DrMenu {
    public static final int VIEW_W = DrBackdrop.VIEW_W;
    public static final int VIEW_H = DrBackdrop.VIEW_H;

    // 子页面也走这套底图与工具方法，统一收在 DrBackdrop
    private static final SamplingMode NEAREST = DrBackdrop.NEAREST;

    // Create 里的 XL / YL / YS，槽位宽 210 高 40 间隔 5
    private static final float XL = 210f;
    private static final float YL = 40f;
    private static final float YS = 5f;
    private static final float BOX_X = 55f;
    private static final float BOX_Y = 55f;

    private static final float HEART_W = 9f;
    private static final float FONT = 12f;
    // 原版每帧 +1，游戏跑 30fps，这里按真实 dt 折算回 30 步进
    private static final float FPS = 30f;

    public enum Sfx { MOVE, SELECT, BACK, ERROR }

    public enum Entry {
        SINGLE, MULTI, ALT;

        String label() {
            return switch (this) {
                case SINGLE -> DrThemeState.isChinese ? "单人" : "Single";
                case MULTI -> DrThemeState.isChinese ? "多人" : "Multi";
                case ALT -> "Alt";
            };
        }

        // 对应原版槽位第二行的 PLACE[i]
        String place() {
            return switch (this) {
                case SINGLE -> DrThemeState.isChinese ? "本地世界" : "WORLDS";
                case MULTI -> DrThemeState.isChinese ? "服务器列表" : "SERVERS";
                case ALT -> DrThemeState.isChinese ? "账号管理" : "ACCOUNTS";
            };
        }
    }

    /** 底部五处落点，坐标是 Draw 里逐条写死的，MENUCOORD 从 3 起 */
    public enum Foot {
        OPTIONS(54f, 190f),
        VIA(135f, 190f),
        MODS(204f, 190f),
        UI_STYLE(135f, 210f),
        QUIT(204f, 210f);

        private final float x;
        private final float y;

        Foot(float x, float y) {
            this.x = x;
            this.y = y;
        }

        float x() {
            return x;
        }

        float y() {
            return y;
        }

        int coord() {
            return ordinal() + 3;
        }

        String text() {
            return switch (this) {
                case OPTIONS -> DrThemeState.isChinese ? "设置" : "OPTIONS";
                case VIA -> "Via";
                case MODS -> "ModMenu";
                case UI_STYLE -> DrThemeState.isChinese ? "界面设置" : "UI STYLE";
                case QUIT -> DrThemeState.isChinese ? "退出" : "QUIT";
            };
        }
    }

    public interface Host {
        void open(Entry entry);

        void options();

        void via();

        void modMenu();

        void uiSettings();

        void quit();

        void sound(Sfx sfx);
    }

    private static final Entry[] ENTRIES = Entry.values();
    private static final Foot[] FOOTS = Foot.values();

    private final Host host;
    // MENU_NO：0 列表，1 槽位确认（原版还有 2-7 的复制/消除流程，这里用不上）
    private int menuNo;
    // coord[0] 初始 -1：原版一进菜单心就落在第一格上，对应取值是 -1（还没定过），
    // 这样鼠标第一次移进任何一格都算「换格」，提示音才响得出来
    private final int[] coord = new int[8];
    private float heartX = 75f;
    private float heartY = 110f;
    private float heartCurX = 75f;
    private float heartCurY = 75f;
    // 底图与 320x240 视口换算全部收在 DrBackdrop，这里只保留状态
    private final DrBackdrop backdrop = new DrBackdrop();
    private float oneBuffer = 2f;
    private float twoBuffer;
    // TEMPCOMMENT / MESSAGETIMER：原版用来显示提示，这里给没有装对应 mod 的入口用
    private String comment = " ";
    private float commentTimer;
    private int mouseCoord = -1;

    public DrMenu(Host host) {
        this.host = host;
        // 原版 Create 里 MENUCOORD 全是 -1，进菜单第一次按方向键/移鼠标都能定位到格子
        java.util.Arrays.fill(coord, -1);
    }

    // 视口换算：320x240 等比铺满，宽高比交给 render 里的裁剪处理，不留黑边

    private static float scale(int screenW, int screenH) {
        return DrBackdrop.scale(screenW, screenH);
    }

    private static float originX(int screenW, float s) {
        return DrBackdrop.originX(screenW, s);
    }

    private static float originY(int screenH, float s) {
        return DrBackdrop.originY(screenH, s);
    }

    public void reset() {
        heartX = 75f;
        heartY = 110f;
        heartCurX = 75f;
        heartCurY = 75f;
        backdrop.reset();
        mouseCoord = -1;
        java.util.Arrays.fill(coord, -1);
        menuNo = 0;
        oneBuffer = 2f;
        twoBuffer = 0f;
        comment = " ";
        commentTimer = 0f;
    }

    /** 给没装对应 mod 的入口用：在 TEMPCOMMENT 那一行闪一句提示，同时响 snd_error */
    public void flash(String message) {
        comment = message;
        commentTimer = 90f;
        sfx(Sfx.ERROR);
    }

    // 每帧推进

    private void tick() {
        backdrop.tick();
        float step = backdrop.lastStep() * FPS;
        oneBuffer -= step;
        twoBuffer -= step;
        if (commentTimer > 0f) {
            commentTimer -= step;
            if (commentTimer <= 0f) comment = " ";
        }
    }

    private boolean fountain() {
        return DrBackdrop.fountain();
    }

    private boolean door() {
        return DrBackdrop.door();
    }

    // 绘制

    public void render(Canvas canvas, int screenW, int screenH, float alpha) {
        tick();
        int a = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
        backdrop.render(canvas, screenW, screenH, a);
        float s = scale(screenW, screenH);
        float ox = originX(screenW, s);
        float oy = originY(screenH, s);
        canvas.save();
        try {
            canvas.translate(ox, oy);
            canvas.scale(s, s);
            drawSlots(canvas, a);
            drawFooter(canvas, a);
            drawComment(canvas, a);
            drawVersion(canvas);
            drawChapterLabel(canvas, a);
            drawHeart(canvas, a);
        } finally {
            canvas.restore();
        }
    }

    /**
     * 暗门与喷泉底图已挪到 DrBackdrop，子页面和主菜单共用同一份实现。
     * 这里的取值入口保留成方法，省得所有调用点都去写 DrBackdrop.xxx。
     */

    private void drawSlots(Canvas canvas, int a) {
        if (a <= 0) return;
        int colA = colA();
        int colB = colB();
        boolean cleared = fountain();
        // TYPE=1（有喷泉）描 2px 且下边压在 BOX_Y2，TYPE=0 描 1px
        float t = cleared ? 2f : 1f;
        for (int i = 0; i < ENTRIES.length; i++) {
            float y1 = BOX_Y + (YL + YS) * i;
            float y2 = y1 + YL - 1f;
            fill(canvas, argb(Math.round(128f * (a / 255f)), 0x000000), BOX_X, y1, XL, y2 - y1);
            // 选中槽位整框换成 COL_B，其余 COL_A
            int border = coord[prevMenu()] == i ? colB : colA;
            float by = y1 - t;
            float bh = (y2 - y1) + t * 2f;
            fill(canvas, argb(a, border), BOX_X - t, by, XL + t * 2f, t);
            fill(canvas, argb(a, border), BOX_X - t, cleared ? y2 : y1 + YL, XL + t * 2f, t);
            fill(canvas, argb(a, border), BOX_X - t, by, t, bh);
            fill(canvas, argb(a, border), BOX_X + XL, by, t, bh);

            if (menuNo == 1 && coord[0] == i) {
                // 确认态：名字行留空，第二行是「进入 / 返回」
                drawText(canvas, DrThemeState.isChinese ? "进入" : "ENTER", BOX_X + 35f, y1 + 22f,
                        coord[1] == 0 ? colB : colA, a);
                drawText(canvas, DrThemeState.isChinese ? "返回" : "BACK", BOX_X + 125f, y1 + 22f,
                        coord[1] == 1 ? colB : colA, a);
                continue;
            }
            drawText(canvas, ENTRIES[i].label(), BOX_X + 25f, y1 + 5f, colA, a);
            drawText(canvas, ENTRIES[i].place(), BOX_X + 25f, y1 + 22f, colA, a);
        }
    }

    private void drawFooter(Canvas canvas, int a) {
        if (a <= 0) return;
        int colA = colA();
        int colB = colB();
        for (Foot foot : FOOTS) {
            if (!footAvailable(foot)) continue;
            // MENU_NO=1 时 coord[0] 落在槽位上，底部不亮
            int color = (menuNo == 0 && coord[0] == foot.coord()) ? colB : colA;
            drawText(canvas, foot.text(), foot.x(), foot.y(), color, a);
        }
    }

    /** 没有 ModMenu 时 MODS 入口不画也不参与导航；Via 是本项目内置的，恒可用。 */
    private static boolean footAvailable(Foot foot) {
        return foot != Foot.MODS || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu");
    }

    private void drawComment(Canvas canvas, int a) {
        if (a <= 0 || comment.isBlank()) return;
        drawText(canvas, comment, 40f, 30f, colB(), a);
    }

    private void drawVersion(Canvas canvas) {
        if (fountain()) {
            String text = "DioxideLite " + DioxideLite.VERSION + " (C) DioxideLite ";
            drawRight(canvas, text, 313f, 236f, 6f, argb(Math.round(255f * 0.4f), 0xFFFFFF));
        } else {
            DrFont.drawShadowed(canvas, DioxideLite.VERSION + " ", 248f, 230f, 6f, argb(255, colA()));
        }
    }

    private void drawChapterLabel(Canvas canvas, int a) {
        if (a <= 0) return;
        int color = fountain() ? 0xFFFFFF : colA();
        DrFont.drawShadowed(canvas, DrTheme.current().label(), 8f, 4f, FONT, argb(a, color));
    }

    private void drawHeart(Canvas canvas, int a) {
        updateHeart();
        Image heart = DrTheme.heart();
        if (heart == null || a <= 0) return;
        try (Paint paint = new Paint()) {
            paint.setAlphaf(a / 255f);
            canvas.drawImageRect(heart, Rect.makeWH(HEART_W, HEART_W),
                    Rect.makeXYWH(heartCurX, heartCurY, HEART_W, HEART_W),
                    NEAREST, paint, true);
        }
    }

    /** 照搬 Draw 末尾的落点计算与插值吸附 */
    private void updateHeart() {
        if (menuNo == 1) {
            heartX = coord[1] == 0 ? 75f : 165f;
            heartY = 81f + (YL + YS) * coord[0];
        } else {
            int c = coord[menuNo];
            if (c < 0) {
                // 还没定位过格子（进来第一帧），心形停在左上默认位，别让下标负值算出屏外坐标
                return;
            }
            if (c <= 2) {
                heartX = 65f;
                heartY = 72f + (YL + YS) * c;
            } else if (c <= 5) {
                heartX = c == 3 ? 44f : c == 4 ? 124f : 194f;
                heartY = 195f;
            } else {
                heartX = c == 6 ? 124f : 194f;
                heartY = 215f;
            }
        }
        if (Math.abs(heartX - heartCurX) <= 2f) heartCurX = heartX;
        if (Math.abs(heartY - heartCurY) <= 2f) heartCurY = heartY;
        heartCurX += (heartX - heartCurX) / 2f;
        heartCurY += (heartY - heartCurY) / 2f;
    }

    // 输入

    public boolean keyPressed(int key) {
        if (menuNo == 1) {
            if (key == GLFW.GLFW_KEY_LEFT && coord[1] == 1) {
                coord[1] = 0;
                sfx(Sfx.MOVE);
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT && coord[1] == 0) {
                coord[1] = 1;
                sfx(Sfx.MOVE);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                if (oneBuffer >= 0f) return true;
                confirm();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (twoBuffer >= 0f) return true;
                cancel();
                return true;
            }
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_DOWN -> stepDown();
            case GLFW.GLFW_KEY_UP -> stepUp();
            case GLFW.GLFW_KEY_LEFT -> stepLeft();
            case GLFW.GLFW_KEY_RIGHT -> stepRight();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (oneBuffer >= 0f) return true;
                confirm();
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                return true;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int screenW, int screenH, int button) {
        float s = scale(screenW, screenH);
        float lx = (float) ((mouseX - originX(screenW, s)) / s);
        float ly = (float) ((mouseY - originY(screenH, s)) / s);
        if (button != 0) return false;
        if (menuNo == 1) {
            int option = confirmHit(lx, ly);
            if (option < 0) return false;
            if (coord[1] != option) {
                coord[1] = option;
                sfx(Sfx.MOVE);
            }
            confirm();
            return true;
        }
        int hit = hitTest(lx, ly);
        if (hit < 0) return false;
        if (coord[0] != hit) {
            coord[0] = hit;
            sfx(Sfx.MOVE);
        }
        confirm();
        return true;
    }

    public void mouseMoved(double mouseX, double mouseY, int screenW, int screenH) {
        float s = scale(screenW, screenH);
        float lx = (float) ((mouseX - originX(screenW, s)) / s);
        float ly = (float) ((mouseY - originY(screenH, s)) / s);
        if (menuNo == 1) {
            int option = confirmHit(lx, ly);
            if (option >= 0 && coord[1] != option) {
                coord[1] = option;
                sfx(Sfx.MOVE);
            }
            return;
        }
        int hit = hitTest(lx, ly);
        if (hit == mouseCoord) return;
        mouseCoord = hit;
        if (hit >= 0 && coord[0] != hit) {
            coord[0] = hit;
            sfx(Sfx.MOVE);
        }
    }

    private void confirm() {
        if (menuNo == 1) {
            oneBuffer = 2f;
            twoBuffer = 2f;
            if (coord[1] == 0) {
                sfx(Sfx.SELECT);
                host.open(ENTRIES[coord[0]]);
            } else {
                sfx(Sfx.BACK);
                menuNo = 0;
            }
            return;
        }
        sfx(Sfx.SELECT);
        switch (coord[0]) {
            case 0, 1, 2 -> {
                coord[1] = 0;
                oneBuffer = 1f;
                twoBuffer = 1f;
                menuNo = 1;
            }
            case 3 -> host.options();
            case 4 -> host.via();
            case 5 -> host.modMenu();
            case 6 -> host.uiSettings();
            default -> host.quit();
        }
        if (menuNo == 0) {
            oneBuffer = 2f;
            twoBuffer = 2f;
        }
    }

    private void cancel() {
        oneBuffer = 1f;
        twoBuffer = 1f;
        sfx(Sfx.BACK);
        menuNo = 0;
    }

    // MENU_NO=0 的八向导航，逐条对应 Step_0 的 MENUCOORD[0] 分支

    private void stepDown() {
        int c = coord[0];
        if (c < 3) c += 1;
        else if (c == 3 || c == 4) c = 6;
        else if (c == 5) c = 7;
        moveTo(c);
    }

    private void stepUp() {
        int c = coord[0];
        if (c <= 0) return;
        if (c < 3) c -= 1;
        else if (c <= 5) c = 2;
        else if (c == 6) c = 4;
        else c = footAvailable(Foot.MODS) ? 5 : 4;
        moveTo(c);
    }

    private void stepRight() {
        int c = coord[0];
        if (c >= 3 && c <= 5) {
            int n = c;
            do {
                n = n >= 5 ? 3 : n + 1;
            } while (!footAvailable(FOOTS[n - 3]));
            c = n;
        } else if (c == 6) c = 7;
        else if (c == 7) c = 6;
        else return;
        moveTo(c);
    }

    private void stepLeft() {
        int c = coord[0];
        if (c >= 3 && c <= 5) {
            int n = c;
            do {
                n = n <= 3 ? 5 : n - 1;
            } while (!footAvailable(FOOTS[n - 3]));
            c = n;
        } else if (c == 6) c = 7;
        else if (c == 7) c = 6;
        else return;
        moveTo(c);
    }

    private void moveTo(int next) {
        if (coord[0] == next) return;
        coord[0] = next;
        mouseCoord = next;
        sfx(Sfx.MOVE);
    }

    private int hitTest(float lx, float ly) {
        for (int i = 0; i < ENTRIES.length; i++) {
            float y1 = BOX_Y + (YL + YS) * i;
            if (lx >= BOX_X && lx <= BOX_X + XL && ly >= y1 && ly <= y1 + YL) return i;
        }
        for (Foot foot : FOOTS) {
            if (!footAvailable(foot)) continue;
            float w = DrFont.measureWidth(foot.text(), FONT);
            if (lx >= foot.x() - 4f && lx <= foot.x() + w + 4f && ly >= foot.y() - 3f && ly <= foot.y() + 16f) {
                return foot.coord();
            }
        }
        return -1;
    }

    private int confirmHit(float lx, float ly) {
        float y1 = BOX_Y + (YL + YS) * coord[0];
        if (ly < y1 + 14f || ly > y1 + 34f) return -1;
        if (lx >= 70f && lx <= 162f) return 0;
        if (lx >= 163f && lx <= 255f) return 1;
        return -1;
    }

    private void sfx(Sfx sfx) {
        host.sound(sfx);
    }

    // 小工具

    private int prevMenu() {
        return menuNo == 1 ? 0 : menuNo;
    }

    private int colA() {
        DrTheme.Chapter chapter = DrTheme.current();
        // 喷泉对应 TYPE=1&&SUBTYPE=1，暗门对应 TYPE=1&&SUBTYPE=0，两者 COL_A 不同
        if (fountain()) return DrTheme.CLEARED_ACCENT;
        if (door()) return DrTheme.UNCLEARED_ACCENT;
        return chapter.accent();
    }

    private int colB() {
        DrTheme.Chapter chapter = DrTheme.current();
        if (fountain()) return DrTheme.CLEARED_HIGHLIGHT;
        if (door()) return DrTheme.UNCLEARED_HIGHLIGHT;
        return chapter.highlight();
    }

    private void drawText(Canvas canvas, String text, float x, float y, int color, int a) {
        DrFont.drawShadowed(canvas, text, x, y, FONT, argb(a, color));
    }

    private void drawRight(Canvas canvas, String text, float rightX, float y, float size, int argb) {
        DrFont.drawShadowed(canvas, text, rightX - DrFont.measureWidth(text, size), y, size, argb);
    }

    private static void fill(Canvas canvas, int argb, float x, float y, float w, float h) {
        DrBackdrop.fill(canvas, argb, x, y, w, h);
    }

    private static int argb(int alpha, int rgb) {
        return DrBackdrop.argb(alpha, rgb);
    }
}
