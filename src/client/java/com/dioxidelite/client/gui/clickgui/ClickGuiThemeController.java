package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Centralized ClickGUI theme routing. Themes are presentation-only and share the same pages/config. */
public final class ClickGuiThemeController {
    private ClickGuiThemeController() {}

    public static Screen create(Config.ClickGuiTheme theme, Screen parent) {
        return switch (theme) {
            case ORIGINAL -> new NewSettingsScreen(parent);
            case MINIMAL_POP -> new DioxideLiteMinimalClickGuiScreen(parent);
            case SIGNATURE -> new DioxideLiteSignatureClickGuiScreen(parent);
        };
    }

    public static void apply(Config.ClickGuiTheme theme, Screen parent) {
        Config.clickGuiTheme = theme;
        Config.save();
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen != null) {
            Screen nextParent = parent != null ? parent : client.screen;
            client.setScreen(create(theme, nextParent));
        }
    }
}
