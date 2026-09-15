package com.dioxidelite.ui.dioxide;

import com.dioxidelite.command.CommandManager;
import net.minecraft.client.Minecraft;

/** Lightweight presentation state shared by chat commands and Dynamic Island. */
public final class DynamicIslandBridge {
    private static final DynamicIslandBridge INSTANCE = new DynamicIslandBridge();
    private volatile String commandText = "";
    private volatile long commandUntil;
    private volatile String inputText = "";

    private DynamicIslandBridge() {}

    public static DynamicIslandBridge getInstance() { return INSTANCE; }

    public void onChatInput(String text) {
        inputText = text == null ? "" : text;
        if (!inputText.startsWith(CommandManager.INSTANCE.prefix())) {
            return;
        }
        String body = inputText.substring(CommandManager.INSTANCE.prefix().length()).trim();
        if (body.isEmpty()) {
            commandText = "COMMAND  ·  type a command";
        } else {
            String command = body.split("\\s+", 2)[0];
            commandText = switch (command.toLowerCase(java.util.Locale.ROOT)) {
                case "bind" -> "BIND  ·  module + key";
                case "config" -> "CONFIG  ·  profile / save / load";
                case "help" -> "HELP  ·  .bind  .config  .help";
                default -> "COMMAND  ·  " + command;
            };
        }
        commandUntil = System.currentTimeMillis() + 2200L;
    }

    public void onCommandSubmitted(String message, boolean handled) {
        if (message == null || !message.startsWith(CommandManager.INSTANCE.prefix())) return;
        String body = message.substring(CommandManager.INSTANCE.prefix().length()).trim();
        String command = body.isEmpty() ? "" : body.split("\\s+", 2)[0];
        commandText = handled
                ? "✓  " + command + " executed"
                : (command.isEmpty() ? "COMMAND  ·  missing command" : "•  " + command + " not registered");
        commandUntil = System.currentTimeMillis() + 2600L;
    }

    public boolean hasCommandStatus() {
        return System.currentTimeMillis() < commandUntil && !commandText.isBlank();
    }

    public String commandText() { return commandText; }

    public String inputText() { return inputText; }

    public boolean chatOpen() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen;
    }
}
