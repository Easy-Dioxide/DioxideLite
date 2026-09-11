package com.dioxidelite.client;

import com.dioxidelite.client.gui.clickgui.ClickGuiThemeController;
import com.dioxidelite.Config;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class KeyInputHandler {
    // TEMP screenshot probe: auto-open the ClickGUI once after entering a world.
    private static boolean tempAutoOpened = false;
    private static int tempAutoOpenTicks = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!tempAutoOpened && client.player != null && client.screen == null) {
                if (++tempAutoOpenTicks > 120) {
                    tempAutoOpened = true;
                    Config.applyGameLanguageDefault();
                    client.setScreen(ClickGuiThemeController.create(Config.clickGuiTheme, null));
                }
            }
            while (KeyBindings.openSettings.consumeClick()) {
                if (client.screen == null) {
                    Config.applyGameLanguageDefault();
                    client.setScreen(ClickGuiThemeController.create(Config.clickGuiTheme, null));
                }
            }
        });
    }
}
