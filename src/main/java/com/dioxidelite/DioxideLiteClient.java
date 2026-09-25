package com.dioxidelite;

import com.dioxidelite.config.ConfigManager;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.Listen;
import com.dioxidelite.module.ModuleManager;
import com.dioxidelite.runtime.FeatureRuntime;
import com.dioxidelite.integration.apollo.ApolloTeamNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** DioxideLite visual-only client bootstrap. */
public final class DioxideLiteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DioxideLite.LOGGER.info("Initializing {} {}...", DioxideLite.NAME, DioxideLite.VERSION);
        ModuleManager.INSTANCE.init();
        ApolloTeamNetworking.init();
        EventBus.INSTANCE.subscribe(this);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            FeatureRuntime.INSTANCE.activate();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                ConfigManager.INSTANCE.saveChecked();
            } catch (Throwable error) {
                DioxideLite.LOGGER.warn("Failed to save DioxideLite config during shutdown", error);
            } finally {
                ModuleManager.INSTANCE.disableAll();
            }
        });
        DioxideLite.LOGGER.info("{} visual runtime loaded.", DioxideLite.NAME);
    }

}
