package com.dioxideliteng.module.movement.fly;

import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.event.impl.EventTick;
import com.dioxideliteng.event.impl.EventUpdate;
import com.dioxideliteng.module.movement.MotionEvent;
import net.minecraft.client.Minecraft;

/**
 * Krs parity: FlyEvent, the per-mode contract of the Krs Fly family
 * (com.instrumentalist.krs.hacks.features.movement.fly.FlyEvent). The Krs
 * IMinecraft base (the shared mc handle every mode reads) is inlined as an
 * interface constant, exactly like the Krs interface does.
 *
 * <p>Event signatures are adapted to the client event classes:
 * Krs UpdateEvent -> EventUpdate, Krs MotionEvent -> MotionEvent (translated
 * to/from Events.PRE_MOTION / Events.POST_MOTION by FlyModeManager), Krs
 * TickEvent -> EventTick, Krs SendPacketEvent -> EventPacketSend, Krs
 * ReceivedPacketEvent -> EventPacketReceive, Krs BlockEvent -> EventBlock.
 */
public interface FlyEvent {
    Minecraft mc = Minecraft.getInstance();

    String getName();

    void onUpdate(EventUpdate event);

    void onMotion(MotionEvent event);

    void onTick(EventTick event);

    void onSendPacket(EventPacketSend event);

    void onReceivedPacket(EventPacketReceive event);

    void onBlock(EventBlock event);
}
