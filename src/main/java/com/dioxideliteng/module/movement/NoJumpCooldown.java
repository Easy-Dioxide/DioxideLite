package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.LivingEntityAccessor;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.NoJumpCooldown) and adapted. */
public class NoJumpCooldown extends Feature {

   public NoJumpCooldown() {
      super("No Jump Cooldown", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      if (mc.player == null || !mc.player.onGround()) return;
      ((LivingEntityAccessor) mc.player).setNoJumpDelay(0);
   }
}
