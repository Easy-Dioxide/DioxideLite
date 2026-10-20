package com.dioxideliteng.module.misc;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.NoBreakCooldown).
 * Krs parity: the module body is empty; the whole bypass lives in the
 * ClientPlayerInteractionManagerMixin redirect of the first destroyDelay read in
 * continueDestroyBlock. The DioxideLite equivalent redirect is wired in
 * mixins/MixinMultiPlayerGameMode#dioxideliteng$noBreakCooldown, which calls
 * NoBreakCooldown2.isActive() instead of Krs ModuleManager.getModuleState.
 */
public class NoBreakCooldown2 extends Feature {
    private static volatile boolean active;

    public NoBreakCooldown2() {
        super("NoBreakCooldown2", Category.MISC);
    }

    public static boolean isActive() {
        return active;
    }

    @Override
    public void onEnable() {
        active = true;
    }

    @Override
    public void onDisable() {
        active = false;
    }
}
