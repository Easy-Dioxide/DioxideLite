package com.dioxideliteng.krs.hacks.features.movement.speed;

import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.utils.IMinecraft;

public interface SpeedEvent extends IMinecraft {
    String getName();
    void onUpdate(UpdateEvent event);
    void onMotion(MotionEvent event);
    void onTick(TickEvent event);
    void onSendPacket(SendPacketEvent event);
    void onReceivedPacket(ReceivedPacketEvent event);
}
