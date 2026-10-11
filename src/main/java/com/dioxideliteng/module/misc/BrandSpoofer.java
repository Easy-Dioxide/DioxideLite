package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.BrandSpoofer) and adapted to the client.
 * Krs replaces the outgoing brand payload via ModifyPacketEvent; the client can only cancel, so this cancels
 * the original brand packet and re-sends a forged one. TextValue is replaced with a ModeSetting of presets.
 */
public class BrandSpoofer extends Feature {
   private static final String VANILLA = "vanilla";

   private final ModeSetting brand = new ModeSetting("Brand", this, VANILLA,
         new String[]{VANILLA, "forge", "fabric", "lunarclient"});

   private boolean spoofing;

   public BrandSpoofer() {
      super("BrandSpoofer", Category.MISC);
   }

   @Override
   public void onDisable() {
      this.spoofing = false;
   }

   @Override
   public void onEvent(Event event) {
      if (!(event instanceof EventPacketSend sending) || this.spoofing) return;
      if (sending.getPacket() instanceof ServerboundCustomPayloadPacket payload
            && payload.payload() instanceof BrandPayload) {
         event.setCancelled(true);
         if (mc.getConnection() == null) return;
         this.spoofing = true;
         try {
            mc.getConnection().send(new ServerboundCustomPayloadPacket(new BrandPayload(this.brand.getValue())));
         } finally {
            this.spoofing = false;
         }
      }
   }
}
