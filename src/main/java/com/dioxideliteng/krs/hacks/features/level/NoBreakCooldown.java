package com.dioxideliteng.krs.hacks.features.level;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.input.Keys;

public class NoBreakCooldown extends Module {

    public NoBreakCooldown() {
        super("No Break Cooldown", ModuleCategory.Level, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
