package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.level.CaveFinder;
import com.dioxideliteng.krs.hacks.features.level.Xray;
import com.dioxideliteng.krs.utils.input.Keys;

public class FullBright extends Module {
    public FullBright() {
        super("Full Bright", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    public static boolean isBrightening() {
        return ModuleManager.getModuleState(Xray.class) || ModuleManager.getModuleState(CaveFinder.class) || ModuleManager.getModuleState(FullBright.class);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
