package com.dioxideliteng.krs.utils.entity;

import com.dioxideliteng.krs.utils.IMinecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;

public abstract class ItemSlot implements IMinecraft {
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
