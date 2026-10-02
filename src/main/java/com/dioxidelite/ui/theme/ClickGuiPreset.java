package com.dioxidelite.ui.theme;

import com.dioxidelite.module.modules.ClickGui;

import java.awt.Color;

/**
 * 旧四个内置主题原本就带的 ClickGUI 预设：选择主题时顺带套用这些
 * 布局模式 / 日间模式 / 背景模糊 / accent 值，保持和旧版本一致的行为。
 */
public record ClickGuiPreset(ClickGui.Mode mode, boolean daylight, Integer blur, Integer accent) {

    public static ClickGuiPreset of(ClickGui.Mode mode, boolean daylight, Integer blur, int accentRgb) {
        return new ClickGuiPreset(mode, daylight, blur, accentRgb);
    }

    /** 把预设写进 ClickGui 模块的设置。 */
    public void apply() {
        ClickGui gui = ClickGui.INSTANCE;
        gui.mode.set(mode);
        gui.daylightMode.set(daylight);
        if (blur != null) {
            gui.popBackgroundBlur.set(blur);
        }
        if (accent != null) {
            gui.accent.set(new Color((accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF));
        }
    }
}
