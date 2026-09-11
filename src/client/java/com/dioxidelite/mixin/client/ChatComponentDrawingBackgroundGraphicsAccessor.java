package com.dioxidelite.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public interface ChatComponentDrawingBackgroundGraphicsAccessor {
    @Accessor("graphics")
    GuiGraphics dioxide_lite$getGraphics();
}
