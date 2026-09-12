package com.dioxidelite.module.modules;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.ButtonSetting;
import com.dioxidelite.ui.screen.AltManagerScreen;

/** Opens the account manager without maintaining an enabled state. */
public final class AltManagerModule extends Module {

    public static final AltManagerModule INSTANCE = new AltManagerModule();

    public final ButtonSetting open = add(new ButtonSetting("Open", this::openScreen));

    private AltManagerModule() {
        super("Alt Manager", Category.CLIENT);
        setToggleable(false);
    }

    @Override
    protected void onTrigger() {
        openScreen();
    }

    private void openScreen() {
        mc.setScreen(new AltManagerScreen(mc.screen));
    }
}
