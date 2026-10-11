package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PortalGodMode) and adapted to the client.
 * NOTE: the Krs source cancels {@link ClientboundTeleportEntityPacket} on the SEND path; since the client never
 * sends clientbound teleport packets this is effectively inert. Ported faithfully per the adaptation rules.
 */
public class PortalGodMode extends Feature {
   public PortalGodMode() {
      super("PortalGodMode", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (!(event instanceof EventPacketSend sending)) return;
      if (mc.player == null || mc.level == null) return;
      if (sending.getPacket() instanceof ClientboundTeleportEntityPacket) {
         event.setCancelled(true);
      }
   }
}
