package com.dioxidelite.util.rotation;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/util/rotation/Rot2f.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


/**
 * A mutable yaw/pitch pair (both in degrees). Used throughout the rotation engine
 * as the unit of "where to aim": modules build a target {@code Rot2f}, the manager
 * smooths toward it, and the send-position mixin reports it to the server.
 */
public final class Rot2f {

    private float yaw;
    private float pitch;

    public Rot2f(float yaw, float pitch) {
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

    public void set(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rot2f copy() {
        return new Rot2f(yaw, pitch);
    }

    @Override
    public String toString() {
        return "Rot2f[yaw=" + yaw + ", pitch=" + pitch + "]";
    }
}
