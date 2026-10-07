package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.CombatVisuals;
import com.dioxidelite.module.modules.render.advanced.Hand;
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

    /** 第一人称手臂定制：在主手基准变换之后套用模块的位置/旋转/缩放。 */
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 0, shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void dioxide$handTransform(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                       float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                       SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        Hand handModule = Hand.INSTANCE;
        if (handModule.isEnabled() && player == Minecraft.getInstance().player
                && hand == InteractionHand.MAIN_HAND) {
            handModule.applyMainHandTransform(poseStack);
        }
    }

    /**
     * 自定义挥动曲线：当 Hand 模块启用且手持剑时，在 vanilla 攻击动画基础上叠加
     * 模块定义的 swingCurve(swingProgress()) 偏移。
     */
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 0, shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void dioxide$customSwing(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                                     float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                                     SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        Hand handModule = Hand.INSTANCE;
        if (!handModule.isEnabled() || player != Minecraft.getInstance().player) return;
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!handModule.shouldOverrideSword()) return;
        float offset = handModule.swingOffset();
        if (offset <= 0.001F) return;
        HumanoidArm arm = player.getMainArm();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.mulPose(Axis.YP.rotationDegrees(side * offset * 12.0F));
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

    /**
     * [DioxideLite 修复] 让 Combat Visuals 的 "Block Offset" 设置真正生效。
     *
     * <p>修复前该设置只被声明、全工程没有任何读取方，是空壳。语义按名称与取值区间
     * 推断：默认值 1.0 表示与原版一致，小于 1.0 把手部姿态往回收，大于 1.0 往外推。
     * 位移量取 {@code (值 - 1.0) * 0.25} 方块，只对第一人称、手持剑、正在格挡时应用。</p>
     *
     * <p><b>这是一处行为新增而非等价重构</b>，语义为我依据设置名与范围所做的推断。
     * 若与你原本的设计不符，删除本方法即可恢复原行为（设置会重新变成空壳）。</p>
     */
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 0, shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void dioxidelite$blockOffset(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CallbackInfo ci) {
        CombatVisuals visuals = CombatVisuals.INSTANCE;
        if (!visuals.isEnabled() || !visuals.blockAnimation.get()) return;
        if (player != Minecraft.getInstance().player || !itemStack.is(ItemTags.SWORDS) || !player.isUsingItem()) return;
        float offset = visuals.blockOffset.get().floatValue() - 1.0F;
        if (Math.abs(offset) <= 0.001F) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(side * offset * 0.25F, -offset * 0.15F, -offset * 0.25F);
    }
}
