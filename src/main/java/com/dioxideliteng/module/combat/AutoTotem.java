package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.AutoTotem) and adapted. */
public class AutoTotem extends Feature {
   private long lastSwapMs;

   public AutoTotem() {
      super("Auto Totem", Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.lastSwapMs = 0L;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || gameMode.getPlayerMode() == GameType.SPECTATOR) return;

      if (mc.gui.screen() instanceof ContainerScreen) return;
      if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return;

      long now = System.currentTimeMillis();
      if (now - this.lastSwapMs < 150L) return;

      int sourceSlot = findTotemInventorySlot();
      if (sourceSlot == -1) return;

      gameMode.handleContainerInput(player.containerMenu.containerId, sourceSlot, 40, ContainerInput.SWAP, player);
      this.lastSwapMs = now;
   }

   private int findTotemInventorySlot() {
      var player = mc.player;
      if (player == null) return -1;
      int size = Math.min(36, player.getInventory().getContainerSize());
      for (int i = 0; i < size; i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING) return inventoryIndexToMenuSlot(i);
      }
      return -1;
   }

   private static int inventoryIndexToMenuSlot(int inventoryIndex) {
      return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
   }
}
