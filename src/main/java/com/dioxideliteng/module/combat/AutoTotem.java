package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.util.krs.MSTimer;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.AutoTotem).
 * Notable adaptations vs the original:
 *  - Module super() constructor replaced by the client Feature(name, Category) form.
 *  - onUpdate/onWorld event handlers mapped to Events.UPDATE / Events.WORLD.
 *  - MSTimer ported to com.dioxideliteng.util.krs.MSTimer (logic identical).
 * The swap logic (offhand check, 150ms timer, totem search over the first 36
 * inventory slots, container-input SWAP against slot 40) is byte-exact Krs parity.
 */
public class AutoTotem extends Feature {
   private final MSTimer swapTimer = new MSTimer();

   public AutoTotem() {
      super("Auto Totem", Category.COMBAT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      swapTimer.reset();
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         swapTimer.reset();
         return;
      }

      if (event instanceof EventUpdate) {
         onUpdate();
      }
   }

   private void onUpdate() {
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || gameMode.getPlayerMode() == GameType.SPECTATOR)
         return;

      if (mc.gui.screen() instanceof ContainerScreen)
         return;

      if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING)
         return;

      if (!swapTimer.hasTimePassed(150L))
         return;

      int sourceSlot = findTotemInventorySlot();
      if (sourceSlot == -1)
         return;

      gameMode.handleContainerInput(player.containerMenu.containerId, sourceSlot, 40, ContainerInput.SWAP, player);
      swapTimer.reset();
   }

   private int findTotemInventorySlot() {
      var player = mc.player;
      if (player == null)
         return -1;

      for (int i = 0; i < Math.min(36, player.getInventory().getContainerSize()); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING)
            return inventoryIndexToMenuSlot(i);
      }

      return -1;
   }

   private int inventoryIndexToMenuSlot(int inventoryIndex) {
      return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
   }
}
