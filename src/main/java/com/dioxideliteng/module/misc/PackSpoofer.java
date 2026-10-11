package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PackSpoofer) and adapted to the client.
 * Krs sends the spoofed responses with sendPacketAsSilent; the client re-dispatches send events synchronously,
 * so a bypass flag is used to stop this module from cancelling its own forged resource-pack packets.
 */
public class PackSpoofer extends Feature {
   private boolean bypass;

   public PackSpoofer() {
      super("PackSpoofer", Category.MISC);
   }

   @Override
   public void onDisable() {
      this.bypass = false;
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketSend sending && !this.bypass) {
         if (mc.player == null || mc.level == null) return;
         if (sending.getPacket() instanceof ServerboundResourcePackPacket) {
            event.setCancelled(true);
         }
      } else if (event instanceof EventPacketReceive receiving) {
         if (mc.player == null || mc.level == null) return;
         if (receiving.getPacket() instanceof ClientboundResourcePackPushPacket push) {
            receiving.setCancelled(true);
            if (mc.getConnection() == null) return;
            this.bypass = true;
            try {
               mc.getConnection().send(new ServerboundResourcePackPacket(push.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
               mc.getConnection().send(new ServerboundResourcePackPacket(push.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
               mc.getConnection().send(new ServerboundResourcePackPacket(push.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            } finally {
               this.bypass = false;
            }
         }
      }
   }
}
