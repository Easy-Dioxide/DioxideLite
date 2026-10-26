package com.dioxideliteng.krs.events.features;

import com.dioxideliteng.krs.events.EventArgument;
import com.dioxideliteng.krs.events.EventListener;
import java.util.Objects;

public class CancelBlockBreakingEvent extends EventArgument {
    @Override
    public void call(EventListener listener) {
        Objects.requireNonNull(listener).onCancelBlockBreaking(this);
    }
}