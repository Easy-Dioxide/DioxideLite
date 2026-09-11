package com.dioxidelite.client.gui.clickgui;

import net.minecraft.client.gui.screens.Screen;

/**
 * GLASS theme — restores the v1.7 pure transparent Liquid Glass ClickGUI as an
 * independent theme. The panel is drawn with the real glass material
 * (SkiaBlurRenderer framebuffer blur + translucent fill + rim highlight)
 * instead of the opaque dark base used by ORIGINAL on the GL backend.
 */
public final class GlassThemeScreen extends NewSettingsScreen {
    public GlassThemeScreen(Screen parent) {
        super(parent);
    }

    @Override
    protected boolean usePureGlass() {
        return true;
    }
}
