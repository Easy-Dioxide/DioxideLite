package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;

/** Fires when a keyboard key is pressed, repeated or released. */
public class EventKey extends Event {
   private int key;
   private int action;

   public int getKey() {
      return this.key;
   }

   public int getAction() {
      return this.action;
   }

   public EventKey reset(int key, int action) {
      this.key = key;
      this.action = action;
      return this;
   }
}
