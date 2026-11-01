package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class Rotations extends Module {
    @Setting
    public static final BooleanValue realistic = new BooleanValue("Realistic", false);

    @Setting
    public static final BooleanValue capeFix = new BooleanValue("Cape Fix", false);

    public Rotations() {
        super("Rotations", ModuleCategory.Render, Keys.KEY_UNKNOWN, true, false);
    }

    public static boolean shouldUseVanilla() {
        return ModuleManager.getModuleState(Rotations.class) && realistic.get();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
