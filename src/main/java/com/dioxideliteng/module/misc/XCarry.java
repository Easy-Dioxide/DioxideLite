package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.XCarry).
 * Krs parity: cancels the outgoing container-close packet for the player's own
 * inventory menu, so the server keeps the inventory "open" (item carry in the
 * crafting slots survives).
 */
public class XCarry extends Feature {

   public XCarry() {
      super("XCarry", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         if (mc.player == null || mc.level == null) return;

         Packet<?> packet = sending.getPacket();
         if (packet instanceof ServerboundContainerClosePacket closePacket
               && closePacket.getContainerId() == mc.player.inventoryMenu.containerId)
            sending.setCancelled(true);
      }
   }
}
