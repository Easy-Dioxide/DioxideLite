package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

public class Sprint extends Feature {
   private static final String SPRINT_LABEL = "Sprint";

   public Sprint() {
      super(SPRINT_LABEL, Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         mc.options.keySprint.setDown(true);
      }
   }
}
