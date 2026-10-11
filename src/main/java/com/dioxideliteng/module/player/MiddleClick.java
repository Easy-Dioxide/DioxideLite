package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventMouseButton;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.MiddleClick) and adapted.
 *  Krs MouseClickEvent maps to EventMouseButton; MSTimer inlined; PacketUtil.sendPacket
 *  maps to mc.getConnection().send(...); the swing packet uses player.swing (26.3 path). */
public class MiddleClick extends Feature {
   private static final String PEARL = "Pearl";
   private static final String FIREWORK = "Firework";
   private static final String WIND_CHARGE = "Wind Charge";

   private final ModeSetting itemMode = new ModeSetting("Item", this, PEARL, new String[]{PEARL, FIREWORK, WIND_CHARGE});
   private final NumberSetting delay = new NumberSetting("Delay", this, 250.0, 0.0, 2000.0, 1.0);

   private long lastUseMs;

   public MiddleClick() {
      super("MiddleClick", Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.lastUseMs = 0L;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.MOUSE_BUTTON) return;
      this.setSuffix(this.itemMode.getValue());

      var mouse = (EventMouseButton) event;
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || mc.gui.screen() != null
            || !mouse.isMiddleButton() || !mouse.isPressed()
            || System.currentTimeMillis() - this.lastUseMs < this.delay.getValue())
         return;

      int slot = findItemSlot(targetItem());
      if (slot == -1) return;

      int oldSlot = player.getInventory().getSelectedSlot();
      if (slot != oldSlot) {
         player.getInventory().setSelectedSlot(slot);
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
      }

      if (gameMode.useItem(player, InteractionHand.MAIN_HAND).consumesAction())
         player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

      if (slot != oldSlot) {
         player.getInventory().setSelectedSlot(oldSlot);
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
      }

      this.lastUseMs = System.currentTimeMillis();
      mouse.setCancelled(true);
   }

   private Item targetItem() {
      return switch (this.itemMode.getValue().toLowerCase(Locale.ROOT)) {
         case "firework" -> Items.FIREWORK_ROCKET;
         case "wind charge" -> Items.WIND_CHARGE;
         default -> Items.ENDER_PEARL;
      };
   }

   private int findItemSlot(Item item) {
      var player = mc.player;
      if (player == null) return -1;
      for (int i = 0; i <= 8; i++) {
         if (player.getInventory().getItem(i).getItem() == item) return i;
      }
      return -1;
   }
}
