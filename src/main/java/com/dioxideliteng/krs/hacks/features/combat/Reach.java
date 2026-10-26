package com.dioxideliteng.krs.hacks.features.combat;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class Reach extends Module {

    public Reach() {
        super("Reach", ModuleCategory.Combat, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private static final BooleanValue block = new BooleanValue("Block", true);

    @Setting
    private static final FloatValue blockReach = new FloatValue("Block Reach", 6f, 0.1f, 6f, "m", block::get);

    @Setting
    private static final BooleanValue entity = new BooleanValue("Entity", true);

    @Setting
    private static final FloatValue entityReach = new FloatValue("Entity Reach", 6f, 0.1f, 6f, "m", entity::get);

    public static double hookBlockReach(double original) {
        if (ModuleManager.getModuleState(Reach.class) && block.get())
            return blockReach.get();

        return original;
    }

    public static double hookEntityReach(double original) {
        if (ModuleManager.getModuleState(Reach.class) && entity.get())
            return entityReach.get();

        return original;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}