package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.features.render.ESP;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void krs$beginShaderEspHeadEquipmentCapture(PoseStack poseStack, SubmitNodeCollector submitter, int light, LivingEntityRenderState state, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        ESP.beginEquipmentCapture(state);
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("RETURN")
    )
    private void krs$endShaderEspHeadEquipmentCapture(PoseStack poseStack, SubmitNodeCollector submitter, int light, LivingEntityRenderState state, float limbSwing, float limbSwingAmount, CallbackInfo ci) {
        ESP.endEquipmentCapture();
    }
}
