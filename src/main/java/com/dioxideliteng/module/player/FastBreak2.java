package com.dioxideliteng.module.player;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.FastBreak).
 *
 * Adaptations:
 *  - static FloatValue mapped to an instance NumberSetting (settings are per-Feature here);
 *  - {@code ModuleManager.getModuleState(FastBreak.class)} replaced with the registered
 *    instance's enabled check;
 *  - the hook itself is called from a Krs mixin (block-break progress); no such mixin exists in
 *    this client, so the static hook is ported verbatim for the hook site to call.
 */
public class FastBreak2 extends Feature {

    private static FastBreak2 instance;

    private final NumberSetting multiplier = new NumberSetting("Multiplier", this, 1.4, 0.1, 3.0, 0.1);

    public FastBreak2() {
        super("FastBreak2", Category.PLAYER);
        instance = this;
    }

    public static float hookFastBreak(float original) {
        if (instance != null && instance.isEnabled() && original <= 18) {
            return Math.min(18, original * (float) instance.multiplier.getValue());
        }

        return original;
    }
}
