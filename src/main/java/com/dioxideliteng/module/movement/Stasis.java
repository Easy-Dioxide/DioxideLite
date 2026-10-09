package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;

public class Stasis extends Feature {
   private static final String DELAY_LABEL = "Delay";
   private static final String STASIS_LABEL = "Stasis";

   private final NumberSetting delay = new NumberSetting(DELAY_LABEL, this, 15.0, 10.0, 50.0, 1.0);

   private int elapsedTicks;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.elapsedTicks++;
         if ((double)this.elapsedTicks >= this.delay.getValue()) {
            this.elapsedTicks = 0;
            return;
         }

         event.setCancelled(true);
      }

      if (event == Events.PRE_MOTION) {
         mc.player.yRotO = mc.player.getYRot();
         mc.player.xRotO = mc.player.getXRot();
      }
   }

   public Stasis() {
      super(STASIS_LABEL, Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.elapsedTicks = 0;
   }
}
