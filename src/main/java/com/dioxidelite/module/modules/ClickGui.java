package com.dioxidelite.module.modules;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.ui.clickgui.PopClickGuiScreen;
import com.dioxidelite.ui.clickgui.WindowClickGuiScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

/**
 * Opens the responsive Setsuna or Drop ClickGUI. Non-toggleable: pressing its keybind
 * (Right Shift by default) runs {@link #onTrigger()} instead of flipping an
 * enabled flag. The accent colour is shared by all client UI surfaces.
 */
public final class ClickGui extends Module {

    public static final ClickGui INSTANCE = new ClickGui();

    public enum Mode { Setsuna, Drop }

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Setsuna));
    public final BooleanSetting daylightMode = add(new BooleanSetting("Daylight Mode", false)
            .visibleWhen(() -> mode.is(Mode.Setsuna)));
    public final IntSetting popBackgroundBlur = add(new IntSetting("Background Blur", 5, 0, 10, 1)
            .visibleWhen(() -> mode.is(Mode.Setsuna)));
    public final ColorSetting accent = add(new ColorSetting("Accent", new Color(166, 86, 238), false));
    public final IntSetting guiScale = add(new IntSetting("GUI Scale", 100, 65, 125, 5));

    private ClickGui() {
        super("ClickGUI", Category.CLIENT);
        setToggleable(false);
        setDefaultKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }


    @Override
    protected void onTrigger() {
        mc.setScreen(createScreen(null));
    }

    public Screen createScreen(Screen parent) {
        return switch (mode.get()) {
            case Drop -> new WindowClickGuiScreen(parent);
            case Setsuna -> new PopClickGuiScreen(parent);
        };
    }
}
