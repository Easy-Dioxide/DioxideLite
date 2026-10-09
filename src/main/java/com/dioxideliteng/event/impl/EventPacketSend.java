package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.network.protocol.Packet;

public final class EventPacketSend extends Event {
   private final Packet<?> packet;

   public EventPacketSend() {
      this.packet = null;
   }

   public EventPacketSend(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }
}
