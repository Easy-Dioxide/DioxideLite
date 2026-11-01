package mixins.krs;

import com.dioxideliteng.krs.hacks.features.level.AlwaysRiptide;
import com.dioxideliteng.krs.hacks.features.level.ItemDropChanger;
import com.dioxideliteng.krs.hacks.features.level.NoBlockEffects;
import com.dioxideliteng.krs.hacks.features.movement.ElytraFly;
import com.dioxideliteng.krs.hacks.features.movement.MovementFix;
import com.dioxideliteng.krs.hacks.features.movement.Step;
import com.dioxideliteng.krs.hacks.features.render.ViewModel;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * KRS living-entity hooks. The sprint-jump yaw (KRS rewrote {@code jumpFromGround}) is handled by DioxideLiteNG's
 * MixinLivingEntity through {@code KrsHooks.jumpYaw}; KRS's OldHitting use-state spoofing is not ported.
 */
@Mixin(LivingEntity.class)
public abstract class MixinKrsLivingEntity {

    @ModifyExpressionValue(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float krs$noBlockEffectsFriction(float original) {
        return NoBlockEffects.shouldIgnore(this) ? NoBlockEffects.normalFriction() : original;
    }

    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 krs$movementFixFallFlyingLook(Vec3 original) {
        if ((Object) this instanceof LocalPlayer && MovementFix.shouldFixMovement())
            return MovementFix.getMovementLookVector();

        return original;
    }

    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float krs$movementFixFallFlyingPitch(float original) {
        if ((Object) this instanceof LocalPlayer && MovementFix.shouldFixMovement())
            return MovementFix.getMovementPitch();

        return original;
    }

    @ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
    private Vec3 krs$elytraFlyMovement(Vec3 original) {
        return ElytraFly.hookFallFlyingMovement(original, (LivingEntity) (Object) this);
    }

    // Since 26.3 the swing duration is computed once when a swing starts, so this also drives the swing ticking.
    @ModifyReturnValue(method = "getModifiedSwingDuration", at = @At("RETURN"))
    private int krs$swingSpeed(int original) {
        return ViewModel.hookSwingSpeed(original, (LivingEntity) (Object) this);
    }

    @ModifyReturnValue(method = "isAutoSpinAttack", at = @At("RETURN"))
    private boolean krs$riptideSpinAttack(boolean original) {
        return original || AlwaysRiptide.shouldForceSpinAttack((LivingEntity) (Object) this);
    }

    @WrapOperation(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"))
    private boolean krs$dropItemSwing(LivingEntity instance, InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, Operation<Boolean> original) {
        if (!(instance instanceof LocalPlayer) || ItemDropChanger.hookDropItemSwing(hand))
            return original.call(instance, hand, animation, sendToSwingingEntity);

        return false;
    }

    @ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"))
    private float krs$stepHeight(float original) {
        return Step.hookStepHeight(original, (LivingEntity) (Object) this);
    }
}
