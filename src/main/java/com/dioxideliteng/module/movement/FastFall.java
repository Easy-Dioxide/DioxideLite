package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.FastFall) and adapted to the client. */
public class FastFall extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String MOTION_LABEL = "Motion";
   private static final String SHIFT = "Shift";
   private static final String ALWAYS = "Always";

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, SHIFT, new String[]{SHIFT, ALWAYS});
   private final NumberSetting motion = new NumberSetting(MOTION_LABEL, this, 0.8, 0.1, 4.0, 0.1);

   public FastFall() {
      super("Fast Fall", Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null || player.onGround() || player.isFallFlying() || player.isInWater() || player.isInLava()) {
         return;
      }
      if (this.mode.is(SHIFT) && !mc.options.keyShift.isDown()) {
         return;
      }
      if (player.getDeltaMovement().y < 0.0) {
         double targetY = -this.motion.getValue();
         var dm = player.getDeltaMovement();
         player.setDeltaMovement(dm.x, targetY, dm.z);
      }
   }
}
