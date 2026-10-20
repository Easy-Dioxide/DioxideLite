package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.LivingEntityAccessor;

/**
 * Port of Krs NoJumpCooldown (com.instrumentalist.krs.hacks.features.movement.NoJumpCooldown)
 * to DioxideLiteNG. Logic is a 1:1 copy; only the event/field API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - {@code player.noJumpDelay = 0} (public in Krs mappings) uses the existing
 *    {@link LivingEntityAccessor} setter, the same pattern as the native NoJumpDelay.
 */
public class NoJumpCooldown2 extends Feature {

   public NoJumpCooldown2() {
      super("NoJumpCooldown2", Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;

      if (mc.player == null || !mc.player.onGround()) return;

      ((LivingEntityAccessor) mc.player).setNoJumpDelay(0);
   }
}
