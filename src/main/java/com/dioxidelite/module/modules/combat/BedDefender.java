package com.dioxidelite.module.modules.combat;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.IntSetting;

/** OpenOnyx BedDefender registration with safe, explicit configuration. */
public final class BedDefender extends Module {
    public static final BedDefender INSTANCE = new BedDefender();
    public final IntSetting targetsPerTick = add(new IntSetting("Targets Per Tick", 4, 1, 16, 1));
    public final IntSetting aimSpeed = add(new IntSetting("Aim Speed", 20, 1, 180, 1));
    public final BooleanSetting bedwarsOnly = add(new BooleanSetting("Bedwars Only", true));
    public final BooleanSetting debug = add(new BooleanSetting("Debug Logs", false));
    private BedDefender() { super("Bed Defender", Category.COMBAT); }
}
