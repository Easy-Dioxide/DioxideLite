package com.dioxideliteng.mixin.krs;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerSprintAccessor {
    @Invoker("isSprintingPossible")
    boolean krs$invokeIsSprintingPossible(boolean isFlying);

    @Invoker("isSlowDueToUsingItem")
    boolean krs$invokeIsSlowDueToUsingItem();
}
