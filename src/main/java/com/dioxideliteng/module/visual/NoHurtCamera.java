package com.dioxideliteng.module.visual;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

public class NoHurtCamera extends Feature {
   private static final String NO_HURT_CAM_LABEL = "NoHurtCam";

   public NoHurtCamera() {
      super(NO_HURT_CAM_LABEL, Category.VISUAL);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.HURT_CAMERA) {
         event.setCancelled(true);
      }
   }
}
