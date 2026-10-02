package com.dioxidelite.ui.theme;

/**
 * 主题的两组附加选项：启动动画与自定义 UI。
 *
 * <p>两组互不联动，也不影响 ClickGUI/HUD；它们只决定“开机演出怎么放”和
 * “主菜单用哪套版式”。两处的章节各自独立，默认全部关闭，等于保持现状。</p>
 */
public final class ThemeAddons {

    public enum BackgroundMode {
        FOUNTAIN("fountain"),
        DOOR("door");

        private final String key;

        BackgroundMode(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static BackgroundMode fromKey(String key) {
            for (BackgroundMode mode : values()) {
                if (mode.key.equalsIgnoreCase(key)) {
                    return mode;
                }
            }
            return FOUNTAIN;
        }
    }

    /**
     * 启动动画附加项。
     *
     * @param enabled     总开关
     * @param styleId     样式 ID，见 {@link IntroStyles}，当前只有 dr
     * @param chapter     演出章节 1..5
     * @param savedFlavor 是否使用“已存档分支”
     */
    public record Intro(boolean enabled, String styleId, int chapter, boolean savedFlavor) {

        public static Intro off() {
            return new Intro(false, IntroStyles.DR, 1, true);
        }

        public Intro withEnabled(boolean value) {
            return new Intro(value, styleId, chapter, savedFlavor);
        }

        public Intro withStyle(String value) {
            return new Intro(enabled, value == null ? IntroStyles.DR : value, chapter, savedFlavor);
        }

        public Intro withChapter(int value) {
            return new Intro(enabled, styleId, clampChapter(value), savedFlavor);
        }

        public Intro withSavedFlavor(boolean value) {
            return new Intro(enabled, styleId, chapter, value);
        }
    }

    /**
     * 自定义 UI 附加项。
     *
     * @param enabled        总开关
     * @param styleId        样式 ID，见 {@link UiStyles}，dioxide = 本项目默认版式
     * @param chapter        DR 版式章节 1..5
     * @param backgroundMode DR 背景模式（喷泉 / 暗门）
     */
    public record Ui(boolean enabled, String styleId, int chapter, BackgroundMode backgroundMode) {

        public static Ui off() {
            return new Ui(false, UiStyles.DIOXIDE, 1, BackgroundMode.FOUNTAIN);
        }

        public Ui withEnabled(boolean value) {
            return new Ui(value, styleId, chapter, backgroundMode);
        }

        public Ui withStyle(String value) {
            return new Ui(enabled, value == null ? UiStyles.DIOXIDE : value, chapter, backgroundMode);
        }

        public Ui withChapter(int value) {
            return new Ui(enabled, styleId, clampChapter(value), backgroundMode);
        }

        public Ui withBackgroundMode(BackgroundMode value) {
            return new Ui(enabled, styleId, chapter, value == null ? BackgroundMode.FOUNTAIN : value);
        }
    }

    private final Intro intro;
    private final Ui ui;

    public ThemeAddons() {
        this(Intro.off(), Ui.off());
    }

    public ThemeAddons(Intro intro, Ui ui) {
        this.intro = intro == null ? Intro.off() : intro;
        this.ui = ui == null ? Ui.off() : ui;
    }

    public Intro intro() {
        return intro;
    }

    public Ui ui() {
        return ui;
    }

    static int clampChapter(int value) {
        return Math.max(1, Math.min(5, value));
    }
}
