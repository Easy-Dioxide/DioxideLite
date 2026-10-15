package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.client.multiplayer.ClientLevel;

public final class EventWorld extends Event {
   private ClientLevel previousWorld;
   private ClientLevel world;

   public EventWorld() {
   }

   public EventWorld(ClientLevel previousWorld, ClientLevel world) {
      this.previousWorld = previousWorld;
      this.world = world;
   }

   public EventWorld reset(ClientLevel previousWorld, ClientLevel world) {
      this.previousWorld = previousWorld;
      this.world = world;
      return this;
   }

   public ClientLevel getPreviousWorld() {
      return this.previousWorld;
   }

   public ClientLevel getWorld() {
      return this.world;
   }
}
