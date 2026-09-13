package com.dioxidelite.module.modules.movement;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;

/** Vanilla-compatible automatic sprint toggle for the Movement category. */
public final class Sprint extends Module {

    public static final Sprint INSTANCE = new Sprint();

    private Sprint() {
        super("Sprint", Category.MOVEMENT);
    }

    @Override
    protected void onDisable() {
        if (mc.options.keySprint.isDown()) {
            mc.options.keySprint.setDown(false);
        }
    }

    @Listen
    private void onTick(PlayerTickEvent.Pre event) {
        if (!noPlayer()) {
            mc.options.keySprint.setDown(true);
        }
    }
}
