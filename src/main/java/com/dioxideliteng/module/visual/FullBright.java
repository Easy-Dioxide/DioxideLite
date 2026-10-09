package com.dioxideliteng.module.visual;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import mixins.OptionInstanceAccessor;

public class FullBright extends Feature {
   private static final String FULL_BRIGHT_LABEL = "FullBright";

   private double previousGamma;

   public FullBright() {
      super(FULL_BRIGHT_LABEL, Category.VISUAL);
   }

   @Override
   public void onEnable() {
      this.previousGamma = (Double)mc.options.gamma().get();
      ((OptionInstanceAccessor)(Object)mc.options.gamma()).setValue(100.0);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         ((OptionInstanceAccessor)(Object)mc.options.gamma()).setValue(100.0);
      }
   }

   @Override
   public void onDisable() {
      mc.options.gamma().set(this.previousGamma);
   }
}
