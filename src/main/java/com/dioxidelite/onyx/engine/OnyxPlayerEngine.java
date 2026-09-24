package com.dioxidelite.onyx.engine;

import net.minecraft.world.item.ItemStack;

/** Player-side utility backend. Keeps inventory/name helpers independent of UI modules. */
public final class OnyxPlayerEngine {
    public boolean hasItem(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    public String applyDisplayName(String original, boolean enabled, boolean nametag, String replacement) {
        if (!enabled || !nametag || replacement == null || replacement.isBlank()) return original;
        return replacement;
    }
}
