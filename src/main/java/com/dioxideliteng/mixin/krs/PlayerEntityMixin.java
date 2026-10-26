package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.BlockEdgeEvent;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.features.combat.Reach;
import com.dioxideliteng.krs.hacks.features.exploit.ChestTP;
import com.dioxideliteng.krs.hacks.features.movement.MovementFix;
import com.dioxideliteng.krs.hacks.features.level.AlwaysRiptide;
import com.dioxideliteng.krs.hacks.features.movement.Sprint;
import com.dioxideliteng.krs.hacks.features.player.FastBreak;
import com.dioxideliteng.krs.utils.move.MovementUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements IMinecraft {

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void updateEvent(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            if (mc.player != null) {
                if (mc.player.onGround())
                    MovementUtil.fallTicks = 0;
                else MovementUtil.fallTicks++;
            }

            if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(UpdateEvent.class))
                return;

            UpdateEvent event = new UpdateEvent();
            KrsClient.eventManager.call(event);
            if (event.isCancelled())
                ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 movementFixSwimmingLookVector(Vec3 original) {
        if ((Object) this instanceof LocalPlayer && MovementFix.shouldFixMovement() && ((Player) (Object) this).isSwimming())
            return MovementFix.getMovementLookVector();

        return original;
    }

    @ModifyReturnValue(method = "isStayingOnGroundSurface", at = @At("RETURN"))
    private boolean safeWalkHook(boolean original) {
        if ((Object) this instanceof LocalPlayer) {
            if (KrsClient.eventManager == null || !KrsClient.eventManager.hasListeners(BlockEdgeEvent.class))
                return original;

            BlockEdgeEvent event = new BlockEdgeEvent();
            KrsClient.eventManager.call(event);
            return original || event.isCancelled();
        }

        return original;
    }

    @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 0))
    private boolean keepSprintHook1(Player instance, Vec3 vec3d) {
        return Sprint.keepSprintHook(instance);
    }

    @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V", ordinal = 0))
    private boolean keepSprintHook2(Player instance, boolean b) {
        return Sprint.keepSprintHook(instance);
    }

    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    public float fastBreakHook(float original) {
        return (Object) this instanceof LocalPlayer ? FastBreak.hookFastBreak(original) : original;
    }

    @ModifyReturnValue(method = "getDesiredPose", at = @At("RETURN"))
    private Pose riptidePoseHook(Pose original) {
        return AlwaysRiptide.hookDesiredPose(original, (Player) (Object) this);
    }

    @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
    public double blockReachHook(double original) {
        if ((Object) this instanceof LocalPlayer) {
            original = Reach.hookBlockReach(original);
            return ChestTP.hookBlockReach(original);
        }

        return original;
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    public double entityReachHook(double original) {
        if ((Object) this instanceof LocalPlayer)
            return Reach.hookEntityReach(original);

        return original;
    }
}
