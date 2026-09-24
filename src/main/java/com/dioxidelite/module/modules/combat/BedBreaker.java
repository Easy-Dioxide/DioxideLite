package com.dioxidelite.module.modules.combat;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.modules.player.BedAura;

/** OpenOnyx BedBreaker entry mapped to DioxideLite's BedAura/bed-breaking engine. */
public final class BedBreaker extends Module {
    public static final BedBreaker INSTANCE = new BedBreaker();
    private BedBreaker() { super("Bed Breaker", Category.COMBAT); }
    @Override protected void onEnable() { if (!BedAura.INSTANCE.isEnabled()) BedAura.INSTANCE.setEnabled(true); }
    @Override protected void onDisable() { if (BedAura.INSTANCE.isEnabled()) BedAura.INSTANCE.setEnabled(false); }
}
