package com.dioxideliteng.util.krs;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.SlotGroup). */
public class SlotGroup<T extends ItemSlot> extends AbstractList<T> {
    public final List<T> slots;

    public SlotGroup(List<T> slots) {
        this.slots = slots;
    }

    public List<Item> getItems() {
        ArrayList<Item> result = new ArrayList<>(slots.size());
        for (T slot : slots)
            result.add(slot.getItemStack().getItem());
        return result;
    }

    public T findSlot(Item item) {
        return findSlot(stack -> stack.getItem() == item);
    }

    public T findSlot(Predicate<ItemStack> predicate) {
        if (Minecraft.getInstance().player == null) return null;
        for (T slot : this) {
            if (predicate.test(slot.getItemStack()))
                return slot;
        }
        return null;
    }

    public SlotGroup<ItemSlot> plus(SlotGroup<?> other) {
        ArrayList<ItemSlot> newList = new ArrayList<>(size() + other.size());
        newList.addAll(this);
        newList.addAll(other);
        return new SlotGroup<>(newList);
    }

    public SlotGroup<ItemSlot> plus(ItemSlot other) {
        ArrayList<ItemSlot> newList = new ArrayList<>(size() + 1);
        newList.addAll(this);
        newList.add(other);
        return new SlotGroup<>(newList);
    }

    @Override
    public T get(int index) {
        return slots.get(index);
    }

    @Override
    public int size() {
        return slots.size();
    }
}
