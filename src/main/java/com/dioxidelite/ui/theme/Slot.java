package com.dioxidelite.ui.theme;

import java.util.ArrayList;
import java.util.List;

/**
 * 主题调色板的槽位。
 *
 * <p>基础 12 槽沿用参考客户端的数据结构，扩展 18 槽覆盖 DioxideLite 自己的取色点
 * （ClickGUI 的 GuiPalette 层级、通用屏幕/HUD、灵动岛、通知）。槽位是“可空”语义：
 * 未显式设置时回退到各消费点原本的取色逻辑，显式设置后覆盖所有映射到它的取色点。</p>
 */
public enum Slot {

    // ---- 窗口 / 面板描边 ----
    WINDOW_BG("windowBg", Group.WINDOW),
    WINDOW_HEADER("windowHeader", Group.WINDOW),
    WINDOW_STROKE("windowStroke", Group.WINDOW),
    OUTLINE("outline", Group.WINDOW),
    OUTLINE_VARIANT("outlineVariant", Group.WINDOW),
    STROKE_SOFT("strokeSoft", Group.WINDOW),

    // ---- 表面层级 ----
    BACKDROP("backdrop", Group.SURFACE),
    SURFACE("surface", Group.SURFACE),
    SURFACE_HOVER("surfaceHover", Group.SURFACE),
    PANEL("panel", Group.SURFACE),
    PANEL_INNER("panelInner", Group.SURFACE),
    SECTION("section", Group.SURFACE),
    HEADER("header", Group.SURFACE),

    // ---- 卡片与控件 ----
    CARD_ENABLED("cardEnabled", Group.CARD),
    CARD_DISABLED("cardDisabled", Group.CARD),
    TRACK("track", Group.CARD),
    SCROLL("scroll", Group.CARD),

    // ---- 文字 ----
    TEXT_PRIMARY("textPrimary", Group.TEXT),
    TEXT_MUTED("textMuted", Group.TEXT),
    TEXT_FAINT("textFaint", Group.TEXT),

    // ---- 强调与状态 ----
    ACCENT("accent", Group.ACCENT),
    ACCENT_SOFT("accentSoft", Group.ACCENT),
    SUCCESS("success", Group.ACCENT),
    WARNING("warning", Group.ACCENT),
    DANGER("danger", Group.ACCENT),
    INFO("info", Group.ACCENT),

    // ---- 模块专属 ----
    ISLAND_BODY("islandBody", Group.MODULE),
    ISLAND_EDGE("islandEdge", Group.MODULE),
    ISLAND_GLOW("islandGlow", Group.MODULE),
    NOTIFY_ACCENT("notifyAccent", Group.MODULE);

    /** 编辑器里的分组，顺便决定 JSON 之外的展示顺序。 */
    public enum Group {
        WINDOW("Window"),
        SURFACE("Surfaces"),
        CARD("Cards & Controls"),
        TEXT("Text"),
        ACCENT("Accent & Status"),
        MODULE("Modules");

        private final String title;

        Group(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final String key;
    private final Group group;

    Slot(String key, Group group) {
        this.key = key;
        this.group = group;
    }

    /** JSON 与展示用的稳定键名。 */
    public String key() {
        return key;
    }

    public Group group() {
        return group;
    }

    /** 按组返回槽位，编辑器渲染用。 */
    public static Slot[] of(Group group) {
        List<Slot> out = new ArrayList<>();
        for (Slot slot : values()) {
            if (slot.group == group) {
                out.add(slot);
            }
        }
        return out.toArray(new Slot[0]);
    }
}
