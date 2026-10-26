package com.dioxideliteng.mixin.krs;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {

    @WrapWithCondition(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
            )
    )
    private boolean krs$playLightningSound(Level level, double x, double y, double z,
                                           SoundEvent sound, SoundSource source,
                                           float volume, float pitch, boolean distanceDelay) {
        return !((LightningBolt) (Object) this).isSilent();
    }
}
