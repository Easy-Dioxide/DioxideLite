package com.dioxidelite.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.dioxidelite.Config;
import com.dioxidelite.client.util.ItemPhysicsRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Shadow private ItemModelResolver itemModelResolver;
    @Unique private ItemEntityRenderState dioxide_lite$currentItemState;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void dioxide_lite$extractItemPhysics(ItemEntity itemEntity, ItemEntityRenderState state, float tickProgress, CallbackInfo ci) {
        boolean blockItem = itemEntity.getItem().getItem() instanceof BlockItem;
        if (Config.item2DRender && !blockItem) {
            this.itemModelResolver.updateForNonLiving(state.item, itemEntity.getItem(), ItemDisplayContext.GUI, itemEntity);
        }
        Vec3 delta = itemEntity.getDeltaMovement();
        boolean moving = delta.horizontalDistanceSqr() > 2.5E-3 || Math.abs(delta.y) > 0.04;
        ((ItemPhysicsRenderState) state).dioxide_lite$setItemPhysics(itemEntity.onGround(), moving, blockItem, itemEntity.getUUID().hashCode());
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void dioxide_lite$beginItemPhysics(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        dioxide_lite$currentItemState = state;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
    private void dioxide_lite$endItemPhysics(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        dioxide_lite$currentItemState = null;
    }

    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getSpin(FF)F")
    )
    private float dioxide_lite$replaceItemSpin(float ageInTicks, float bobOffset) {
        if (Config.item2DRender && dioxide_lite$currentItemState != null && !((ItemPhysicsRenderState) dioxide_lite$currentItemState).dioxide_lite$itemPhysicsBlockItem()) {
            return 0.0f;
        }
        if (!Config.itemPhysics || dioxide_lite$currentItemState == null) {
            return ItemEntity.getSpin(ageInTicks, bobOffset);
        }

        ItemPhysicsRenderState state = (ItemPhysicsRenderState) dioxide_lite$currentItemState;
        if (state.dioxide_lite$itemPhysicsOnGround()) {
            return stableAngleRad(state.dioxide_lite$itemPhysicsSeed(), 0);
        }
        return ageInTicks * 0.32f * Config.itemPhysicsRotationSpeed + stableAngleRad(state.dioxide_lite$itemPhysicsSeed(), 1);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;submitMultipleFromCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/phys/AABB;)V")
    )
    private void dioxide_lite$applyItemPhysicsPose(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (Config.item2DRender && !((ItemPhysicsRenderState) state).dioxide_lite$itemPhysicsBlockItem()) {
            poseStack.mulPose(cameraRenderState.orientation);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.scale(0.55f, 0.55f, 0.55f);
            return;
        }
        if (!Config.itemPhysics) {
            return;
        }

        ItemPhysicsRenderState physics = (ItemPhysicsRenderState) state;
        if (physics.dioxide_lite$itemPhysicsOnGround()) {
            float bob = (float) Math.sin(state.ageInTicks / 10.0f + state.bobOffset) * 0.1f + 0.1f;
            AABB box = state.item.getModelBoundingBox();
            float groundOffset = box.getZsize() > 0.0625 ? 0.0125f : -0.1200f;
            poseStack.translate(0.0f, -bob + groundOffset, 0.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(stableAngleDeg(physics.dioxide_lite$itemPhysicsSeed(), 2) * 0.08f - 14.4f));
        } else {
            float age = state.ageInTicks;
            float speed = Config.itemPhysicsRotationSpeed;
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 9.5f * speed + stableAngleDeg(physics.dioxide_lite$itemPhysicsSeed(), 3)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 6.0f * speed + stableAngleDeg(physics.dioxide_lite$itemPhysicsSeed(), 4)));
        }
    }

    @Unique
    private float stableAngleRad(int seed, int salt) {
        return (float) Math.toRadians(stableAngleDeg(seed, salt));
    }

    @Unique
    private float stableAngleDeg(int seed, int salt) {
        int mixed = seed ^ (salt * 0x9E3779B9);
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        return (mixed & 0xFFFF) / 65535.0f * 360.0f;
    }
}
