package com.dioxidelite.client.render.nativeui;

import com.dioxidelite.Config;

/** Central policy for keeping DioxideLite UI work bounded on the Minecraft render thread. */
public final class RenderPerformance {
    private static long lastSampleNs;
    private static float smoothedFrameMs = 16.67f;

    private RenderPerformance() {}

    public static void sampleFrame(long nowNs) {
        if (lastSampleNs != 0L) {
            float ms = (nowNs - lastSampleNs) / 1_000_000f;
            if (ms > 0f && ms < 250f) smoothedFrameMs += (ms - smoothedFrameMs) * .08f;
        }
        lastSampleNs = nowNs;
    }

    public static boolean reducedEffects() {
        return Config.performanceMode || smoothedFrameMs > 24f;
    }

    public static boolean animateDecorations() {
        return !reducedEffects();
    }

    public static float frameMs() {
        return smoothedFrameMs;
    }
}
