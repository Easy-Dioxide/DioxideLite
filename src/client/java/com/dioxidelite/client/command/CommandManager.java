package com.dioxidelite.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.dioxidelite.client.command.impl.ClientNameCommand;
import com.dioxidelite.client.command.impl.HelpCommand;
import com.dioxidelite.client.command.impl.UpdateCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class CommandManager {
    private CommandManager() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("DioxideLite")
                    .then(HelpCommand.build())
                    .then(UpdateCommand.build())
                    .then(ClientNameCommand.build());
            dispatcher.register(root);
        });
    }
}
