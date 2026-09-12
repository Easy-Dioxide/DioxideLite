package com.dioxidelite.ui.dioxide;

import com.dioxidelite.module.modules.ClickGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.awt.Color;

/** Presentation-only theme router. It never changes gameplay/module behaviour. */
public final class DioxideThemeController {
    public enum Theme { LIQUID_GLASS, MINIMAL, SIGNATURE }
    private static Theme current = Theme.LIQUID_GLASS;
    private DioxideThemeController() {}
    public static Theme current() { return current; }
    public static void apply(Theme theme, Screen parent) {
        current = theme == null ? Theme.LIQUID_GLASS : theme;
        ClickGui gui = ClickGui.INSTANCE;
        switch (current) {
            case LIQUID_GLASS -> { gui.mode.set(ClickGui.Mode.Pop); gui.daylightMode.set(false); gui.accent.set(new Color(120, 200, 255)); }
            case MINIMAL -> { gui.mode.set(ClickGui.Mode.Drop); gui.daylightMode.set(false); gui.accent.set(new Color(205, 215, 225)); }
            case SIGNATURE -> { gui.mode.set(ClickGui.Mode.Pop); gui.daylightMode.set(true); gui.accent.set(new Color(105, 185, 255)); }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.setScreen(gui.createScreen(parent));
    }
    public static void cycle(Screen parent) {
        Theme[] values = Theme.values();
        apply(values[(current.ordinal() + 1) % values.length], parent);
    }
}
