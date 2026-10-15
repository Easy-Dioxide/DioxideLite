package com.dioxideliteng.util.krs;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.OffHandSlot). */
public final class OffHandSlot extends HotbarItemSlot {
    OffHandSlot() {
        super(-1, 40, InteractionHand.OFF_HAND);
    }

    @Override
    public ItemStack getItemStack() {
        return mc.player != null ? mc.player.getOffhandItem() : ItemStack.EMPTY;
    }

    @Override
    public Integer getIdForServer(ContainerScreen screen) {
        return screen == null ? 45 : null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OffHandSlot;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
