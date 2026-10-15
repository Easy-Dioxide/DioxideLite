package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.world.entity.Entity;

public final class EventAttack extends Event {
   private Entity entity;

   public EventAttack() {
   }

   public EventAttack(Entity entity) {
      this.entity = entity;
   }

   public EventAttack reset(Entity entity) {
      this.entity = entity;
      return this;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
