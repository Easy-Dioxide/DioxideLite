package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.input.Keys;

public class ClickGui extends Module {

    public ClickGui() {
        super("Click Gui", ModuleCategory.Render, Keys.KEY_RIGHT_SHIFT, true, false);
    }

    public static int getOpenGuiKey() {
        ClickGui clickGui = ModuleManager.getModule(ClickGui.class);
        return clickGui != null ? clickGui.key : Keys.KEY_UNKNOWN;
    }

    @Override
    public String description() {
        return "344 is RIGHT_SHIFT";
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        if (KrsClient.loaded)
            this.toggle();
    }
}
