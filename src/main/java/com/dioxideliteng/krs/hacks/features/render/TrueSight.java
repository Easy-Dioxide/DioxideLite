package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class TrueSight extends Module {

    @Setting
    public static final BooleanValue entities = new BooleanValue("Entities", false);

    @Setting
    public static final BooleanValue barriers = new BooleanValue("Barriers", true);

    public TrueSight() {
        super("True Sight", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public static boolean requiresTrueSight(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity livingEntity)) {
            return false;
        }

        return livingEntity.isInvisible();
    }
}
