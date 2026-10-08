package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;

public class EventSlowdown extends Event {
   private float speedMultiplier;

   public EventSlowdown reset(float speedMultiplier) {
      this.speedMultiplier = speedMultiplier;
      return this;
   }

   public float getSpeedMultiplier() {
      return this.speedMultiplier;
   }

   public void setSpeedMultiplier(float speedMultiplier) {
      this.speedMultiplier = speedMultiplier;
   }
}
