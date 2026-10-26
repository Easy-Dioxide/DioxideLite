package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class Capes extends Module {

    @Setting
    public static final BooleanValue customCape = new BooleanValue("Custom Cape", false);

    @Setting
    public static final BooleanValue capeOverride = new BooleanValue("Cape Override", true, customCape::get);

    @Setting
    public static final BooleanValue enchantmentGlint = new BooleanValue("Enchantment Glint", true);

    @Setting
    public static final BooleanValue oldCapeMovement = new BooleanValue("1.8 Cape Movement", true);

    public Capes() {
        super("Capes", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

}
