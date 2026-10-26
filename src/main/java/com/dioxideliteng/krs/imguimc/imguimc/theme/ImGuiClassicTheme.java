package com.dioxideliteng.krs.imguimc.theme;

import com.dioxideliteng.krs.imguimc.interfaces.Theme;

public class ImGuiClassicTheme implements Theme {
    @Override
    public void preRender() {
        KrsImGuiTheme.applyClassic();
    }

    @Override
    public void postRender() {

    }
}
