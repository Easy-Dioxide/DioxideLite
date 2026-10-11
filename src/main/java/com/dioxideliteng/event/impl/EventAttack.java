package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.world.entity.Entity;

/** Fires before the local player attacks an entity. Cancelling prevents the attack. */
public class EventAttack extends Event {
   private Entity entity;

   public Entity getEntity() {
      return this.entity;
   }

   public EventAttack reset(Entity entity) {
      this.entity = entity;
      return this;
   }
}
