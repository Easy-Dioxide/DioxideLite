package com.dioxideliteng.module.movement.speed;

import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import net.minecraft.client.Minecraft;

/**
 * Krs parity: SpeedEvent, the per-mode contract of the Krs Speed family
 * (com.instrumentalist.krs.hacks.features.movement.speed.SpeedEvent). The Krs
 * IMinecraft base (the shared mc handle every mode reads) is inlined as an
 * interface constant, exactly like the Krs interface does.
 *
 * <p>Event signatures are adapted to the client event classes:
 * Krs UpdateEvent -> EventUpdate, Krs MotionEvent -> MotionEvent (translated
 * to/from Events.PRE_MOTION / Events.POST_MOTION by SpeedModeManager), Krs
 * TickEvent -> EventTick, Krs SendPacketEvent -> EventPacketSend, Krs
 * ReceivedPacketEvent -> EventPacketReceive.
 */
public interface SpeedEvent {
    Minecraft mc = Minecraft.getInstance();

    String getName();

    void onUpdate(EventUpdate event);

    void onMotion(MotionEvent event);

    void onTick(EventTick event);

    void onSendPacket(EventPacketSend event);

    void onReceivedPacket(EventPacketReceive event);
}
