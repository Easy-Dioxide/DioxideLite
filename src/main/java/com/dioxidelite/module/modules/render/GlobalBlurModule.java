package com.dioxidelite.module.modules.render;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.IntSetting;

/** Global backdrop-blur master switch + strength (ClickGUI Render category).
 *  Toggling the module ON enables the blur; OFF (default) draws no HUD backdrop
 *  blur at all, which also skips the per-frame backdrop snapshot — full-resolution
 *  visuals, zero blur cost. The strength slider overrides every HUD's own radius. */
public final class GlobalBlurModule extends Module {

    public static final GlobalBlurModule INSTANCE = new GlobalBlurModule();

    public final IntSetting strength = add(new IntSetting("Blur Strength", 6, 1, 16, 1));

    private GlobalBlurModule() {
        super("Global Blur", Category.RENDER);
    }
}
