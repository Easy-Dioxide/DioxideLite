package com.dioxidelite.module.modules.movement;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.MoveEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.core.engine.MovementEngine;

/** Conservative edge protection matching the reference client SafeWalk semantics. */
public final class SafeWalk extends Module {
    public static final SafeWalk INSTANCE = new SafeWalk();
    private final BooleanSetting onlyGround = add(new BooleanSetting("Only On Ground", true));
    private final MovementEngine reference = new MovementEngine(mc);
    private SafeWalk() { super("Safe Walk", Category.MOVEMENT); }
    @Listen private void onMove(MoveEvent event) {
        if (noPlayer()) return;
        double x = event.getX(), z = event.getZ();
        if (!reference.isSafeMove(x, z, onlyGround.get())) {
            event.setX(0.0D);
            event.setZ(0.0D);
        }
    }
}
