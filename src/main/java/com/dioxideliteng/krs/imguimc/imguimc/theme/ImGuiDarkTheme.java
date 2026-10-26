package com.dioxideliteng.krs.imguimc.theme;

import com.dioxideliteng.krs.imguimc.interfaces.Theme;

public class ImGuiDarkTheme implements Theme {
    @Override
    public void preRender() {
        KrsImGuiTheme.applyDark();
    }

    @Override
    public void postRender() {

    }
}
