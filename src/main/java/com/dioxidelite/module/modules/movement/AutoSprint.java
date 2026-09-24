package com.dioxidelite.module.modules.movement;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.onyx.engine.OnyxMovementEngine;

/** OpenOnyx-compatible AutoSprint port. */
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();
    private final BooleanSetting onlyForward = add(new BooleanSetting("Only Forward", true));
    private final OnyxMovementEngine onyx = new OnyxMovementEngine(mc);
    private AutoSprint() { super("Auto Sprint", Category.MOVEMENT); }
    @Listen private void onTick(PlayerTickEvent.Pre event) {
        if (noPlayer()) return;
        if (onyx.shouldSprint(onlyForward.get())) mc.options.keySprint.setDown(true);
    }
    @Override protected void onDisable() { mc.options.keySprint.setDown(false); }
}
