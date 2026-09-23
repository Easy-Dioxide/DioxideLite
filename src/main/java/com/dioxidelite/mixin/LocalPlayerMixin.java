package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/LocalPlayerMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.MoveEvent;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.event.events.SendPositionEvent;
import com.dioxidelite.event.events.SlowdownEvent;
import com.dioxidelite.module.modules.combat.KillAura;
import com.dioxidelite.module.modules.movement.MovementFix;
import com.dioxidelite.module.modules.movement.NoSlow;
import com.dioxidelite.module.modules.movement.Scaffold;
import com.dioxidelite.module.modules.movement.Velocity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link PlayerTickEvent} and {@link MoveEvent} from the local player's
 * simulation, and {@link SendPositionEvent} around {@code sendPosition} — the
 * latter lets modules rewrite the position/rotation reported to the server
 * (silent aim) without turning the client view. A cancelled {@link MoveEvent}
 * re-issues the move with the handler-supplied vector.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    @Unique
    private SendPositionEvent dioxidelite$sendPositionEvent;

    protected LocalPlayerMixin(ClientLevel level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.BEFORE, ordinal = 0),
            cancellable = true)
    private void dioxidelite$preTick(CallbackInfo ci) {
        PlayerTickEvent.Pre event = EventBus.INSTANCE.post(new PlayerTickEvent.Pre());
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.AFTER, ordinal = 0))
    private void dioxidelite$postTick(CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerTickEvent.Post());
    }

    @Inject(
            method = "move",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            cancellable = true)
    private void dioxidelite$onMove(MoverType moverType, Vec3 delta, CallbackInfo ci) {
        MoveEvent event = EventBus.INSTANCE.post(new MoveEvent(delta.x, delta.y, delta.z));
        if (event.isCancelled()) {
            super.move(moverType, new Vec3(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void dioxidelite$onPreSendPosition(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        dioxidelite$sendPositionEvent = EventBus.INSTANCE.post(new SendPositionEvent(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.onGround()));
        if (dioxidelite$sendPositionEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 dioxidelite$redirectPosition(LocalPlayer instance, Operation<Vec3> original) {
        return new Vec3(dioxidelite$sendPositionEvent.getX(), dioxidelite$sendPositionEvent.getY(), dioxidelite$sendPositionEvent.getZ());
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double dioxidelite$redirectGetX(LocalPlayer instance, Operation<Double> original) {
        return dioxidelite$sendPositionEvent.getX();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double dioxidelite$redirectGetY(LocalPlayer instance, Operation<Double> original) {
        return dioxidelite$sendPositionEvent.getY();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double dioxidelite$redirectGetZ(LocalPlayer instance, Operation<Double> original) {
        return dioxidelite$sendPositionEvent.getZ();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float dioxidelite$redirectGetYRot(LocalPlayer instance, Operation<Float> original) {
        return dioxidelite$sendPositionEvent.getYaw();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float dioxidelite$redirectGetXRot(LocalPlayer instance, Operation<Float> original) {
        return dioxidelite$sendPositionEvent.getPitch();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean dioxidelite$redirectOnGround(LocalPlayer instance, Operation<Boolean> original) {
        return dioxidelite$sendPositionEvent.isOnGround();
    }

    @Inject(method = "canStartSprinting", at = @At("RETURN"), cancellable = true)
    private void dioxidelite$hookMoveFixCanStartSprinting(CallbackInfoReturnable<Boolean> cir) {
        if (MovementFix.INSTANCE.shouldPreventLocalSprint()
                || Scaffold.INSTANCE.shouldSuppressSprint()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(
            method = "canStartSprinting",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
    private boolean dioxidelite$scaffoldOmnidirectionalSprint(boolean original) {
        if (!Scaffold.INSTANCE.shouldSprintOmnidirectionally()) {
            return original;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        ClientInput input = player.input;
        float forward = input.getMoveVector().y;
        float sideways = input.getMoveVector().x;
        boolean moving = Math.abs(forward) > 1.0E-5F || Math.abs(sideways) > 1.0E-5F;
        boolean walking = Math.abs(forward) >= 0.8F || Math.abs(sideways) >= 0.8F;
        return player.isUnderWater() ? moving : walking;
    }

    @ModifyReturnValue(method = "itemUseSpeedMultiplier", at = @At("RETURN"))
    private float dioxidelite$onItemUseSlowdown(float original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        SlowdownEvent event = EventBus.INSTANCE.post(new SlowdownEvent(
                player,
                player.getUseItem(),
                original
        ));
        return event.isCancelled() ? 1.0F : event.getMultiplier();
    }

    @ModifyReturnValue(method = "isSlowDueToUsingItem", at = @At("RETURN"))
    private boolean dioxidelite$allowNoSlowSprint(boolean original) {
        return original
                && !NoSlow.INSTANCE.shouldAllowSprinting()
                && !KillAura.INSTANCE.shouldBypassBasicBlockSlowdown();
    }

    @WrapOperation(
            method = "sendIsSprintingIfNeeded",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"))
    private boolean dioxidelite$hookMoveFixSprintPacket(LocalPlayer instance, Operation<Boolean> original) {
        boolean sprinting;
        if (MovementFix.INSTANCE.shouldSuppressSprintPacket()) {
            sprinting = false;
        } else {
            sprinting = original.call(instance);
        }
        return Scaffold.INSTANCE.modifyServerSprint(sprinting);
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void dioxidelite$cancelBlockPush(double x, double z, CallbackInfo ci) {
        if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.blockPush.get()) {
            ci.cancel();
        }
    }
}
