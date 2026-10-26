package com.dioxideliteng.krs.utils.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemSlotUtil {
    private ItemSlotUtil() {
    }

    public static boolean isBundle(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == Items.BUNDLE || Items.DYED_BUNDLE.asList().contains(item);
    }
}
