package com.dioxidelite;

import com.dioxidelite.client.KeyBindings;
import com.dioxidelite.client.KeyInputHandler;
import com.dioxidelite.client.Update;
import com.dioxidelite.client.TermsManager;
import com.dioxidelite.client.VersionWarningManager;
import com.dioxidelite.client.command.CommandManager;
import com.dioxidelite.client.render.MainUI.MainUIBackgrounds;
import com.dioxidelite.client.render.MainUI.MainUIScreenManager;
import com.dioxidelite.client.modules.impl.Combat.ElytraAssistManager;
import com.dioxidelite.client.modules.impl.Misc.VictorySound;
import com.dioxidelite.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import com.dioxidelite.client.modules.impl.Render.CustomCapeManager;
import com.dioxidelite.client.modules.impl.Render.NotificationOverlay;
import com.dioxidelite.client.modules.impl.Tool.AutoChestDepositManager;
import com.dioxidelite.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.dioxidelite.client.modules.impl.Tool.FakePlayerManager;
import com.dioxidelite.client.modules.impl.Tool.FishingRodAssistManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class DioxideLiteClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        VictorySound.init();
        MainUIBackgrounds.init();
        TermsManager.ensure();
        CustomCapeManager.init();
        KeyBindings.register();
        KeyInputHandler.register();
        MainUIScreenManager.init();
        CommandManager.register();
        Update.startAutoCheck();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AutoChestDepositManager.tick(client);
            ElytraAssistManager.tick(client);
            InputMethodFix.tick(client);
            FishingRodAssistManager.tick(client);
            BlockCountDisplayRenderer.getInstance().tick(client);
            FakePlayerManager.tick(client);
            VersionWarningManager.tick(client);
            Update.tick(client);
        });

    }
}
