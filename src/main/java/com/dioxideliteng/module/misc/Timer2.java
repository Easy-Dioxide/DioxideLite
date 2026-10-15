package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.TimerUtil;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.Timer).
 * Krs parity: "Vanilla" mode drives TimerUtil.timerSpeed from the speed setting
 * (resetting when Move Only is on and the player stands still), "Rounding Error"
 * mode pins the speed to 1.0075. On disable the timer speed is always reset.
 * Named Timer2 because DioxideLite already ships a native movement/Timer built on
 * TimerController, which is a different mechanism than the Krs TimerUtil.timerSpeed
 * hook this port targets.
 */
public class Timer2 extends Feature {
    private final ModeSetting mode = new ModeSetting("Mode", this, "Vanilla",
            new String[]{"Vanilla", "Rounding Error"});
    private final NumberSetting speed = new NumberSetting("Speed", this, 1.5, 0.1, 10.0, 0.1);
    private final BooleanSetting moveOnly = new BooleanSetting("Move Only", this, false);

    public Timer2() {
        super("Timer2", Category.MISC);
        this.speed.setVisible(() -> this.mode.is("Vanilla"));
        this.moveOnly.setVisible(() -> this.mode.is("Vanilla"));
    }

    @Override
    public void onDisable() {
        TimerUtil.reset();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.TICK)
            return;

        if (this.mode.is("Vanilla")) {
            if (this.moveOnly.getValue() && !MovementUtil.isMoving())
                TimerUtil.reset();
            else
                TimerUtil.timerSpeed = (float) this.speed.getValue();
        }

        if (this.mode.is("Rounding Error")) {
            TimerUtil.timerSpeed = 1.0075F;
        }
    }
}
