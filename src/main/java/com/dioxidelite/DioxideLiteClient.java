package com.dioxidelite;

import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.KeyInputEvent;
import com.dioxidelite.module.ModuleManager;
import com.dioxidelite.ui.dioxide.DioxideThemeController;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** DioxideLite visual-only client bootstrap. */
public final class DioxideLiteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DioxideLite.LOGGER.info("Initializing {} 2.0.0...", DioxideLite.NAME);
        ModuleManager.INSTANCE.init();
        EventBus.INSTANCE.subscribe(this);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.INSTANCE.disableAll());
        DioxideLite.LOGGER.info("{} visual runtime loaded.", DioxideLite.NAME);
    }

    @Listen
    private void onGlobalThemeKey(KeyInputEvent event) {
        if (event.key() != GLFW.GLFW_KEY_F6 || event.action() != GLFW.GLFW_PRESS) return;
        if (DioxideLite.mc().screen == null) {
            DioxideThemeController.cycleInGame();
        }
    }
}
