package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;

public final class EventKey extends Event {
   private int key;
   private int action;

   public EventKey() {
   }

   public EventKey(int key, int action) {
      this.key = key;
      this.action = action;
   }

   public EventKey reset(int key, int action) {
      this.key = key;
      this.action = action;
      return this;
   }

   public int getKey() {
      return this.key;
   }

   public int getAction() {
      return this.action;
   }
}
