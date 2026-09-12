package com.dioxidelite.event.events;

import com.dioxidelite.event.Event;

/** Supplies the yaw used to apply the local player's ground-jump impulse. */
public final class JumpEvent extends Event {

    private float yaw;

    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
