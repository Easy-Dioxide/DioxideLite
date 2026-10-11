package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import java.util.Locale;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.level.AutoBypass) and adapted.
 *  Krs dependencies dropped: Client.notificationManager (no equivalent wired here),
 *  TextValue "Password" (no text setting in client -> hardcoded default), and
 *  sendUnattendedCommand(..) replaced with sendCommand(..). */
public class AutoBypass extends Feature {
   private static final String HYPIXEL = "Hypixel";
   private static final String CUBECRAFT = "Cubecraft";
   private static final String PURPLE_PRISON = "Purple Prison";
   private static final String AUTH_ME = "Auth Me";
   private static final String PASSWORD = "aaaaaaaa";

   private final ModeSetting mode = new ModeSetting("Mode", this, HYPIXEL,
         new String[]{HYPIXEL, CUBECRAFT, PURPLE_PRISON, AUTH_ME});

   private String neededCommand = null;
   private String neededClickCommand = null;

   public AutoBypass() {
      super("AutoBypass", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      this.neededCommand = null;
      this.neededClickCommand = null;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         this.setSuffix(this.mode.getValue());
         var player = mc.player;
         if (player == null) return;
         if (this.neededClickCommand != null) {
            String clickCommand = this.neededClickCommand;
            if (clickCommand.startsWith("/")) clickCommand = clickCommand.substring(1);
            player.connection.sendCommand(clickCommand);
            this.neededClickCommand = null;
         } else if (this.neededCommand != null) {
            player.connection.sendCommand(this.neededCommand);
            this.neededCommand = null;
         }
         return;
      }
      if (event instanceof EventPacketReceive recv && !recv.isCancelled()) {
         onPacket(recv);
      }
   }

   private void onPacket(EventPacketReceive recv) {
      if (this.neededCommand != null || this.neededClickCommand != null) return;
      if (mc.player == null) return;
      Packet<?> packet = recv.getPacket();
      String current = this.mode.getValue().toLowerCase(Locale.ROOT);

      if (current.equals(HYPIXEL.toLowerCase(Locale.ROOT))) {
         if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay()) {
            String msg = chatPacket.content().getString();
            if (msg.contains("You were spawned in Limbo.")
                  || msg.contains("You are AFK. Move around to return from AFK.")) {
               this.neededCommand = "lobby";
            } else if (msg.contains("You won! Want to play again?")
                  || msg.contains("You died! Want to play again?")) {
               ClickEvent.RunCommand command = findClickHereCommand(chatPacket.content());
               if (command != null) this.neededClickCommand = command.command();
            }
         }
      } else if (current.equals(CUBECRAFT.toLowerCase(Locale.ROOT))) {
         if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay()
               && chatPacket.content().getString().contains("Thank you for playing")) {
            this.neededCommand = "playagain now";
         }
      } else if (current.equals(PURPLE_PRISON.toLowerCase(Locale.ROOT))) {
         if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay()
               && chatPacket.content().getString().contains("ALERT! Your inventory is full (Use /sell)")) {
            this.neededCommand = "sell";
         }
      } else if (current.equals(AUTH_ME.toLowerCase(Locale.ROOT))) {
         if (packet instanceof ClientboundSystemChatPacket chatPacket && !chatPacket.overlay()) {
            String msg = chatPacket.content().getString();
            if (msg.contains("login")) {
               this.neededCommand = "login " + PASSWORD;
            } else if (msg.contains("register")) {
               this.neededCommand = "register " + PASSWORD + " " + PASSWORD;
            }
         }
      }
   }

   private static ClickEvent.RunCommand findClickHereCommand(Component message) {
      ClickEvent.RunCommand firstCommand = null;
      for (Component part : message.toFlatList()) {
         if (!(part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand command)) continue;
         if (part.getString().toLowerCase(Locale.ROOT).contains("click here")) return command;
         if (firstCommand == null) firstCommand = command;
      }
      return firstCommand;
   }
}
