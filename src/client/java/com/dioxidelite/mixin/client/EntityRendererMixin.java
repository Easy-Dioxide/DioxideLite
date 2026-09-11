package com.dioxidelite.mixin.client;

import com.dioxidelite.client.util.NameTagPlayerFilterState;
import com.dioxidelite.client.util.NameTagPlayerFilterContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dioxide_lite$captureNameTagPlayerFilter(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        ((NameTagPlayerFilterState) state).dioxide_lite$setNameTagRealPlayer(isRealPlayer(entity));
    }

    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void dioxide_lite$beginNameTagPlayerFilter(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        NameTagPlayerFilterContext.setRealPlayer(((NameTagPlayerFilterState) state).dioxide_lite$isNameTagRealPlayer());
    }

    @Inject(method = "submitNameTag", at = @At("RETURN"))
    private void dioxide_lite$endNameTagPlayerFilter(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        NameTagPlayerFilterContext.clear();
    }

    private static boolean isRealPlayer(Entity entity) {
        if (!(entity instanceof Player)) return false;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return true;

        PlayerInfo info = connection.getPlayerInfo(entity.getUUID());
        return info != null;
    }
}
