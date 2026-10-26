package com.dioxideliteng.krs.hacks.features.dev;

import com.dioxideliteng.krs.events.features.AttackEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.ChatUtil;
import net.minecraft.world.entity.Entity;
import com.dioxideliteng.krs.utils.input.Keys;

public class Debugga extends Module {

    public Debugga() {
        super("Debugga", ModuleCategory.Dev, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onAttack(AttackEvent event) {
        Entity target = event.entity;
        if (target == null) return;
        ChatUtil.printChat(target.getUUID() + "");
    }
}
