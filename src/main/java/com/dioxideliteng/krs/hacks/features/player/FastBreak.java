package com.dioxideliteng.krs.hacks.features.player;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.ChatUtil;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class FastBreak extends Module {

    public FastBreak() {
        super("Fast Break", ModuleCategory.Player, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private static final FloatValue multiplier = new FloatValue("Multiplier", 1.4f, 0.1f, 3f);

    public static float hookFastBreak(float original) {
        if (ModuleManager.getModuleState(FastBreak.class) && original <= 18) {
            return Math.min(18, original * multiplier.get());
        }

        return original;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}