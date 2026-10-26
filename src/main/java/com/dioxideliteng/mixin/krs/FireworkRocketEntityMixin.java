package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.movement.MovementFix;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin implements IMinecraft {

    @Shadow
    private LivingEntity attachedToEntity;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 movementFixFireworkLookVector(Vec3 original) {
        if (attachedToEntity == mc.player && MovementFix.shouldFixMovement())
            return MovementFix.getMovementLookVector();

        return original;
    }
}
