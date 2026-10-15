package com.dioxideliteng.util.krs;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.HotbarItemSlot). */
public class HotbarItemSlot extends ItemSlot {
    public final int hotbarSlot;
    public final int hotbarSlotForServer;
    public final InteractionHand useHand;

    public HotbarItemSlot(int hotbarSlot) {
        this(hotbarSlot, hotbarSlot, InteractionHand.MAIN_HAND);
    }

    protected HotbarItemSlot(int hotbarSlot, int hotbarSlotForServer, InteractionHand useHand) {
        this.hotbarSlot = hotbarSlot;
        this.hotbarSlotForServer = hotbarSlotForServer;
        this.useHand = useHand;
    }

    @Override
    public ItemStack getItemStack() {
        return mc.player != null ? mc.player.getInventory().getItem(hotbarSlot) : ItemStack.EMPTY;
    }

    public ItemStack getItem() {
        return getItemStack();
    }

    public int getHotbarSlotForServer() {
        return hotbarSlotForServer;
    }

    public boolean isSelected() {
        return mc.player != null && hotbarSlotForServer == mc.player.getInventory().getSelectedSlot();
    }

    public InteractionHand getUseHand() {
        return useHand;
    }

    @Override
    public Integer getIdForServer(ContainerScreen screen) {
        return screen == null ? 36 + hotbarSlot : screen.getMenu().slots.size() - 36 + 27 + hotbarSlot;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        HotbarItemSlot that = (HotbarItemSlot) other;
        return hotbarSlot == that.hotbarSlot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), hotbarSlot);
    }

    @Override
    public String toString() {
        return "HotbarItemSlot(hotbarSlot=" + hotbarSlot + ", itemStack=" + getItemStack() + ")";
    }
}
