package com.dioxidelite.module.modules.movement;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.core.engine.MovementEngine;

/** the reference client-compatible AutoSprint port. */
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();
    private final BooleanSetting onlyForward = add(new BooleanSetting("Only Forward", true));
    private final MovementEngine reference = new MovementEngine(mc);
    private AutoSprint() { super("Auto Sprint", Category.MOVEMENT); }
    @Listen private void onTick(PlayerTickEvent.Pre event) {
        if (noPlayer()) return;
        if (reference.shouldSprint(onlyForward.get())) mc.options.keySprint.setDown(true);
    }
    @Override protected void onDisable() { mc.options.keySprint.setDown(false); }
}
