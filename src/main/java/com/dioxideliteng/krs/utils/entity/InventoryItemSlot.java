package com.dioxideliteng.krs.utils.entity;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class InventoryItemSlot extends ItemSlot {
    private final int inventorySlot;

    public InventoryItemSlot(int inventorySlot) {
        this.inventorySlot = inventorySlot;
    }

    @Override
    public ItemStack getItemStack() {
        return mc.player != null ? mc.player.getInventory().getItem(9 + inventorySlot) : ItemStack.EMPTY;
    }

    @Override
    public Integer getIdForServer(ContainerScreen screen) {
        return screen == null ? 9 + inventorySlot : screen.getMenu().slots.size() - 36 + inventorySlot;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        InventoryItemSlot that = (InventoryItemSlot) other;
        return inventorySlot == that.inventorySlot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), inventorySlot);
    }
}
