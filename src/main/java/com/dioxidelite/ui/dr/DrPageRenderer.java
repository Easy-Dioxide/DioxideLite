package com.dioxidelite.ui.dr;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.types.Rect;

import java.util.List;

/**
 * 三个子页面共用的 DR 列表页渲染与交互。
 *
 * 页面只负责把各自的数据翻成 {@link DrPage.Row}，滚动、命中、心形、底栏都收在这里，
 * 省得单人 / 多人 / Via 各抄一遍版式。
 *
 * 视口 320x240，列表从 BOX_Y 起往下排，底栏固定两行（190 / 210），
 * 可视区下界取两个底栏行往上一格，别让列表压到文字上。
 */
public final class DrPageRenderer {
    /** 可视区下界：底栏第一行 y 再往上留一点余量 */
    public static final float VIEW_BOTTOM = 182f;

    public interface Source {
        /** 当前要显示的条目，顺序即绘制顺序 */
        List<DrPage.Row> rows();

        /** 底栏文案，顺序即从左上到右下的落点顺序 */
        List<DrPage.Action> actions();

        /** 标题，画在顶部 TEMPCOMMENT 那一行的位置 */
        String title();

        /** 当前选中的条目下标，-1 表示没有 */
        int selected();

        /**
         * 内容版本号。数据或状态变了就换个值，渲染器据此决定要不要重建行缓存。
         * 多人页的延迟和人数是异步刷新的，靠这个自动跟上，不用手工通知。
         */
        default int version() {
            return rows().hashCode();
        }
    }

    private final DrPage page = new DrPage();
    private Source source;
    private List<DrPage.Row> rows = List.of();
    private int cachedVersion;
    private float scroll;
    private float targetScroll;
    private int hovered = -1;
    private int keyCursor = -1;

    public DrPageRenderer(Source source) {
        this.source = source;
    }

    /** Via 这类两级列表用：切层级时换一份数据源，滚动位置一并归零 */
    public void setSource(Source source) {
        this.source = source;
        invalidate();
        this.scroll = 0f;
        this.targetScroll = 0f;
    }

    public Source source() {
        return source;
    }

    /**
     * 行内容只在数据真的变了才重建。世界列表每次 mouseMoved 都 new 一遍加读存档摘要，
     * 多人那边还要算延迟文案，每帧重算太浪费，所以缓一份；
     * 版本号对不上就自动重来，多人页的 ping 异步结果也能跟上。
     */
    private void sync() {
        int version = source.version();
        if (version == cachedVersion) return;
        cachedVersion = version;
        rows = source.rows();
        clampScroll();
    }

    /** 强制重建，切层级或列表整体换掉时用 */
    public void invalidate() {
        cachedVersion = 0;
        rows = List.of();
        hovered = -1;
        sync();
    }

    public List<DrPage.Row> rows() {
        return rows;
    }

    public void reset() {
        page.reset();
        scroll = 0f;
        targetScroll = 0f;
        hovered = -1;
        keyCursor = -1;
    }

    /** 每帧推进滚动动画与底图计时 */
    public void tick() {
        page.tick();
        scroll += (targetScroll - scroll) * 0.24f;
        // 滚动是插值的，光标所在的格子会跟着列表一起往上挪，
        // 每帧按当前 scroll 重新算一次落点，心形才不会掉在原来的位置不动
        syncHeart();
    }

    /** 按光标现在停在的格子重算心形落点；光标不在列表或底栏上就不动它 */
    private void syncHeart() {
        int rowCount = rows.size();
        if (hovered >= 0 && hovered < rowCount) {
            page.heartOnRow(hovered, DrPage.BOX_Y, scroll);
            // 滚动时行会滑出可视区，心形跟着滑出去会飘到底栏上，
            // 钳在可视区里让它顶到边上停住
            page.clampHeart(DrPage.BOX_Y, VIEW_BOTTOM);
            return;
        }
        List<DrPage.Action> actions = source.actions();
        int index = keyCursor - rowCount;
        if (index >= 0 && index < actions.size()) {
            DrPage.Action target = actions.get(index);
            page.heartOnXY(target.x() - 10f, (target.line() == 0 ? DrPage.FOOT_Y1 : DrPage.FOOT_Y2) + 5f);
        }
    }

    public void render(Canvas canvas, int screenW, int screenH, int a) {
        tick();
        sync();
        page.renderBackdrop(canvas, screenW, screenH, a);
        float s = DrPage.scale(screenW, screenH);
        float ox = DrPage.originX(screenW, s);
        float oy = DrPage.originY(screenH, s);
        canvas.save();
        try {
            canvas.translate(ox, oy);
            canvas.scale(s, s);
            float titleW = DrFont.measureWidth(source.title(), DrPage.FONT);
            DrFont.drawShadowed(canvas, source.title(), (DrBackdrop.VIEW_W - titleW) * 0.5f, 30f, DrPage.FONT,
                    DrBackdrop.argb(a, DrPage.colB()));
            List<DrPage.Row> visible = rows;
            canvas.save();
            canvas.clipRect(Rect.makeXYWH(0f, DrPage.BOX_Y, DrBackdrop.VIEW_W, VIEW_BOTTOM - DrPage.BOX_Y));
            page.drawRows(canvas, visible, DrPage.BOX_Y, scroll, DrPage.BOX_Y, VIEW_BOTTOM,
                    source.selected(), hovered, a);
            canvas.restore();
            if (visible.isEmpty()) {
                String empty = DrThemeState.isChinese ? "空" : "EMPTY";
                float w = DrFont.measureWidth(empty, DrPage.FONT);
                DrFont.drawShadowed(canvas, empty, (DrBackdrop.VIEW_W - w) * 0.5f, 80f, DrPage.FONT,
                        DrBackdrop.argb(a, DrPage.colA()));
            }
            List<DrPage.Action> actions = source.actions();
            page.drawActions(canvas, actions, hoveredAction(), a);
            page.drawHeart(canvas, a);
        } finally {
            canvas.restore();
        }
    }

