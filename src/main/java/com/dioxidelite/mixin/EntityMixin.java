package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/EntityMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.RaytraceEvent;
import com.dioxidelite.event.events.StrafeEvent;
import com.dioxidelite.module.modules.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @WrapOperation(
            method = "getViewVector",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 dioxidelite$hookViewVector(Entity instance, float pitch, float yaw, Operation<Vec3> original) {
        if (instance != Minecraft.getInstance().player) {
            return original.call(instance, pitch, yaw);
        }

        RaytraceEvent event = EventBus.INSTANCE.post(new RaytraceEvent(yaw, pitch));
        return original.call(instance, event.getPitch(), event.getYaw());
    }

    @WrapOperation(
            method = "moveRelative",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float dioxidelite$hookMoveRelativeYaw(Entity instance, Operation<Float> original) {
        if (instance == Minecraft.getInstance().player) {
            StrafeEvent event = EventBus.INSTANCE.post(new StrafeEvent(instance.getYRot()));
            return event.getYaw();
        }
        return original.call(instance);
    }

    @ModifyArgs(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void dioxidelite$cancelEntityPush(Args args) {
        if ((Object) this == Minecraft.getInstance().player
                && Velocity.INSTANCE.isEnabled()
                && Velocity.INSTANCE.entityPush.get()) {
            args.set(0, 0.0);
            args.set(1, 0.0);
            args.set(2, 0.0);
        }
    }
}
