package com.dioxideliteng.util.krs;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.Slots). */
public final class Slots {
    private Slots() {
    }

    public static SlotGroup<HotbarItemSlot> hotbar() {
        ArrayList<HotbarItemSlot> hotbar = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) hotbar.add(new HotbarItemSlot(i));
        return new SlotGroup<>(hotbar);
    }

    public static SlotGroup<InventoryItemSlot> container() {
        ArrayList<InventoryItemSlot> container = new ArrayList<>(27);
        for (int i = 0; i < 27; i++) container.add(new InventoryItemSlot(i));
        return new SlotGroup<>(container);
    }

    public static SlotGroup<OffHandSlot> offHand() {
        return new SlotGroup<>(List.of(new OffHandSlot()));
    }

    public static SlotGroup<ArmorItemSlot> armor() {
        ArrayList<ArmorItemSlot> armor = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) armor.add(new ArmorItemSlot(i));
        return new SlotGroup<>(armor);
    }

    public static SlotGroup<HotbarItemSlot> offhandWithHotbar() {
        SlotGroup<HotbarItemSlot> hotbar = hotbar();
        ArrayList<HotbarItemSlot> offhandWithHotbar = new ArrayList<>(1 + hotbar.size());
        offhandWithHotbar.add(new OffHandSlot());
        offhandWithHotbar.addAll(hotbar);
        return new SlotGroup<>(offhandWithHotbar);
    }

    public static SlotGroup<ItemSlot> all() {
        return hotbar().plus(offHand()).plus(container()).plus(armor());
    }

    public static <T extends HotbarItemSlot> T findClosestSlot(SlotGroup<T> slotGroup, Item item) {
        return findClosestSlot(slotGroup, stack -> stack.getItem() == item);
    }

    public static <T extends HotbarItemSlot> T findClosestSlot(SlotGroup<T> slotGroup, Item... items) {
        return findClosestSlot(slotGroup, stack -> containsItem(items, stack.getItem()));
    }

    private static boolean containsItem(Item[] items, Item item) {
        for (Item candidate : items) {
            if (candidate == item)
                return true;
        }
        return false;
    }

    public static <T extends HotbarItemSlot> T findClosestSlot(SlotGroup<T> slotGroup, Predicate<ItemStack> predicate) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;

        int selected = player.getInventory().getSelectedSlot();
        T bestSlot = null;
        int bestDistance = Integer.MAX_VALUE;

        for (T slot : slotGroup) {
            if (!predicate.test(slot.getItemStack()))
                continue;

            int distance;
            if (slot instanceof OffHandSlot)
                distance = Integer.MIN_VALUE + 1;
            else if (slot.getHotbarSlotForServer() == selected)
                distance = Integer.MIN_VALUE;
            else
                distance = Math.abs(selected - slot.getHotbarSlotForServer());

            if (distance < bestDistance) {
                bestDistance = distance;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    public static boolean hasItem(SlotGroup<?> slotGroup, Item item) {
        for (ItemSlot slot : slotGroup) {
            if (slot.getItemStack().getItem() == item)
                return true;
        }
        return false;
    }
}
