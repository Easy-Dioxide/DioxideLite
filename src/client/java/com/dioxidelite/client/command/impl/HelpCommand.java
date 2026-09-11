package com.dioxidelite.client.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.dioxidelite.Config;
import com.dioxidelite.client.util.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class HelpCommand {
    private HelpCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
        return ClientCommandManager.literal("help")
                .executes(context -> {
                    ChatUtils.send(Config.isChinese
                            ? "可用命令：/DioxideLite update，/DioxideLite clientname <名称>"
                            : "Available commands: /DioxideLite update, /DioxideLite clientname <name>");
                    return 1;
                });
    }
}
