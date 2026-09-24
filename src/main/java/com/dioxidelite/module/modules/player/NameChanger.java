package com.dioxidelite.module.modules.player;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.StringSetting;
import com.dioxidelite.onyx.engine.OnyxPlayerEngine;

/** Local display-name override used by HUD/name-tag adapters. */
public final class NameChanger extends Module {
    public static final NameChanger INSTANCE = new NameChanger();
    public final StringSetting displayName = add(new StringSetting("Display Name", ""));
    public final BooleanSetting nameTag = add(new BooleanSetting("Nametag", true));
    public final BooleanSetting chat = add(new BooleanSetting("Chat", false));
    private static final OnyxPlayerEngine ONYX = new OnyxPlayerEngine();
    private NameChanger() { super("Name Changer", Category.PLAYER); }
    public static String apply(String original) {
        NameChanger m = INSTANCE;
        return ONYX.applyDisplayName(original, m.isEnabled(), m.nameTag.get(), m.displayName.get());
    }
}
