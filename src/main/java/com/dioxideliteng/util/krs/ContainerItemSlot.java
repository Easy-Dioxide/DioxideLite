package com.dioxideliteng.util.krs;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.ContainerItemSlot). */
public class ContainerItemSlot extends ItemSlot {
    public final int slotInContainer;

    public ContainerItemSlot(int slotInContainer) {
        this.slotInContainer = slotInContainer;
    }

    private ContainerScreen getScreen() {
        return (ContainerScreen) mc.gui.screen();
    }

    @Override
    public ItemStack getItemStack() {
        return getScreen().getMenu().slots.get(slotInContainer).getItem();
    }

    @Override
    public Integer getIdForServer(ContainerScreen screen) {
        return slotInContainer;
    }

    public int distance(ContainerItemSlot itemSlot) {
        int slotId = slotInContainer;
        int otherId = itemSlot.slotInContainer;
        int rowA = slotId / 9;
        int colA = slotId % 9;
        int rowB = otherId / 9;
        int colB = otherId % 9;
        return (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ContainerItemSlot that = (ContainerItemSlot) other;
        return slotInContainer == that.slotInContainer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), slotInContainer);
    }
}
