package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class AntiBlind extends Module {

    @Setting
    public static final BooleanValue fire = new BooleanValue("Fire", true);

    @Setting
    public static final BooleanValue pumpkin = new BooleanValue("Pumpkin", true);

    @Setting
    public static final BooleanValue camera = new BooleanValue("Camera", true);

    @Setting
    public static final BooleanValue effects = new BooleanValue("Effects", true);

    public AntiBlind() {
        super("Anti Blind", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

}
