package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.resources.Identifier;

public class EventSound extends Event {
   private Identifier soundId;

   public Identifier getSoundId() {
      return this.soundId;
   }

   public EventSound reset(Identifier soundId) {
      this.soundId = soundId;
      return this;
   }
}
