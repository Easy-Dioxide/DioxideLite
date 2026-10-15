package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PackSpoofer).
 * Krs parity: the outgoing resource-pack status packet is cancelled, and an incoming
 * resource-pack push is cancelled while the ACCEPTED/DOWNLOADED/SUCCESSFULLY_LOADED
 * status chain is sent silently, so the server believes the pack was applied.
 */
public class PackSpoofer extends Feature {

   public PackSpoofer() {
      super("Pack Spoofer", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         if (mc.player == null || mc.level == null) return;

         Packet<?> packet = sending.getPacket();
         if (packet instanceof ServerboundResourcePackPacket)
            sending.setCancelled(true);
         return;
      }
      if (event instanceof EventPacketReceive receiving && !receiving.isCancelled()) {
         if (mc.player == null || mc.level == null) return;

         Packet<?> packet = receiving.getPacket();
         if (packet instanceof ClientboundResourcePackPushPacket pushPacket) {
            receiving.setCancelled(true);
            PacketUtil.sendPacketAsSilent(new ServerboundResourcePackPacket(pushPacket.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
            PacketUtil.sendPacketAsSilent(new ServerboundResourcePackPacket(pushPacket.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
            PacketUtil.sendPacketAsSilent(new ServerboundResourcePackPacket(pushPacket.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            DynamicIslandManager.notifyInfo("Pack Spoofer", "Faked resource pack");
         }
      }
   }
}
