package com.dioxideliteng.krs.hacks.features.player;



import com.dioxideliteng.krs.events.features.MotionEvent;
import com.dioxideliteng.krs.events.features.RenderHudEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.mojang.blaze3d.platform.InputConstants;
import com.dioxideliteng.krs.utils.input.Keys;

public class SneakSpam extends Module {

    public SneakSpam() {
        super("Sneak Spam", ModuleCategory.Player, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null || InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue())) return;

        mc.options.keyShift.setDown(!mc.options.keyShift.isDown());
    }
}
