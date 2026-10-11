package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.SafeWalk) and adapted. */
public class SafeWalk extends Feature {
   private static final String ONLY_MOVING_LABEL = "Only Moving";
   private static final String IGNORE_SNEAK_LABEL = "Ignore Sneak";

   private final BooleanSetting onlyMoving = new BooleanSetting(ONLY_MOVING_LABEL, this, false);
   private final BooleanSetting ignoreSneak = new BooleanSetting(IGNORE_SNEAK_LABEL, this, false);

   public SafeWalk() {
      super("Safe Walk", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.BLOCK_EDGE) return;
      var player = mc.player;
      if (player == null || player.isFallFlying() || player.getAbilities().flying) return;

      if (this.onlyMoving.getValue() && !this.isMoving()) return;

      if (!this.ignoreSneak.getValue() && player.isShiftKeyDown()) return;

      event.setCancelled(true);
   }

   private boolean isMoving() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }
}
