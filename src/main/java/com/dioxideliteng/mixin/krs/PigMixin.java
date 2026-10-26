package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.movement.EntityControl;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pig.class)
public abstract class PigMixin {

    @ModifyReturnValue(method = "getControllingPassenger", at = @At("RETURN"))
    private LivingEntity entityControlPigPassengerHook(LivingEntity original) {
        return EntityControl.hookControllingPassenger(original, (Pig) (Object) this);
    }
}
