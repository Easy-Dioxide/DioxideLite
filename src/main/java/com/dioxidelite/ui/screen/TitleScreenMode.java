package com.dioxidelite.ui.screen;

/** Session-scoped choice between DioxideLite's home and Minecraft's title screen. */
public final class TitleScreenMode {

    private static boolean vanilla;

    private TitleScreenMode() {
    }

    public static void useVanilla() {
        vanilla = true;
    }

    public static void useDioxideLite() {
        vanilla = false;
    }

    public static boolean isVanilla() {
        return vanilla;
    }
}
