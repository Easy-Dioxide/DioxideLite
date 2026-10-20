package com.dioxideliteng.util.krs;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.ItemSlotUtil). */
public final class ItemSlotUtil {
    private ItemSlotUtil() {
    }

    public static boolean isBundle(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == Items.BUNDLE || Items.DYED_BUNDLE.asList().contains(item);
    }
}
