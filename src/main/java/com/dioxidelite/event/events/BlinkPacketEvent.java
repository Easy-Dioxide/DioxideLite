package com.dioxidelite.event.events;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/event/events/BlinkPacketEvent.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Event;
import net.minecraft.network.protocol.Packet;

public final class BlinkPacketEvent extends Event {
    public enum Origin { INCOMING, OUTGOING }
    public enum Action { FLUSH(0), PASS(1), QUEUE(2); private final int priority; Action(int p) { priority = p; } }
    private final Packet<?> packet;
    private final Origin origin;
    private Action action = Action.FLUSH;
    public BlinkPacketEvent(Packet<?> packet, Origin origin) { this.packet = packet; this.origin = origin; }
    public Packet<?> packet() { return packet; }
    public Origin origin() { return origin; }
    public Action action() { return action; }
    public void setAction(Action value) { if (value.priority > action.priority) action = value; }
}
