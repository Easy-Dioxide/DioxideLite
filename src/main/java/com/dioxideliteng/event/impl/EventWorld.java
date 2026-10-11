package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.client.multiplayer.ClientLevel;

/** Fires when the client changes or leaves a level. */
public class EventWorld extends Event {
   private ClientLevel previousWorld;
   private ClientLevel world;

   public ClientLevel getPreviousWorld() {
      return this.previousWorld;
   }

   public ClientLevel getWorld() {
      return this.world;
   }

   public EventWorld reset(ClientLevel previousWorld, ClientLevel world) {
      this.previousWorld = previousWorld;
      this.world = world;
      return this;
   }
}
