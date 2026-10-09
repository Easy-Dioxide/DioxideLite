package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.TimerController;

public class Timer extends Feature {
   private static final String TIMER_LABEL = "Timer";
   private static final String SPEED_LABEL = "Speed";

   private final NumberSetting speed;

   @Override
   public void onDisable() {
      TimerController.setMultiplier(1.0F);
   }

   public Timer() {
      super(TIMER_LABEL, Category.MOVEMENT);
      this.speed = new NumberSetting(SPEED_LABEL, this, 1.0, 0.1, 5.0, 0.1);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         TimerController.setMultiplier((float)this.speed.getValue());
      }
   }
}
