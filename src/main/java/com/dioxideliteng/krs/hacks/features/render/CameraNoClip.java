package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class CameraNoClip extends Module {

    @Setting
    public static final FloatValue distance = new FloatValue("Distance", 0f, -4f, 100f, "m");

    public CameraNoClip() {
        super("Camera No Clip", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, false);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

}
