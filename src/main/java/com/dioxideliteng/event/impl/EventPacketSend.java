package com.dioxideliteng.event.impl;

import com.dioxideliteng.event.Event;
import net.minecraft.network.protocol.Packet;

public final class EventPacketSend extends Event {
   private Packet<?> packet;

   public EventPacketSend() {
      this.packet = null;
   }

   public EventPacketSend(Packet<?> packet) {
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   /** Swaps the outgoing packet; the connection sends the replacement instead (ported KRS modules rely on this). */
   public void replace(Packet<?> replacement) {
      this.packet = replacement;
   }
}
