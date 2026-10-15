package com.dioxideliteng.module.misc;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PathFinder) and adapted.
 * Settings host for the teleport pathfinder used by KillAura2's TP Reach / Random TP
 * (see util.pathfinder.MainPathFinder). The module's enabled state is irrelevant - the
 * settings are always live, mirroring Krs where this module cannot be disabled.
 */
public class PathFinder extends Feature {
    public static PathFinder INSTANCE;

    public final ModeSetting mode;
    public final NumberSetting linearSteps;
    public final BooleanSetting verticalPassThrough;
    public final NumberSetting maxVerticalRange;

    public PathFinder() {
        super("PathFinder", Category.MISC);
        INSTANCE = this;
        this.mode = new ModeSetting("Mode", this, "Reconstruct",
                new String[]{"Reconstruct", "Linear"});
        this.linearSteps = new NumberSetting("Linear Steps", this, 5.0, 1.0, 10.0, 1.0);
        this.verticalPassThrough = new BooleanSetting("Allow Vertical Pass Through", this, false);
        this.maxVerticalRange = new NumberSetting("Max Vertical Range", this, 5.0, 2.0, 5.0, 1.0);
        this.linearSteps.setVisible(() -> this.mode.is("Linear"));
        this.verticalPassThrough.setVisible(() -> this.mode.is("Reconstruct"));
        this.maxVerticalRange.setVisible(() -> this.mode.is("Reconstruct") && this.verticalPassThrough.getValue());
    }

    public static String pathMode() {
        return INSTANCE != null ? INSTANCE.mode.getValue() : "Reconstruct";
    }

    public static int linearSteps() {
        return INSTANCE != null ? (int) INSTANCE.linearSteps.getValue() : 5;
    }

    public static boolean verticalPassThrough() {
        return INSTANCE != null && INSTANCE.verticalPassThrough.getValue();
    }

    public static int maxVerticalRange() {
        return INSTANCE != null ? (int) INSTANCE.maxVerticalRange.getValue() : 5;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
