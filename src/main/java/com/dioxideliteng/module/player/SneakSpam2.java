package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.SneakSpam).
 *
 * Adaptations:
 *  - Krs onMotion (MotionEvent, dispatched at sendPosition) -> Events.PRE_MOTION, the exact
 *    analog event in this client (same dispatch site, once per outgoing move packet).
 */
public class SneakSpam2 extends Feature {

    public SneakSpam2() {
        super("SneakSpam2", Category.PLAYER);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.PRE_MOTION) return;
        if (mc.player == null
                || InputConstants.isKeyDown(InputConstants.getKey(mc.options.keyShift.saveString()).getValue()))
            return;

        mc.options.keyShift.setDown(!mc.options.keyShift.isDown());
    }
}
