package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.MinecraftAccessor;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.NoCombatDelay) and adapted. */
public class NoCombatDelay extends Feature {
   public NoCombatDelay() {
      super("No Combat Delay", Category.COMBAT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      if (mc.player == null) return;
      ((MinecraftAccessor) mc).setMissTime(0);
   }
}
