package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.movement.EntityControl;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @ModifyReturnValue(method = "isTamed", at = @At("RETURN"))
    private boolean entityControlTamedHook(boolean original) {
        return original || EntityControl.shouldTreatAsTamed((AbstractHorse) (Object) this);
    }
}
