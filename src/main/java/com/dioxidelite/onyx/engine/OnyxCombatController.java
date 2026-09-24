package com.dioxidelite.onyx.engine;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Current-version state controller corresponding to OpenOnyx's CombatController.
 * It deliberately owns state only; packet transport and rendering remain in
 * DioxideLite's existing event/rotation/visual layers.
 */
public final class OnyxCombatController {
    private final Minecraft mc;
    private LivingEntity target;
    private long lastActionNanos;
    private boolean active;

    public OnyxCombatController(Minecraft mc) {
        this.mc = mc;
    }

    public void begin(LivingEntity target) {
        if (target == null || mc.player == null || !target.isAlive()) {
            reset();
            return;
        }
        this.target = target;
        this.active = true;
    }

    public void markAction() {
        lastActionNanos = System.nanoTime();
    }

    public boolean isActive() { return active && target != null && target.isAlive(); }
    public LivingEntity target() { return target; }
    public long lastActionNanos() { return lastActionNanos; }

    public void reset() {
        active = false;
        target = null;
        lastActionNanos = 0L;
    }
}
