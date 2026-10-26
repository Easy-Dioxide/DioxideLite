package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.movement.EntityControl;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class MobMixin {

    @ModifyReturnValue(method = "isSaddled", at = @At("RETURN"))
    private boolean entityControlSaddleHook(boolean original) {
        return original || EntityControl.shouldTreatAsSaddled((Mob) (Object) this);
    }
}
