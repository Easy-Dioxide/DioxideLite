package com.dioxidelite.client.gui.clickgui;

import com.dioxidelite.Config;

public final class UiText {
    private UiText() {}

    public static String t(String zh, String en) {
        return Config.isChinese ? zh : en;
    }
}
