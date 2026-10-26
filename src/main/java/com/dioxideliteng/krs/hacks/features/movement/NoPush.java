package com.dioxideliteng.krs.hacks.features.movement;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.input.Keys;

public class NoPush extends Module {

    public NoPush() {
        super("No Push", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
