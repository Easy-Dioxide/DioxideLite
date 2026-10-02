package com.dioxidelite.ui.dr;

/**
 * DR 主题的运行时状态。值由 {@code ThemeRuntime} 按当前主题的附加选项写入，
 * DR 版式与演出代码只读。
 *
 * <p>UI 版式与启动演出是两套独立配置：{@link #uiEnabled}/{@link #uiChapter}
 * 管主菜单；{@link #introEnabled}/{@link #introChapter} 管开机演出，互不联动。</p>
 */
public final class DrThemeState {

    public enum MainUIBackgroundMode { FOUNTAIN, DOOR }

    public static volatile boolean isChinese = true;

    /** 自定义 UI 附加项：主菜单是否走 DR 版式、第几章、背景模式。 */
    public static volatile boolean uiEnabled = false;
    public static volatile int uiChapter = 1;
    public static volatile MainUIBackgroundMode mainUIBackgroundMode = MainUIBackgroundMode.FOUNTAIN;

    /** 启动动画附加项：是否播放、第几章、已存档分支。 */
    public static volatile boolean introEnabled = false;
    public static volatile int introChapter = 1;
    public static volatile boolean drIntroSavedFlavor = true;

    private DrThemeState() {
    }
}
