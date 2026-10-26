package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.value.ListValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class ImGui extends Module {

    public ImGui() {
        super("Im Gui", ModuleCategory.Render, Keys.KEY_UNKNOWN, true, false);
    }

    public static int getOpenGuiKey() {
        ImGui imGui = ModuleManager.getModule(ImGui.class);
        return imGui != null ? imGui.key : Keys.KEY_UNKNOWN;
    }

    @Setting
    public static final ListValue theme = new ListValue(
            "Theme",
            new String[]{"Light", "Classic", "Dark"},
            "Classic"
    );

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
