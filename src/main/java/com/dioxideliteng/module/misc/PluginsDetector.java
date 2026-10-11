package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.nulling.PluginsDetector) and adapted to the client.
 * TickTimer is inlined, PacketUtil.sendPacket -> mc.getConnection().send, ChatUtil.printChat -> sendSystemMessage.
 * The imguimc ModuleRenderable hook and ModuleManager.plChecked flag were dropped; results are printed on
 * disable instead. The static plugins / detectedAcs fields and getAntiCheats() are retained for HUD consumers.
 */
public class PluginsDetector extends Feature {
   private int timer;
   private int step;
   public static String[] plugins = null;
   public static List<String> detectedAcs = null;

   public PluginsDetector() {
      super("PluginsDetector", Category.MISC);
   }

   @Override
   public void onEnable() {
      if (mc.player == null || mc.level == null) return;
      plugins = null;
      detectedAcs = null;
      this.step = 0;
      this.timer = 0;
   }

   @Override
   public void onDisable() {
      if (mc.player == null || mc.level == null) return;
      detectPlugins(true);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         this.timer++;
         if (this.timer >= 20) {
            this.timer = 0;
            this.step++;
         }
         if (this.timer != 1) return;
         if (mc.player == null || mc.level == null) return;

         switch (this.step) {
            case 0 -> mc.getConnection().send(new ServerboundCommandSuggestionPacket(0, "/version "));
            case 1 -> mc.getConnection().send(new ServerboundCommandSuggestionPacket(0, "/bukkit:version "));
            case 2, 3 -> {
               this.timer = 0;
               this.step++;
            }
            case 4 -> mc.getConnection().send(new ServerboundCommandSuggestionPacket(0, "/"));
            default -> setEnabled(false);
         }
         return;
      }

      if (!(event instanceof EventPacketReceive receiving)) return;
      if (mc.player == null || mc.level == null) return;

      var packet = receiving.getPacket();
      if (packet instanceof ClientboundCommandSuggestionsPacket cmdPacket) {
         if (cmdPacket.suggestions().isEmpty()) return;

         if (this.step == 0 || this.step == 1) {
            plugins = collectPlugins(cmdPacket, false);
         } else if (this.step == 4) {
            plugins = collectPlugins(cmdPacket, true);
         }

         if (plugins != null && plugins.length > 0) {
            List<String> antiCheatResults = collectAntiCheats(plugins);
            detectedAcs = antiCheatResults.isEmpty() ? null : antiCheatResults;
            setEnabled(false);
         }
         receiving.setCancelled(true);
      } else if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
         String content = chatPacket.body().content();
         if (isChatPluginsMessage(content)) {
            plugins = parseChatPlugins(content);
            setEnabled(false);
            receiving.setCancelled(true);
         }
      }
   }

   private static String[] collectPlugins(ClientboundCommandSuggestionsPacket packet, boolean removeNamespace) {
      LinkedHashSet<String> results = new LinkedHashSet<>();

      for (ClientboundCommandSuggestionsPacket.Entry entry : packet.suggestions()) {
         String suggestion = entry.text();
         if (removeNamespace ? !isNamespacedCommand(suggestion) : !isSimplePluginName(suggestion)) continue;

         if (removeNamespace) {
            int separator = suggestion.indexOf(':');
            if (separator > 0) suggestion = suggestion.substring(0, separator);
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
      if (separator < 1 || separator > 32 || separator != value.lastIndexOf(':')) return false;

      int commandLength = value.length() - separator - 1;
      if (commandLength < 1 || commandLength > 31) return false;

      for (int i = 0; i < separator; i++) {
         char c = value.charAt(i);
         if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) return false;
      }

      for (int i = separator + 1; i < value.length(); i++) {
         if (!isAsciiLetterOrDigit(value.charAt(i))) return false;
      }

      return true;
   }

   private static boolean isChatPluginsMessage(String content) {
      if (!content.startsWith("Plugins (")) return false;

      int countEnd = content.indexOf("): ");
      if (countEnd <= "Plugins (".length()) return false;

      for (int i = "Plugins (".length(); i < countEnd; i++) {
         char c = content.charAt(i);
         if (c < '0' || c > '9') return false;
      }

      int pluginStart = countEnd + 3;
      if (pluginStart >= content.length()) return false;

      int start = pluginStart;
      while (start < content.length()) {
         int end = content.indexOf(", ", start);
         if (end < 0) end = content.length();

         if (!isSimplePluginName(content, start, end)) return false;

         start = end + 2;
      }

      return true;
   }

   private static String[] parseChatPlugins(String content) {
      int start = content.indexOf("): ") + 3;
      ArrayList<String> results = new ArrayList<>();

      while (start < content.length()) {
         int end = content.indexOf(", ", start);
         if (end < 0) end = content.length();

         results.add(content.substring(start, end));
         start = end + 2;
      }

      return results.toArray(String[]::new);
   }

   private static boolean isSimplePluginName(String value, int start, int end) {
      int length = end - start;
      if (length < 1 || length > 32) return false;

      for (int i = start; i < end; i++) {
         if (!isAsciiLetterOrDigit(value.charAt(i))) return false;
      }
      return true;
   }

   private static boolean isAsciiLetterOrDigit(char c) {
      return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
   }

   private static List<String> collectAntiCheats(String[] pluginNames) {
      ArrayList<String> results = new ArrayList<>();

      for (String plugin : pluginNames) {
         if (isAntiCheat(plugin) && !results.contains(plugin)) results.add(plugin);
      }

      return results;
   }

   public static void detectPlugins(boolean printChat) {
      if (plugins == null || plugins.length == 0) {
         if (printChat && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("[PluginsDetector] Failed!"));
         }
         return;
      }

      int acCount = 0;
      StringBuilder pluginList = new StringBuilder();
      for (String plugin : plugins) {
         boolean antiCheat = isAntiCheat(plugin);
         if (antiCheat) acCount++;

         if (!pluginList.isEmpty()) pluginList.append(", ");
         if (antiCheat) pluginList.append("AC-");
         pluginList.append(plugin);
      }

      if (printChat && mc.player != null) {
         String message = "Plugins Detected [All: " + plugins.length + ", AC: " + acCount + "] - " + pluginList;
         mc.player.sendSystemMessage(Component.literal("[PluginsDetector] " + message));
      }
   }

   public static List<String> getAntiCheats() {
      if (detectedAcs == null) return null;

      ArrayList<String> results = new ArrayList<>(detectedAcs.size());
      for (String antiCheat : detectedAcs) {
         if (!antiCheat.isEmpty()) results.add(antiCheat.substring(0, 1).toUpperCase() + antiCheat.substring(1));
      }

      return results;
   }

   private static boolean isAntiCheat(String plugin) {
      return switch (plugin.toLowerCase(Locale.ROOT).replace(" ", "")) {
         case "nocheatplus", "grimac", "aac", "hawk", "intave", "horizon",
               "vulcan", "spartan", "kauri", "anticheatreloaded", "matrix",
               "themis", "negativity", "polar", "wave", "storm", "oxa", "light",
               "fox", "ghost", "godseye" -> true;
         default -> false;
      };
   }
}
