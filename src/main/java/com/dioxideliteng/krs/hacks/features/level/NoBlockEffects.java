package com.dioxideliteng.krs.hacks.features.level;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.input.Keys;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Blocks;

public class NoBlockEffects extends Module {

    public NoBlockEffects() {
        super("No Block Effects", ModuleCategory.Level, Keys.KEY_UNKNOWN, false, true);
    }

    public static boolean shouldIgnore(Object entity) {
        return entity instanceof LocalPlayer && ModuleManager.getModuleState(NoBlockEffects.class);
    }

    public static float normalFriction() {
        return Blocks.STONE.getFriction();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
