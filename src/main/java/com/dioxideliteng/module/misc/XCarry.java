package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.XCarry) and adapted to the client. */
public class XCarry extends Feature {
   public XCarry() {
      super("XCarry", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (!(event instanceof EventPacketSend sending)) return;
      if (mc.player == null || mc.level == null) return;
      if (sending.getPacket() instanceof ServerboundContainerClosePacket close
            && close.getContainerId() == mc.player.inventoryMenu.containerId) {
         event.setCancelled(true);
      }
   }
}
