package com.dioxideliteng.util.krs;

import com.dioxideliteng.util.krs.ChatUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.nulling.PluginsDetector) — static core only.
 * The Krs class is a nulling-category Module; its event wiring (onEnable/onDisable/onUpdate/onReceivedPacket,
 * which drive the suggestion-request scan) is not ported here and remains the responsibility of the
 * future nulling-category PluginsDetector module. Everything static (shared state + parsing/AC matching)
 * is preserved 1:1 so AntiCheatDetector and the future module behave exactly like Krs.
 * Deviation: Krs's {@code ModuleRenderable.addCommandLog} (imguimc) has no client equivalent and is dropped.
 */
public final class PluginsDetector {
    public static String[] plugins = null;
    public static List<String> detectedAcs = null;
    public static boolean plChecked = false;

    private PluginsDetector() {
    }

    public static String[] collectPlugins(ClientboundCommandSuggestionsPacket packet, boolean removeNamespace) {
        LinkedHashSet<String> results = new LinkedHashSet<>();

        for (ClientboundCommandSuggestionsPacket.Entry entry : packet.suggestions()) {
            String suggestion = entry.text();
            if (removeNamespace ? !isNamespacedCommand(suggestion) : !isSimplePluginName(suggestion))
                continue;

            if (removeNamespace) {
                int separator = suggestion.indexOf(':');
                if (separator > 0)
                    suggestion = suggestion.substring(0, separator);
            }

            results.add(suggestion);
        }


        return results.toArray(String[]::new);
    }

    private static boolean isSimplePluginName(String value) {
        return isSimplePluginName(value, 0, value.length());
    }

    private static boolean isNamespacedCommand(String value) {
        int separator = value.indexOf(':');
        if (separator < 1 || separator > 32 || separator != value.lastIndexOf(':'))
            return false;

        int commandLength = value.length() - separator - 1;
        if (commandLength < 1 || commandLength > 31)
            return false;

        for (int i = 0; i < separator; i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9'))
                return false;
        }

        for (int i = separator + 1; i < value.length(); i++) {
            if (!isAsciiLetterOrDigit(value.charAt(i)))
                return false;
        }

        return true;
    }

    public static boolean isChatPluginsMessage(String content) {
        if (!content.startsWith("Plugins ("))
            return false;

        int countEnd = content.indexOf("): ");
        if (countEnd <= "Plugins (".length())
            return false;

        for (int i = "Plugins (".length(); i < countEnd; i++) {
            char c = content.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }

        int pluginStart = countEnd + 3;
        if (pluginStart >= content.length())
            return false;

        int start = pluginStart;
        while (start < content.length()) {
            int end = content.indexOf(", ", start);
            if (end < 0)
                end = content.length();

            if (!isSimplePluginName(content, start, end))
                return false;

            start = end + 2;
        }

        return true;
    }

    public static String[] parseChatPlugins(String content) {
        int start = content.indexOf("): ") + 3;
        ArrayList<String> results = new ArrayList<>();

        while (start < content.length()) {
            int end = content.indexOf(", ", start);
            if (end < 0)
                end = content.length();

            results.add(content.substring(start, end));
            start = end + 2;
        }

        return results.toArray(String[]::new);
    }

    private static boolean isSimplePluginName(String value, int start, int end) {
        int length = end - start;
        if (length < 1 || length > 32)
            return false;

        for (int i = start; i < end; i++) {
            if (!isAsciiLetterOrDigit(value.charAt(i)))
                return false;
        }
        return true;
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
    }

    public static List<String> collectAntiCheats(String[] pluginNames) {
        ArrayList<String> results = new ArrayList<>();

        for (String plugin : pluginNames) {
            if (isAntiCheat(plugin) && !results.contains(plugin))
                results.add(plugin);
        }

        return results;
    }

    public static void detectPlugins(boolean printChat) {
        if (plugins == null || plugins.length == 0) {
            if (printChat) {
                ChatUtil.printChat("Failed!");
            }
            return;
        }

        int acCount = 0;
        StringBuilder pluginList = new StringBuilder();
        for (String plugin : plugins) {
            boolean antiCheat = isAntiCheat(plugin);
            if (antiCheat)
                acCount++;

            if (!pluginList.isEmpty())
                pluginList.append(", ");
            if (antiCheat)
                pluginList.append("AC-");
            pluginList.append(plugin);
        }

        if (printChat) {
            String message = "Plugins Detected [All: " + plugins.length + ", AC: " + acCount + "] - " + pluginList;
            ChatUtil.printChat(message);
        }
    }

    public static List<String> getAntiCheats() {
        if (detectedAcs == null)
            return null;

        ArrayList<String> results = new ArrayList<>(detectedAcs.size());
        for (String antiCheat : detectedAcs) {
            if (!antiCheat.isEmpty())
                results.add(antiCheat.substring(0, 1).toUpperCase() + antiCheat.substring(1));
        }

        return results;
    }

    public static boolean isAntiCheat(String plugin) {
        return switch (plugin.toLowerCase(Locale.ROOT).replace(" ", "")) {
            case "nocheatplus", "grimac", "aac", "hawk", "intave", "horizon",
                    "vulcan", "spartan", "kauri", "anticheatreloaded", "matrix",
                    "themis", "negativity", "polar", "wave", "storm", "oxa", "light",
                    "fox", "ghost", "godseye" -> true;
            default -> false;
        };
    }
}
