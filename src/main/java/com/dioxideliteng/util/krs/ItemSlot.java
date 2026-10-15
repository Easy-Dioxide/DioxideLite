package com.dioxideliteng.util.krs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;

/** Ported from Krs (com.instrumentalist.krs.utils.entity.ItemSlot). */
public abstract class ItemSlot {
    public static final Minecraft mc = Minecraft.getInstance();

    public abstract ItemStack getItemStack();

    public abstract Integer getIdForServer(ContainerScreen screen);

    public Integer getIdForServerWithCurrentScreen() {
        return getIdForServer(mc.gui.screen() instanceof ContainerScreen screen ? screen : null);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object other);
}
