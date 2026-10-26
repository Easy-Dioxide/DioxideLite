package com.dioxideliteng.krs.utils.math;

import java.util.concurrent.TimeUnit;

public final class MSTimer {
    private long timeNanos = Long.MIN_VALUE;

    public long currentTime() {
        if (timeNanos == Long.MIN_VALUE)
            return Long.MAX_VALUE;

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timeNanos);
    }

    public boolean hasTimePassed(final long milliseconds) {
        return milliseconds <= 0L
                || timeNanos == Long.MIN_VALUE
                || System.nanoTime() - timeNanos >= TimeUnit.MILLISECONDS.toNanos(milliseconds);
    }

    public void reset() {
        timeNanos = System.nanoTime();
    }
}
