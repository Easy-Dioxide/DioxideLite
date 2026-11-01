package com.dioxideliteng.krs.hacks.features.level;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.entity.PlayerUtil;
import com.dioxideliteng.krs.utils.input.Keys;

public class VisualSpoofer extends Module {

    public VisualSpoofer() {
        super("Visual Spoofer", ModuleCategory.Level, Keys.KEY_UNKNOWN, true, false);
    }

    @Override
    public void onDisable() {
        PlayerUtil.INSTANCE.fullResetSpoofState();
    }

    @Override
    public void onEnable() {
    }
}
