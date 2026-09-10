package com.dioxidelite.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("minecraft")
    Minecraft dioxide_lite$getMinecraft();

    @Invoker("getWidth")
    int dioxide_lite$getChatWidth();
}
