package com.dioxidelite.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("getJumpPower")
    float DioxideLite$invokeGetJumpPower();

    @Accessor("noJumpDelay")
    int DioxideLite$getNoJumpDelay();
}
