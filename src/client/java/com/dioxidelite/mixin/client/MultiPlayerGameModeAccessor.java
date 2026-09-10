package com.dioxidelite.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    float dioxideLite$getDestroyProgress();

    @Accessor("destroyDelay")
    int dioxideLite$getDestroyDelay();

    @Accessor("isDestroying")
    boolean dioxideLite$isDestroying();

    @Accessor("destroyBlockPos")
    BlockPos dioxideLite$getDestroyBlockPos();
}
