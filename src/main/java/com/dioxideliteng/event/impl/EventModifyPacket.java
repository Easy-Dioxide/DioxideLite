package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.network.protocol.Packet;

/** Fires before an outgoing packet is sent; the packet can be replaced or the send cancelled. */
public final class EventModifyPacket extends Event {
   private Packet<?> packet;

   public EventModifyPacket() {
   }

   public EventModifyPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventModifyPacket reset(Packet<?> packet) {
      this.packet = packet;
      return this;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }
}
