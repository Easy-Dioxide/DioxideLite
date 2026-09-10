package com.dioxidelite.client;

import com.dioxidelite.client.gui.clickgui.TermsScreen;
import com.dioxidelite.client.gui.clickgui.ClickGuiThemeController;
import com.dioxidelite.Config;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class KeyInputHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.openSettings.consumeClick()) {
                if (client.screen == null) {
                    Config.applyFirstUseLanguageDefault();
                    Minecraft.getInstance().setScreen(Config.termsRead
                        ? ClickGuiThemeController.create(Config.clickGuiTheme, null)
                        : new TermsScreen(null));
                }
            }
        });
    }
}
