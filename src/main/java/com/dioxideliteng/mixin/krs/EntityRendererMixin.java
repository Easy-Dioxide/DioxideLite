package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.render.NameTags;
import com.dioxideliteng.krs.oringo.IEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void krs$hideVanillaNameTag(T entity, S state, float tickDelta, CallbackInfo ci) {
        ((IEntityRenderState) state).client$setEntity(entity);

        if (ModuleManager.getModuleState(NameTags.class) && NameTags.shouldRender(entity)) {
            state.nameTag = null;
            state.scoreText = null;
        }
    }

    @Inject(at = @At("HEAD"), method = "submit", cancellable = true)
    public void render(S state, PoseStack matrices, SubmitNodeCollector submitter, CameraRenderState cameraState, CallbackInfo ci) {
        if (state.leashStates != null && !state.leashStates.isEmpty()) {
            ci.cancel();
            return;
        }

    }
}
