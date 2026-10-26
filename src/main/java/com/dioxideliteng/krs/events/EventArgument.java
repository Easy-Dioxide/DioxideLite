package com.dioxideliteng.krs.events;

public abstract class EventArgument {
    private boolean cancelled = false;

    protected EventArgument() {
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    public abstract void call(EventListener listener);
}
