package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffects;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.AntiLevitation) and adapted. */
public class AntiLevitation extends Feature {

   public AntiLevitation() {
      super("Anti Levitation", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventPacketReceive e) {
         var player = mc.player;
         if (player == null || !(e.getPacket() instanceof ClientboundUpdateMobEffectPacket packet)) return;
         if (packet.getEntityId() == player.getId() && packet.getEffect() == MobEffects.LEVITATION)
            e.setCancelled(true);
      } else if (event == Events.TICK) {
         if (mc.player != null && mc.player.hasEffect(MobEffects.LEVITATION))
            mc.player.removeEffect(MobEffects.LEVITATION);
      }
   }
}
