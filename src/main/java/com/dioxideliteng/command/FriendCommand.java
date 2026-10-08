package com.dioxideliteng.command;

import com.dioxideliteng.DioxideLiteNGClient;
import com.dioxideliteng.config.ConfigManager;
import com.dioxideliteng.util.Friends;

public final class FriendCommand extends Command {
   public FriendCommand() { super("Friend", "friend <add/remove/list> [player]", new String[]{"friend", "friends"}); }
   @Override public void execute(String[] args, String input) {
      if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
         DioxideLiteNGClient.sendPrefixedMessage("Friends: " + String.join(", ", Friends.names()));
      } else if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
         Friends.add(args[2]); ConfigManager.saveState();
         DioxideLiteNGClient.sendPrefixedMessage("Added friend: " + args[2]);
      } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
         Friends.remove(args[2]); ConfigManager.saveState();
         DioxideLiteNGClient.sendPrefixedMessage("Removed friend: " + args[2]);
      } else DioxideLiteNGClient.sendPrefixedMessage("Usage: ." + this.syntax);
   }
}
