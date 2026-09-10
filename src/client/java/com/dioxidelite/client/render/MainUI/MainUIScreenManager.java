package com.dioxidelite.client.render.MainUI;

import com.dioxidelite.Config;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class MainUIScreenManager {
    private MainUIScreenManager() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen titleScreen)) return;
            if (Config.useMainUI) {
                client.setScreen(new DioxideLiteMainUI(titleScreen));
                return;
            }
            Button button = Button.builder(Component.literal("DioxideLite · SETSUNA"), b -> {
                Config.useMainUI = true;
                Config.save();
                client.setScreen(new DioxideLiteMainUI(titleScreen, true));
            }).bounds(Math.max(6, scaledWidth - 174), 12, 162, 22).build();
            Screens.getButtons(titleScreen).add(button);
        });
    }
}
