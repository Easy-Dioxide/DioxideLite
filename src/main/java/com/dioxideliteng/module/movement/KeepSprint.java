package com.dioxideliteng.module.movement;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;

public class KeepSprint extends Feature {
   private static final String KEEP_SPRINT_LABEL = "KeepSprint";
   private static final String SPEED_LABEL = "Speed";

   private NumberSetting speed;

   public KeepSprint() {
      super(KEEP_SPRINT_LABEL, Category.MOVEMENT);
      this.speed = new NumberSetting(SPEED_LABEL, this, 0.6, 0.6, 1.0, 0.05);
   }

   public double getSprintMotionMultiplier() {
      return !this.isEnabled() ? 0.6 : this.speed.getValue();
   }
}
