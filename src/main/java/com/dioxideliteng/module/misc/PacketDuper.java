package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PacketDuper) and adapted to the client. */
public class PacketDuper extends Feature {
   private static final String MODE_2B2TJP = "2b2tJP";

   private final ModeSetting mode = new ModeSetting("Mode", this, MODE_2B2TJP, new String[]{MODE_2B2TJP});

   public PacketDuper() {
      super("PacketDuper", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         this.setSuffix(this.mode.getValue());
         return;
      }
      if (!(event instanceof EventPacketSend sending)) return;
      if (mc.player == null) return;
      var packet = sending.getPacket();
      if (this.mode.is(MODE_2B2TJP)
            && (packet instanceof ServerboundMovePlayerPacket || packet instanceof ServerboundMoveVehiclePacket)) {
         event.setCancelled(true);
      }
   }
}
