package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PacketDuper).
 * Krs parity: in "2b2tJP" mode every outgoing move/move-vehicle packet is cancelled
 * (the 2b2tJP dupe relies on the server never receiving them).
 */
public class PacketDuper extends Feature {

   private final ModeSetting mode = new ModeSetting("Mode", this, "2b2tJP", new String[]{"2b2tJP"});

   public PacketDuper() {
      super("Packet Duper", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         this.setSuffix(this.mode.getValue());
         return;
      }
      if (event instanceof EventPacketSend sending && !sending.isCancelled()) {
         if (mc.player == null) return;

         Packet<?> packet = sending.getPacket();
         if (this.mode.is("2b2tJP")
               && (packet instanceof ServerboundMovePlayerPacket || packet instanceof ServerboundMoveVehiclePacket))
            sending.setCancelled(true);
      }
   }
}
