package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventModifyPacket;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.BrandSpoofer).
 * Krs parity: the outgoing brand payload is replaced ("vanilla" by default, or the
 * configured brand when Custom Brand is on), and the world-change notification fires
 * once per faked brand.
 * Adaptation: the Krs TextValue "Brand String" has no free-text equivalent in this
 * client's setting system, so it is ported as a ModeSetting over the common brand
 * values (Krs default "NerdClient" first); the bypass logic itself is unchanged.
 */
public class BrandSpoofer extends Feature {

   private final BooleanSetting customBrand = new BooleanSetting("Custom Brand", this, false);
   private final ModeSetting brandString = new ModeSetting("Brand String", this, "NerdClient",
         new String[]{"NerdClient", "vanilla", "LunarClient", "Forge", "Fabric"});

   private boolean spoofed = false;

   public BrandSpoofer() {
      super("Brand Spoofer", Category.MISC);
      this.brandString.setVisible(this.customBrand::getValue);
   }

   @Override
   public void onDisable() {
      spoofed = false;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         if (spoofed) {
            String fakerString = customBrand.getValue() ? brandString.getValue() : "vanilla";
            DynamicIslandManager.notifyInfo("Brand Spoofer", "Faked client brand to \"" + fakerString + "\"");
            spoofed = false;
         }
         return;
      }
      if (event instanceof EventModifyPacket modifyPacket) {
         Packet<?> packet = modifyPacket.getPacket();

         if (packet instanceof ServerboundCustomPayloadPacket customPayloadPacket
               && customPayloadPacket.payload() instanceof BrandPayload) {
            String fakerString = customBrand.getValue() ? brandString.getValue() : "vanilla";
            modifyPacket.setPacket(new ServerboundCustomPayloadPacket(new BrandPayload(fakerString)));
            spoofed = true;
         }
      }
   }
}
