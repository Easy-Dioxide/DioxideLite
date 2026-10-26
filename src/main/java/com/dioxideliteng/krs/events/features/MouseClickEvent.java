package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;

import java.util.Objects;

public class MouseClickEvent extends EventArgument {
    public final long window;
    public final int button;
    public final int action;
    public final int mods;

    public MouseClickEvent(long window, int button, int action, int mods) {
        this.window = window;
        this.button = button;
        this.action = action;
        this.mods = mods;
    }

    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onMouseClick(this);
    }
}