package com.dioxideliteng.krs.hacks.features.movement;



import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.input.Keys;

public class NoJumpCooldown extends Module {

    public NoJumpCooldown() {
        super("No Jump Cooldown", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || !mc.player.onGround()) return;

        mc.player.noJumpDelay = 0;
    }
}
