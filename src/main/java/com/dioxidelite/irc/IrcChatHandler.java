package com.dioxidelite.irc;

import com.dioxidelite.module.modules.player.IrcModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Minimal {@code /irc} chat-command handling, wired into the chat-screen send
 * path (see ChatScreenMixin) so no Fabric command API module is required.
 */
public final class IrcChatHandler {

    private IrcChatHandler() {
    }

    /**
     * Handles {@code /irc <sub>} messages typed in the chat box.
     *
     * @return true when the message was consumed (not sent to the server)
     */
    public static boolean handle(String message) {
        if (message == null || !message.startsWith("/irc")) {
            return false;
        }
        String[] parts = message.trim().split("\\s+", 3);
        String sub = parts.length > 1 ? parts[1].toLowerCase(java.util.Locale.ROOT) : "";
        switch (sub) {
            case "connect" -> {
                IrcModule module = IrcModule.INSTANCE;
                if (!module.isEnabled()) {
                    feedback("§7[IRC] 模块未启用（ClickGUI → Player → IRC）");
                } else {
                    feedback("§e[IRC] 正在连接 " + module.host.get() + ":" + module.port.get() + " ...");
                    module.connectIrc();
                }
            }
            case "disconnect" -> {
                IrcModule module = IrcModule.INSTANCE;
                if (!module.isEnabled()) {
                    feedback("§7[IRC] 模块未启用");
                } else {
                    module.disconnectIrc();
                    feedback("§7[IRC] 已断开");
                }
            }
            case "status" -> {
                IrcModule module = IrcModule.INSTANCE;
                if (!module.isEnabled()) {
                    feedback("§7[IRC] 模块未启用");
                } else if (module.isConnected()) {
                    feedback("§a[IRC] 已连接 · " + IrcModule.ircOnlineUsers().size() + " 人在线");
                } else {
                    feedback("§c[IRC] 未连接（" + module.host.get() + ":" + module.port.get() + "）");
                }
            }
            case "send" -> {
                String content = parts.length > 2 ? parts[2] : "";
                IrcModule module = IrcModule.INSTANCE;
                if (!module.isEnabled()) {
                    feedback("§c[IRC] 模块未启用");
                } else if (content.isBlank()) {
                    feedback("§7[IRC] 用法: /irc send <消息>");
                } else if (module.client() != null) {
                    module.client().sendMessage(content);
                } else {
                    feedback("§c[IRC] 客户端未初始化");
                }
            }
            default -> feedback("§e/irc connect · disconnect · status · send <消息>");
        }
        return true;
    }

    private static void feedback(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) {
            return;
        }
        client.execute(() -> client.gui.getChat().addClientSystemMessage(Component.literal(message)));
    }
}
