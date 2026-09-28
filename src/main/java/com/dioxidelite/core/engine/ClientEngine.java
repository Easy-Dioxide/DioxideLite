package com.dioxidelite.core.engine;

import com.dioxidelite.DioxideLite;

/** Shared client backend marker. Rendering remains owned by DioxideLite/Skija. */
public final class ClientEngine {
    private ClientEngine() {}

    public static void verifyClientContext() {
        if (DioxideLite.mc() == null) throw new IllegalStateException("Minecraft client context unavailable");
    }
}
