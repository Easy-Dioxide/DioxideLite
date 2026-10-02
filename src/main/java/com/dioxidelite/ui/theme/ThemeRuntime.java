package com.dioxidelite.ui.theme;

import com.dioxidelite.module.modules.ClickGui;
import com.dioxidelite.ui.dr.DrIntroPlayer;
import com.dioxidelite.ui.dr.DrMenuScreen;
import com.dioxidelite.ui.dr.DrThemeState;
import com.dioxidelite.ui.screen.MainMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;

/**
 * 主题的应用层：把“当前选了什么主题”同步到全局运行时。
 *
 * <p>副作用共三类：旧主题的 ClickGUI 预设、DR 的章节/开关状态、主菜单版式路由。
 * 选择主题走 {@link #select}；编辑器保存附加选项后走 {@link #applyAddons}；
 * 重播演出走 {@link #replayIntro}。</p>
 */
public final class ThemeRuntime {

    private ThemeRuntime() {
    }

    /** 客户端启动：加载主题并同步状态（演出预载由 FeatureRuntime 负责）。 */
    public static void init() {
        Themes.ensureLoaded();
        syncDrState();
    }

    /** 选择主题：应用 ClickGUI 预设 + 同步 + 路由 + 按需重播演出。 */
    public static void select(String id) {
        ThemeEntry entry = Themes.setCurrent(id);
        if (entry.clickGuiPreset() != null) {
            entry.clickGuiPreset().apply();
        }
        syncDrState();
        routeHomeScreen();
        maybeReplayIntro();
    }

    /** 编辑器改完附加选项后调用：同步 DR 状态并可能换掉主菜单。 */
    public static void applyAddons() {
        syncDrState();
        routeHomeScreen();
    }

    /** 主菜单入口屏。 */
    public static Screen homeScreen() {
        Themes.ensureLoaded();
        return DrThemeState.uiEnabled ? new DrMenuScreen() : new MainMenuScreen();
    }

    /** 打开 ClickGUI 的主题页（DR 底栏 UI_STYLE、主题模块入口）。 */
    public static void openThemeScreen(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(ClickGui.INSTANCE.createThemeScreen(parent));
        }
    }

    /** 重播当前主题的启动演出：就地播放，任何屏幕都会盖在最上层；未启用返回 false。 */
    public static boolean replayIntro() {
        syncDrState();
        if (!DrThemeState.introEnabled) {
            return false;
        }
        DrIntroPlayer.replay();
        return true;
    }

    /** 停止演出（关闭启动动画开关时调用）。 */
    public static void stopIntro() {
        DrIntroPlayer.close();
    }

    /** DR 演出是否处于播放/预热状态（加载画面接管判断）。 */
    public static boolean introActive() {
        return DrThemeState.introEnabled && DrIntroPlayer.isActive() && !DrIntroPlayer.isFinished();
    }

    private static void syncDrState() {
        ThemeAddons addons = Themes.currentEntry().addons();
        ThemeAddons.Ui ui = addons.ui();
        ThemeAddons.Intro intro = addons.intro();

        DrThemeState.uiEnabled = ui.enabled() && UiStyles.DR.equalsIgnoreCase(ui.styleId());
        DrThemeState.uiChapter = ThemeAddons.clampChapter(ui.chapter());
        DrThemeState.mainUIBackgroundMode = ui.backgroundMode() == ThemeAddons.BackgroundMode.DOOR
                ? DrThemeState.MainUIBackgroundMode.DOOR
                : DrThemeState.MainUIBackgroundMode.FOUNTAIN;

        DrThemeState.introEnabled = intro.enabled() && IntroStyles.DR.equalsIgnoreCase(intro.styleId());
        DrThemeState.introChapter = ThemeAddons.clampChapter(intro.chapter());
        DrThemeState.drIntroSavedFlavor = intro.savedFlavor();

        DrThemeState.isChinese = isChineseLanguage();
    }

    /** 需要 DR 版式的屏正在显示时，切换开关会就地换屏。 */
    private static void routeHomeScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Screen screen = mc.screen;
        if (DrThemeState.uiEnabled) {
            if (screen instanceof MainMenuScreen) {
                mc.setScreen(new DrMenuScreen());
            }
        } else if (screen instanceof DrMenuScreen) {
            mc.setScreen(new MainMenuScreen());
        }
    }

    /**
     * 切到带启动演出的主题时，在主菜单顺带重播一次，方便立即看到效果；
     * 游戏内只换配置，不打断玩家。
     */
    private static void maybeReplayIntro() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level != null || !DrThemeState.introEnabled) {
            return;
        }
        Screen screen = mc.screen;
        boolean atHome = screen == null || screen instanceof MainMenuScreen || screen instanceof DrMenuScreen;
        if (atHome && !DrIntroPlayer.isPlaying()) {
            DrIntroPlayer.replay();
        }
    }

    private static boolean isChineseLanguage() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.getLanguageManager().getSelected()
                    .toLowerCase(Locale.ROOT).startsWith("zh");
        } catch (Throwable ignored) {
            return true;
        }
    }
}
