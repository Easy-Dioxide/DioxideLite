package com.dioxideliteng.krs.hacks.features.dev;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.ReceivedPacketEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.ChatUtil;
import com.dioxideliteng.krs.utils.input.Keys;

public class Sex extends Module {

    public Sex() {
        super("Sex", ModuleCategory.Dev, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        if (KrsClient.loaded) {
            ChatUtil.printChat("Do not use this module");
            this.toggle();
        }
    }
}
