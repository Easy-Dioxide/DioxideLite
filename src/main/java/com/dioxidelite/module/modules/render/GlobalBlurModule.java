package com.dioxidelite.module.modules.render;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.IntSetting;

/**
 * Global backdrop-blur master switch for visual/HUD surfaces.
 *
 * <p>The module toggle is the master switch: disabled means no backdrop blur
 * is requested or captured anywhere. The strength setting controls the single
 * global blur radius used by HUD/fusion surfaces when enabled.</p>
 */
public final class GlobalBlurModule extends Module {

    public static final GlobalBlurModule INSTANCE = new GlobalBlurModule();

    public final IntSetting strength = add(new IntSetting(
            "Blur Strength", 4, 1, 16, 1).visibleWhen(this::isEnabled));

    private GlobalBlurModule() {
        super("Global Blur", Category.RENDER);
        // Explicitly off: a fresh profile must have zero backdrop-blur cost.
        setEnabled(false);
    }
}
