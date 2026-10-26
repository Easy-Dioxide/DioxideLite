package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class KeyboardEvent extends EventArgument {

    public final int key, action;

    public KeyboardEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    @Override
    public void call(@Nullable EventListener listener) {
        Objects.requireNonNull(listener).onKey(this);
    }
}
