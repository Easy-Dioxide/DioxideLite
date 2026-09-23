package com.dioxidelite.event.events;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/event/events/RotationAnimationEvent.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Event;

/**
 * Supplies the rotations used to animate the local player's third-person
 * model. Silent rotations can replace these values without moving the camera.
 */
public final class RotationAnimationEvent extends Event {

    private float yaw;
    private float lastYaw;
    private float pitch;
    private float lastPitch;

    public RotationAnimationEvent(float yaw, float lastYaw, float pitch, float lastPitch) {
        this.yaw = yaw;
        this.lastYaw = lastYaw;
        this.pitch = pitch;
        this.lastPitch = lastPitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }
}
