package com.dioxidelite.module.modules;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.ButtonSetting;
import com.dioxidelite.setting.settings.ThemeSelectSetting;
import com.dioxidelite.ui.theme.Themes;
import com.dioxidelite.ui.theme.ThemeRuntime;

/**
 * 主题选择器：点击循环切换内置/自定义主题；附加选项（启动动画、自定义 UI）
 * 已经并入主题本身，用主题编辑器或 ClickGUI 的 THEMES 分类调整。
 */
public final class ThemeModule extends Module {

    public static final ThemeModule INSTANCE = new ThemeModule();

    public final ThemeSelectSetting theme = add(new ThemeSelectSetting("Theme")
            .onChange(ThemeRuntime::select));

    public final ButtonSetting replayIntro = add(new ButtonSetting("Replay Intro", ThemeRuntime::replayIntro)
            .visibleWhen(() -> Themes.currentEntry().addons().intro().enabled()));

    private ThemeModule() {
        super("Theme", Category.CLIENT);
        setToggleable(false);
    }
}
