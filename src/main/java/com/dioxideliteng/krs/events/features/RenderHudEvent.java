package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;

import java.util.Objects;

public class RenderHudEvent extends EventArgument {
    public final float tickDelta;

    public RenderHudEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onRenderHud(this);
    }
}
