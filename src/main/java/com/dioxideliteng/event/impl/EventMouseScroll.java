package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;

/** Fires when the mouse wheel is scrolled. */
public class EventMouseScroll extends Event {
   private double horizontal;
   private double vertical;

   public double getHorizontal() {
      return this.horizontal;
   }

   public double getVertical() {
      return this.vertical;
   }

   public EventMouseScroll reset(double horizontal, double vertical) {
      this.horizontal = horizontal;
      this.vertical = vertical;
      return this;
   }
}
