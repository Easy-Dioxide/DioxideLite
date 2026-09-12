package com.dioxidelite.mixin;

import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("RETURN"))
    private void DioxideLite$appendLegendarySuffix(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (entity instanceof Player player && state.nameTag != null) {
            state.nameTag = LegendSuffixUtil.appendIfLegendary(
                    state.nameTag,
                    player.getName().getString());
        }
    }
}
