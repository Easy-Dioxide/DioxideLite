package com.dioxidelite.module.modules.render;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.ui.hud.HudEditorScreen;
import net.minecraft.client.gui.screens.ChatScreen;

/** Opens the drag-and-drop HUD layout editor. */
public final class HudEditorModule extends Module {

    public static final HudEditorModule INSTANCE = new HudEditorModule();

    private HudEditorModule() {
        super("HUD Editor", Category.CLIENT);
        setToggleable(false);
    }

    @Override
    protected void onTrigger() {
        if (mc.screen instanceof ChatScreen) {
            HudEditorScreen.openOverlay();
        } else {
            mc.setScreen(new HudEditorScreen());
        }
    }
}
