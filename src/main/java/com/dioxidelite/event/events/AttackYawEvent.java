package com.dioxidelite.event.events;

import com.dioxidelite.event.Event;

public final class AttackYawEvent extends Event {

    private float yaw;

    public AttackYawEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
