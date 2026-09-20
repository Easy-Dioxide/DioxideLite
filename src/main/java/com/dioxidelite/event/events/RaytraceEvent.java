package com.dioxidelite.event.events;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/event/events/RaytraceEvent.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Event;

/** Lets silent-rotation features replace the yaw and pitch used for a view ray. */
public final class RaytraceEvent extends Event {

    private float yaw;
    private float pitch;

    public RaytraceEvent(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
