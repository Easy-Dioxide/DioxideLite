package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class HUDHider extends Module {

    public HUDHider() {
        super("Hud Hider", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public String description() {
        return "Hides hud elements";
    }

    @Setting
    public static final BooleanValue board = new BooleanValue("ScoreBoard", true);

    @Setting
    public static final BooleanValue bos = new BooleanValue("BossBar", true);

    @Setting
    public static final BooleanValue bar = new BooleanValue("ActionBar", true);

    @Setting
    public static final BooleanValue titled = new BooleanValue("Title", true);

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