    /** 鼠标或键盘停在底栏上时返回那一项的下标；hovered 存的是行下标，两者别混用 */
    private int hoveredAction() {
        int rowCount = rows.size();
        if (keyCursor >= rowCount) {
            int index = keyCursor - rowCount;
            if (index < source.actions().size()) return index;
        }
        return -1;
    }

    public boolean mouseScrolled(double verticalAmount) {
        targetScroll -= (float) verticalAmount * 30f;
        clampScroll();
        return true;
    }

    /** 返回落在第几条上，-1 表示没命中条目 */
    public int mouseMoved(double screenX, double screenY, int screenW, int screenH) {
        sync();
        float s = DrPage.scale(screenW, screenH);
        float lx = (float) ((screenX - DrPage.originX(screenW, s)) / s);
        float ly = (float) ((screenY - DrPage.originY(screenH, s)) / s);
        int beforeCursor = keyCursor;
        int hit = DrPage.rowAt(lx, ly, rows.size(), DrPage.BOX_Y, scroll, DrPage.BOX_Y, VIEW_BOTTOM);
        hovered = hit;
        if (hit >= 0) {
            // 鼠标和键盘共用一套光标，免得动过鼠标之后按方向键从上一次的格子跳
            keyCursor = hit;
        } else {
            int action = DrPage.actionAt(lx, ly, source.actions());
            if (action >= 0) {
                keyCursor = rows.size() + action;
            }
        }
        syncHeart();
        // 光标换了格子才响，来回蹭同一行不该刷出一串提示音
        if (keyCursor != beforeCursor) {
            DrSound.play(DrSound.Sfx.MOVE);
        }
        return hit;
    }

    /** 命中底栏文案时返回它在本页 actions 里的下标，-1 表示没有 */
    public int actionAt(double screenX, double screenY, int screenW, int screenH) {
        float s = DrPage.scale(screenW, screenH);
        float lx = (float) ((screenX - DrPage.originX(screenW, s)) / s);
        float ly = (float) ((screenY - DrPage.originY(screenH, s)) / s);
        return DrPage.actionAt(lx, ly, source.actions());
    }

    public int hovered() {
        return hovered;
    }

    /**
     * 纯键盘操作，对应原版的 up_p/down_p/left_p/right_p + button1_p。
     * 返回选中条目变化后的下标，交由调用方决定拿到的是列表项还是底栏动作。
     *
     * @param key GLFW 键码
     */
    public int keyPressed(int key) {
        sync();
        int rowCount = rows.size();
        int actionCount = source.actions().size();
        int total = rowCount + actionCount;
        if (total == 0) return -1;
        int cursor = cursor(total);
        int before = cursor;
        switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> cursor = next(cursor, total);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> cursor = prev(cursor, total);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> cursor = next(cursor, total);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> cursor = prev(cursor, total);
            default -> {
                return -2;
            }
        }
        setCursor(cursor, rowCount);
        // 方向键真的挪了位置才响；已经停在那一格上再按不该重复出声
        if (cursor != before) {
            DrSound.play(DrSound.Sfx.MOVE);
        }
        return cursor < rowCount ? cursor : -1;
    }

    /** 回车触发；-2 表示不归本组件管，-1 表示当前停在底栏，其余是列表下标 */
    public int confirmKey(int key) {
        if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                && key != org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER
                && key != org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
            return -2;
        }
        sync();
        int rowCount = rows.size();
        int total = rowCount + source.actions().size();
        if (total == 0) return -2;
        int cursor = cursor(total);
        return cursor < rowCount ? cursor : -1;
    }

    private int cursor(int total) {
        if (keyCursor < 0 || keyCursor >= total) return 0;
        return keyCursor;
    }

    private void setCursor(int cursor, int rowCount) {
        keyCursor = cursor;
        hovered = cursor < rowCount ? cursor : -1;
        syncHeart();
    }

    private int next(int cursor, int total) {
        return (cursor + 1) % total;
    }

    private int prev(int cursor, int total) {
        return (cursor - 1 + total) % total;
    }

    public void resetCursor() {
        keyCursor = -1;
        hovered = -1;
    }

    /** 键盘光标在底栏上的下标；没停在底栏时返回 -1 */
    public int cursorActionIndex() {
        sync();
        int rowCount = rows.size();
        if (keyCursor < rowCount) return -1;
        int index = keyCursor - rowCount;
        return index >= 0 && index < source.actions().size() ? index : -1;
    }

    public void setSelectedHeart(int index) {
        page.heartOnRow(index, DrPage.BOX_Y, scroll);
    }

    private void clampScroll() {
        float content = rows.size() * DrPage.ROW_H;
        float view = VIEW_BOTTOM - DrPage.BOX_Y;
        float max = Math.max(0f, content - view);
        targetScroll = Math.max(0f, Math.min(max, targetScroll));
        scroll = Math.max(0f, Math.min(max, scroll));
    }
}
