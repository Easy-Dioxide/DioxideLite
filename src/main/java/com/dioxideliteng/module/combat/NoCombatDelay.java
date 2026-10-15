package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.MinecraftAccessor;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.NoCombatDelay).
 * Krs writes the protected Minecraft#missTime field directly through a @Shadow
 * field in its MinecraftClientMixin; the port goes through the client's
 * MinecraftAccessor mixin accessor for the same field.
 */
public class NoCombatDelay extends Feature {

   public NoCombatDelay() {
      super("No Combat Delay", Category.COMBAT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      if (mc.player == null) return;

      ((MinecraftAccessor) mc).setMissTime(0);
   }
}
