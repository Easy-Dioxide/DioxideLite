package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;
import java.util.Objects;
import net.minecraft.network.protocol.Packet;

public class SendPacketEvent extends EventArgument {
    public Packet<?> packet;

    public SendPacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onSendPacket(this);
    }
}