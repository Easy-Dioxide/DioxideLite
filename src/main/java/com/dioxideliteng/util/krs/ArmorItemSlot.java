package com.dioxideliteng.util.krs;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.ArmorItemSlot). */
public class ArmorItemSlot extends ItemSlot {
    private final int armorType;

    public ArmorItemSlot(int armorType) {
        this.armorType = armorType;
    }

    @Override
    public ItemStack getItemStack() {
        if (mc.player == null) return ItemStack.EMPTY;
        EquipmentSlot slot = switch (armorType) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            default -> EquipmentSlot.HEAD;
        };
        return mc.player.getItemBySlot(slot);
    }

    @Override
    public Integer getIdForServer(ContainerScreen screen) {
        return screen == null ? 8 - armorType : null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ArmorItemSlot that = (ArmorItemSlot) other;
        return armorType == that.armorType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), armorType);
    }
}
