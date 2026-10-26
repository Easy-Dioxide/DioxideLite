package com.dioxideliteng.krs.hacks.features.movement;

import com.dioxideliteng.krs.events.features.BlockEdgeEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.move.MovementUtil;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.input.Keys;

public class SafeWalk extends Module {
    @Setting
    private final BooleanValue onlyMoving = new BooleanValue("Only Moving", false);

    @Setting
    private final BooleanValue ignoreSneak = new BooleanValue("Ignore Sneak", false);

    public SafeWalk() {
        super("Safe Walk", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onBlockEdge(BlockEdgeEvent event) {
        var player = mc.player;
        if (player == null || player.isFallFlying() || player.getAbilities().flying)
            return;

        if (onlyMoving.get() && !MovementUtil.isMoving())
            return;

        if (!ignoreSneak.get() && player.isShiftKeyDown())
            return;

        event.cancel();
    }
}
