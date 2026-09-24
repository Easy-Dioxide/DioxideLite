package com.dioxidelite.onyx.engine;

import com.dioxidelite.DioxideLite;

/** Shared client backend marker. Rendering remains owned by DioxideLite/Skija. */
public final class OnyxClientEngine {
    private OnyxClientEngine() {}

    public static void verifyClientContext() {
        if (DioxideLite.mc() == null) throw new IllegalStateException("Minecraft client context unavailable");
    }
}
