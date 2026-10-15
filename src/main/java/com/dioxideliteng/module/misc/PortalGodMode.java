package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PortalGodMode).
 * Krs parity (including the original's clientbound check inside the send handler):
 * cancels ClientboundTeleportEntityPacket, which keeps the riding entity from being
 * teleported out of the portal god-mode state.
 */
public class PortalGodMode extends Feature {

   public PortalGodMode() {
      super("Portal God Mode", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         if (mc.player == null || mc.level == null) return;

         Packet<?> packet = sending.getPacket();
         if (packet instanceof ClientboundTeleportEntityPacket)
            sending.setCancelled(true);
      }
   }
}
