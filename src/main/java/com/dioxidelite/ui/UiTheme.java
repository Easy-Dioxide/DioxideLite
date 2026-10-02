package com.dioxidelite.ui;

import com.dioxidelite.ui.theme.Slot;
import com.dioxidelite.ui.theme.Themes;

/**
 * Shared visual tokens for all DioxideLite screens and HUD surfaces.
 *
 * <p>颜色一律走动态访问器：先查当前主题的显式槽，未设置时回退到这里的出厂默认值，
 * 所以 CLASSIC 主题下的观感与旧版本一致。纯几何 token（圆角）保持静态常量。</p>
 */
public final class UiTheme {

    // ------------------------------------------------------------------
    // 出厂默认值（主题未覆盖时的回退）
    // ------------------------------------------------------------------

    private static final int DEFAULT_BACKDROP = argb(170, 3, 6, 7);
    private static final int DEFAULT_SURFACE = argb(246, 12, 16, 18);
    private static final int DEFAULT_SURFACE_ALT = argb(238, 17, 22, 24);
    private static final int DEFAULT_SURFACE_RAISED = argb(246, 25, 32, 34);
    private static final int DEFAULT_SURFACE_HOVER = argb(250, 31, 40, 42);
    private static final int DEFAULT_CONTROL = argb(244, 21, 27, 29);
    private static final int DEFAULT_CONTROL_HOVER = argb(250, 34, 43, 45);
    private static final int DEFAULT_HEADER = argb(248, 14, 19, 21);
    private static final int DEFAULT_BORDER = rgb(53, 65, 67);
    private static final int DEFAULT_BORDER_STRONG = rgb(73, 88, 90);
    private static final int DEFAULT_BORDER_SOFT = argb(138, 56, 68, 70);
    private static final int DEFAULT_SHADOW = argb(105, 0, 0, 0);

    private static final int DEFAULT_TEXT = rgb(241, 246, 244);
    private static final int DEFAULT_TEXT_MUTED = rgb(166, 178, 174);
    private static final int DEFAULT_TEXT_FAINT = rgb(103, 117, 113);

    private static final int DEFAULT_ACCENT_DARK = rgb(22, 112, 92);
    private static final int DEFAULT_ACCENT_SOFT = argb(46, 62, 214, 180);
    private static final int DEFAULT_SUCCESS = rgb(84, 211, 143);
    private static final int DEFAULT_WARNING = rgb(244, 183, 86);
    private static final int DEFAULT_DANGER = rgb(238, 100, 96);
    private static final int DEFAULT_INFO = rgb(91, 174, 255);

    public static final float RADIUS = 6.0F;
    public static final float RADIUS_SMALL = 4.0F;

    private UiTheme() {
    }

    // ------------------------------------------------------------------
    // 动态颜色访问器
    // ------------------------------------------------------------------

    public static int backdrop() {
        return Themes.resolve(Slot.BACKDROP, DEFAULT_BACKDROP);
    }

    public static int surface() {
        return Themes.resolve(Slot.SURFACE, DEFAULT_SURFACE);
    }

    public static int surfaceAlt() {
        return Themes.resolve(Slot.SURFACE, DEFAULT_SURFACE_ALT);
    }

    public static int surfaceRaised() {
        return DEFAULT_SURFACE_RAISED;
    }

    public static int surfaceHover() {
        return Themes.resolve(Slot.SURFACE_HOVER, DEFAULT_SURFACE_HOVER);
    }

    public static int control() {
        return DEFAULT_CONTROL;
    }

    public static int controlHover() {
        return DEFAULT_CONTROL_HOVER;
    }

    public static int header() {
        return Themes.resolve(Slot.WINDOW_HEADER, DEFAULT_HEADER);
    }

    public static int border() {
        return Themes.resolve(Slot.OUTLINE, DEFAULT_BORDER);
    }

    public static int borderStrong() {
        return DEFAULT_BORDER_STRONG;
    }

    public static int borderSoft() {
        return Themes.resolve(Slot.STROKE_SOFT, DEFAULT_BORDER_SOFT);
    }

    public static int shadow() {
        return DEFAULT_SHADOW;
    }

    public static int text() {
        return Themes.resolve(Slot.TEXT_PRIMARY, DEFAULT_TEXT);
    }

    public static int textMuted() {
        return Themes.resolve(Slot.TEXT_MUTED, DEFAULT_TEXT_MUTED);
    }

    public static int textFaint() {
        return Themes.resolve(Slot.TEXT_FAINT, DEFAULT_TEXT_FAINT);
    }

    /** Crosshair 等模块把自己当作固定色板用；这里给默认强调色，动态 accent 走 {@link #accent()}。 */
    public static int accentDefault() {
        return rgb(62, 214, 180);
    }

    public static int accentDark() {
        return DEFAULT_ACCENT_DARK;
    }

    public static int accentSoft() {
        return Themes.resolve(Slot.ACCENT_SOFT, DEFAULT_ACCENT_SOFT);
    }

    public static int success() {
        return Themes.resolve(Slot.SUCCESS, DEFAULT_SUCCESS);
    }

    public static int warning() {
        return Themes.resolve(Slot.WARNING, DEFAULT_WARNING);
    }

    public static int danger() {
        return Themes.resolve(Slot.DANGER, DEFAULT_DANGER);
    }

    public static int info() {
        return Themes.resolve(Slot.INFO, DEFAULT_INFO);
    }

    /** 当前主题的强调色（CLASSIC 回退 ClickGui.accent）。 */
    public static int accent() {
        return Themes.accent();
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    public static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    public static int rgb(int red, int green, int blue) {
        return argb(255, red, green, blue);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
