package com.dioxideliteng.util.krs;

import java.text.DecimalFormat;

/**
 * Port of Krs math/Interpolation (com.instrumentalist.krs.utils.math.Interpolation).
 */
public final class Interpolation {
    public static final Interpolation INSTANCE = new Interpolation();

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private Interpolation() {
    }

    public DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public void setDecimalFormat(DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
    }

    public float lerpWithTime(float start, float end, float delta, float deltaTime) {
        return start + (delta * deltaTime) * (end - start);
    }

    public float lerp(float start, float end, float delta) {
        return start + delta * (end - start);
    }

    public float valueLimitedLerpWithTime(float start, float end, float delta, float deltaTime) {
        return valueLimitedLerpWithTime(start, end, delta, deltaTime, 320);
    }

    public float valueLimitedLerpWithTime(float start, float end, float delta, float deltaTime, int limit) {
        float distance = Math.abs(end - start);

        if (Float.isNaN(distance) || Float.isInfinite(distance))
            return start;

        float lok = 2f;
        if (distance >= limit)
            lok = 1f;

        float dynamicDelta = delta + (distance / lok);
        float t = Math.max(0f, Math.min(1f, dynamicDelta * deltaTime));
        return start + (end - start) * t;
    }
}
