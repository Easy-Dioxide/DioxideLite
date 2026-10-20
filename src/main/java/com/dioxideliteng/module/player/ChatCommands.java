package com.dioxideliteng.module.player;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.ChatCommands).
 *
 * Adaptations:
 *  - the Krs TextValue "Prefix" has no free-text equivalent in this client's setting system
 *    (same adaptation as BrandSpoofer), so it is ported as a ModeSetting over the common
 *    chat-command prefixes (Krs default "." first);
 *  - the Krs default-on state (Module#activateInitialState) is a client-level concern handled
 *    by FeatureManager.loadEnabled; the module itself carries no extra state.
 */
public class ChatCommands extends Feature {

    public final ModeSetting prefix = new ModeSetting("Prefix", this, ".",
            new String[]{".", ",", "-", "!", "'"});

    public ChatCommands() {
        super("ChatCommands", Category.PLAYER);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

}
