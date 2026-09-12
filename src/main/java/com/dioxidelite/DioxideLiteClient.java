package com.dioxidelite;

import com.dioxidelite.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** DioxideLite visual-only client bootstrap. */
public final class DioxideLiteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DioxideLite.LOGGER.info("Initializing {} 2.0.0...", DioxideLite.NAME);
        ModuleManager.INSTANCE.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.INSTANCE.disableAll());
        DioxideLite.LOGGER.info("{} visual runtime loaded.", DioxideLite.NAME);
    }
}
