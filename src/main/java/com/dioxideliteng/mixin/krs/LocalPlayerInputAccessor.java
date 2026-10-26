package com.dioxideliteng.mixin.krs;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerInputAccessor {
    @Accessor("lastSentInput")
    void krs$setLastSentInput(Input input);
}
