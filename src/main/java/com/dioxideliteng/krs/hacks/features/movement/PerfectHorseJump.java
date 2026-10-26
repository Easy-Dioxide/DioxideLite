package com.dioxideliteng.krs.hacks.features.movement;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.input.Keys;

public class PerfectHorseJump extends Module {

    public PerfectHorseJump() {
        super("Perfect Horse Jump", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    public static float modifiedHorseJump(float original) {
        if (ModuleManager.getModuleState(PerfectHorseJump.class))
            return 1f;

        return original;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
