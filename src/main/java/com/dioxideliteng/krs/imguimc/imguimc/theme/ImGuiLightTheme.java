package com.dioxideliteng.krs.imguimc.theme;

import com.dioxideliteng.krs.imguimc.interfaces.Theme;

public class ImGuiLightTheme implements Theme {
    @Override
    public void preRender() {
        KrsImGuiTheme.applyLight();
    }

    @Override
    public void postRender() {

    }
}
