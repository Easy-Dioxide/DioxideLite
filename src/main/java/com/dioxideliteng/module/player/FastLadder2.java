package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.TimerUtil;

import java.util.Locale;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.FastLadder).
 *
 * Adaptations:
 *  - ListValue/FloatValue/BooleanValue mapped to ModeSetting/NumberSetting/BooleanSetting;
 *  - the Krs value visibility predicates mapped to Setting#setVisible;
 *  - Krs onUpdate -> Events.UPDATE.
 */
public class FastLadder2 extends Feature {
    private final ModeSetting mode = new ModeSetting("Mode", this, "Vanilla", new String[]{"Vanilla", "Timer"});
    private final BooleanSetting resetY = new BooleanSetting("ResetY", this, true);
    private final NumberSetting speed = new NumberSetting("Speed", this, 0.4, 0.2, 1.0, 0.1);
    private final NumberSetting timerSpeed = new NumberSetting("Timer Speed", this, 2.0, 1.1, 3.0, 0.1);

    private boolean wasClimbing = false;

    public FastLadder2() {
        super("FastLadder2", Category.PLAYER);
        this.resetY.setVisible(() -> mode.is("Vanilla"));
        this.speed.setVisible(() -> mode.is("Vanilla"));
        this.timerSpeed.setVisible(() -> mode.is("Timer"));
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        wasClimbing = false;
        TimerUtil.reset();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.UPDATE) return;

        var player = mc.player;
        if (player == null) return;

        boolean climbing = player.onClimbable() && (player.horizontalCollision || mc.options.keyJump.isDown());
        switch (mode.getValue().toLowerCase(Locale.ROOT)) {
            case "vanilla" -> {
                if (climbing) {
                    MovementUtil.setVelocityY(speed.getValue());
                    wasClimbing = true;
                } else if (wasClimbing) {
                    if (resetY.getValue())
                        MovementUtil.setVelocityY(-0.1);
                    wasClimbing = false;
                }
            }
            case "timer" -> {
                if (climbing) {
                    TimerUtil.timerSpeed = (float) timerSpeed.getValue();
                    wasClimbing = true;
                } else if (wasClimbing) {
                    TimerUtil.reset();
                    wasClimbing = false;
                }
            }
        }
    }
}
