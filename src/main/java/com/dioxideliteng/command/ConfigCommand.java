package com.dioxideliteng.command;
import com.dioxideliteng.config.ConfigManager;
import com.dioxideliteng.DioxideLiteNGClient;
import java.util.List;
import java.util.Collection;

public final class ConfigCommand extends Command {
   public ConfigCommand() { super("Config", "config <save|load|list|delete> [name]", new String[]{"config", "cfg"}); }
   @Override public void execute(String[] args, String raw) {
      try {
         if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
            DioxideLiteNGClient.sendPrefixedMessage("Configs: " + String.join(", ", ConfigManager.getConfigNames())); return;
         }
         if (args.length < 3) { DioxideLiteNGClient.sendPrefixedMessage("Usage: ." + this.syntax); return; }
         String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
         switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
            case "save" -> { ConfigManager.saveConfig(name); DioxideLiteNGClient.sendPrefixedMessage("Saved " + name); }
            case "load" -> { ConfigManager.loadConfig(name); DioxideLiteNGClient.sendPrefixedMessage("Loaded " + name); }
            case "delete" -> DioxideLiteNGClient.sendPrefixedMessage(ConfigManager.deleteConfig(name) ? "Deleted " + name : "Config not found");
            default -> DioxideLiteNGClient.sendPrefixedMessage("Usage: ." + this.syntax);
         }
      } catch (RuntimeException error) { DioxideLiteNGClient.sendPrefixedMessage("Config error: " + error.getMessage()); }
   }
   @Override public Collection<String> complete(String[] args) {
      return args.length <= 1 ? List.of("save", "load", "list", "delete") : List.of(ConfigManager.getConfigNames());
   }
}
