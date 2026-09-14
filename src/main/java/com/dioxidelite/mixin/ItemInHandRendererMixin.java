package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.CombatVisuals;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies optional first-person sword swing/block presentation without changing combat logic. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;", ordinal = 0))
    private ItemUseAnimation dioxide$blockAnimation(ItemUseAnimation original, @Local(argsOnly = true, name = "player") AbstractClientPlayer player, @Local(argsOnly = true, name = "itemStack") ItemStack stack) {
        CombatVisuals visuals = CombatVisuals.INSTANCE;
        if (visuals.isEnabled() && visuals.blockAnimation.get() && player == Minecraft.getInstance().player && stack.is(ItemTags.SWORDS) && player.isUsingItem()) {
            return ItemUseAnimation.BLOCK;
        }
        return original;
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 0, shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void dioxide$swing(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        CombatVisuals visuals = CombatVisuals.INSTANCE;
        if (!visuals.isEnabled() || !visuals.swordSwing.get() || player != Minecraft.getInstance().player || !itemStack.is(ItemTags.SWORDS)) return;
        float strength = visuals.swingStrength.get().floatValue();
        if (strength <= 0.001F || attack <= 0.001F) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float a = attack * strength;
        float first = Mth.sin(a * a * (float)Math.PI);
        float second = Mth.sin(Mth.sqrt(a) * (float)Math.PI);
        poseStack.translate(side * -0.08F, 0.04F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (34.0F - first * 18.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * second * -16.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(second * -55.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -34.0F));
    }
}
