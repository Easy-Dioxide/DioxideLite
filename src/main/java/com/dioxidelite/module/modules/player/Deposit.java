package com.dioxidelite.module.modules.player;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.IntSetting;

/** OpenOnyx Deposit UI entry. Container interaction is intentionally routed through the existing screen API. */
public final class Deposit extends Module {
    public static final Deposit INSTANCE = new Deposit();
    public final BooleanSetting hotbarSweep = add(new BooleanSetting("Hotbar Sweep", true));
    public final IntSetting delay = add(new IntSetting("Delay", 50, 0, 500, 10));
    private Deposit() { super("Deposit", Category.PLAYER); }
}
