package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffects;

/**
 * Port of Krs AntiLevitation (com.instrumentalist.krs.hacks.features.movement.AntiLevitation)
 * to DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs ReceivedPacketEvent -> {@code event instanceof EventPacketReceive} (cancel via
 *    {@code setCancelled(true)}, same wire effect as Krs {@code event.cancel()});
 *  - Krs UpdateEvent -> {@link Events#UPDATE}.
 */
public class AntiLevitation2 extends Feature {

   public AntiLevitation2() {
      super("AntiLevitation2", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketReceive receiving) {
         var player = mc.player;
         if (player == null || !(receiving.getPacket() instanceof ClientboundUpdateMobEffectPacket packet))
            return;

         if (packet.getEntityId() == player.getId() && packet.getEffect() == MobEffects.LEVITATION)
            receiving.setCancelled(true);
      } else if (event == Events.UPDATE) {
         if (mc.player != null && mc.player.hasEffect(MobEffects.LEVITATION))
            mc.player.removeEffect(MobEffects.LEVITATION);
      }
   }
}
