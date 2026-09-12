package com.dioxidelite.command;

import java.util.List;

/** DioxideLite v2 intentionally exposes no gameplay/cheat command registry. */
public final class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();
    private CommandManager() {}
    public void init() {}

    public static final String DEFAULT_PREFIX = ".";
    public static final int DEFAULT_HINT_COUNT = 5;
    private String prefix = DEFAULT_PREFIX;
    private int hintCount = DEFAULT_HINT_COUNT;

    public boolean handle(String message) { return false; }
    public String complete(String input, int cursor) { return null; }
    public String prefix() { return prefix; }
    public void setPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Command prefix must not be blank");
        }
        this.prefix = prefix;
    }
    public int hintCount() { return hintCount; }
    public void setHintCount(int hintCount) { this.hintCount = Math.max(0, hintCount); }

    /** Tokenized command input produced by the (unused in v2) suggestion pipeline. */
    public record TokenizationResult(List<String> tokens) {}
}
