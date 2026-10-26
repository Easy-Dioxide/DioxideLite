package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.render.AntiBlind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class InGameOverlayRendererMixin {

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void antiBlindHook(PoseStack poseStack, SubmitNodeCollector submitter, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ModuleManager.getModuleState(AntiBlind.class) && AntiBlind.fire.get())
            ci.cancel();
    }
}
