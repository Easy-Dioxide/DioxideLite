package com.dioxideliteng.krs.hacks.features.player;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.TextValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class ChatCommands extends Module {

    @Setting
    public static final TextValue prefix = new TextValue("Prefix", ".");

    public ChatCommands() {
        super("Chat Commands", ModuleCategory.Player, Keys.KEY_UNKNOWN, true, false);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

}
