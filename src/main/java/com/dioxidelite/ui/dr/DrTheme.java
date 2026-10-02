package com.dioxidelite.ui.dr;

import io.github.humbleui.skija.Image;

import java.io.InputStream;

/**
 * DELTARUNE 章节主题（样式 1-5）
 * 标题取自启动器 get_chapter_title()，图标取启动器 spr_chapterIcon 帧序
 * （get_chapter_icon_index：1→1、2→2、3→3、4→5、5→6）
 * 配色是各章 DEVICE_MENU 里 COL_A/COL_B/COL_PLUS 的原值，不是近似色：
 *   未存档（ch1 的 TYPE=0）  c_green / c_lime / merge(c_lime, c_white, .5)
 *   未通关（ch2-5 SUBTYPE=0）merge(c_ltgray, c_maroon, .2) / c_white / merge(c_yellow, c_white, .4)
 *   已通关（TYPE=1 且有喷泉）merge(c_ltgray, c_navy, .2) / c_white / merge(c_yellow, c_white, .5)
 * 后两组在 GML 里由 TYPE/SUBTYPE 决定，喷泉态由 DrMenu 在 FOUNTAIN 背景模式下取用
 */
public final class DrTheme {
    public record Chapter(String label, String title, String icon, int accent, int highlight, int selected) {
    }

    // 未通关配色：ch2-ch5 首次进入该章（SUBTYPE=0），四章逐字节相同
    public static final int UNCLEARED_ACCENT = 0xFFB39A9A;
    public static final int UNCLEARED_HIGHLIGHT = 0xFFFFFFFF;
    public static final int UNCLEARED_SELECTED = 0xFFFFFF66;

    // 已通关配色：TYPE=1 且 SUBTYPE=1，此时 BGMADE=1 才有喷泉背景
    public static final int CLEARED_ACCENT = 0xFF9A9AB3;
    public static final int CLEARED_HIGHLIGHT = 0xFFFFFFFF;
    public static final int CLEARED_SELECTED = 0xFFFFFF80;

    public static final Chapter[] CHAPTERS = {
            new Chapter("第1章", "开端", "icon_1", 0xFF008000, 0xFF00FF00, 0xFF80FF80),
            new Chapter("第2章", "赛博世界", "icon_2", UNCLEARED_ACCENT, UNCLEARED_HIGHLIGHT, UNCLEARED_SELECTED),
            new Chapter("第3章", "深夜", "icon_3", UNCLEARED_ACCENT, UNCLEARED_HIGHLIGHT, UNCLEARED_SELECTED),
            new Chapter("第4章", "预言", "icon_5", UNCLEARED_ACCENT, UNCLEARED_HIGHLIGHT, UNCLEARED_SELECTED),
            new Chapter("第5章", "庆典日", "icon_6", UNCLEARED_ACCENT, UNCLEARED_HIGHLIGHT, UNCLEARED_SELECTED),
    };

    private static Image menuBg;
    private static final Image[] waves = new Image[5];
    private static Image heart;
    private static final Image[] doors = new Image[2];

    private DrTheme() {
    }

    public static boolean active() {
        return DrThemeState.uiEnabled;
    }

    public static Chapter current() {
        int style = DrThemeState.uiChapter;
        if (style < 1 || style > CHAPTERS.length) {
            return CHAPTERS[0];
        }
        return CHAPTERS[style - 1];
    }

    // 子页面统一风格：DR 样式激活时换成章节配色，否则原样返回

    public static int pageStroke(int baseArgb) {
        if (!active()) return baseArgb;
        return (baseArgb & 0xFF000000) | (current().accent() & 0xFFFFFF);
    }

    public static int pageTitleColor(int baseArgb) {
        if (!active()) return baseArgb;
        return (baseArgb & 0xFF000000) | (current().highlight() & 0xFFFFFF);
    }

    public static int pageButtonColor(int baseRgb) {
        if (!active()) return baseRgb;
        return current().accent() & 0xFFFFFF;
    }

    public static int pageButtonHover(int baseRgb) {
        if (!active()) return baseRgb;
        return current().highlight() & 0xFFFFFF;
    }

    public static Image menuBg() {
        if (menuBg == null) {
            menuBg = loadPng("/assets/dioxide-lite/mainmenu/dr/menu_bg.png");
        }
        return menuBg;
    }

    public static Image wave(int frame) {
        int index = frame % waves.length;
        if (index < 0) index += waves.length;
        if (waves[index] == null) {
            waves[index] = loadPng("/assets/dioxide-lite/mainmenu/dr/wave_" + index + ".png");
        }
        return waves[index];
    }

    public static Image heart() {
        if (heart == null) {
            heart = loadPng("/assets/dioxide-lite/mainmenu/dr/cursor.png");
        }
        return heart;
    }

    // spr_giantdarkdoor：第 4 章 TYPE=1&&SUBTYPE=0 时叠的暗门，2 帧交替
    public static Image door(int frame) {
        int index = frame % doors.length;
        if (index < 0) index += doors.length;
        if (doors[index] == null) {
            doors[index] = loadPng("/assets/dioxide-lite/mainmenu/dr/door_" + index + ".png");
        }
        return doors[index];
    }

    private static Image loadPng(String path) {
        try (InputStream is = DrTheme.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return Image.makeFromEncoded(is.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }
}
