package com.dioxidelite.mixin;

import com.dioxidelite.accessor.EntityRenderStateAccessor;
import com.dioxidelite.module.modules.render.Chams;
import com.dioxidelite.module.modules.render.ESP;
import com.dioxidelite.module.modules.render.NameTags;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Visual render hooks for Chams, ESP outlines and custom Name Tags. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Shadow public abstract Identifier getTextureLocation(S state);

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType dioxide$modifyRenderType(RenderType original, S state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        Chams chams = Chams.INSTANCE;
        if (chams.isEnabled() && chams.noDepth.get()
                && ((EntityRenderStateAccessor) state).dioxidelite$getEntity() instanceof Player player
                && player != Minecraft.getInstance().player) {
            return chams.getRenderType(getTextureLocation(state));
        }
        return original;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void dioxide$extract(T entity, S state, float partialTicks, CallbackInfo ci) {
        Chams chams = Chams.INSTANCE;
        if (chams.isEnabled() && chams.shouldRenderGlow(entity)) {
            state.outlineColor = chams.getGlowColor(entity);
        } else if (entity instanceof Player player && ESP.INSTANCE.shouldRenderOutline(entity)) {
            state.outlineColor = ESP.INSTANCE.getPlayerColor(player);
        }
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void dioxide$hideVanillaName(T entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && NameTags.INSTANCE.isEnabled() && !NameTags.INSTANCE.vanillaNameTags.get()) {
            cir.setReturnValue(false);
        }
    }
}
