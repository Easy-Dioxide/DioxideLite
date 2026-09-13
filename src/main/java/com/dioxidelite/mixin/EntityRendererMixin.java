package com.dioxidelite.mixin;

import com.dioxidelite.irc.IrcNameTagUtil;
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
        if (entity instanceof Player player) {
            String rawName = player.getName().getString();
            if (state.nameTag == null) {
                // 26.1.2 leaves the local player's name tag unset; fill it so
                // the name-tag module (Legend Watch) can decorate it.
                state.nameTag = player.getName();
                state.nameTagAttachment = player.getAttachments().getNullable(
                        net.minecraft.world.entity.EntityAttachment.NAME_TAG,
                        0, player.getYRot(partialTicks));
            }
            state.nameTag = LegendSuffixUtil.appendIfLegendary(state.nameTag, rawName);
            state.nameTag = IrcNameTagUtil.appendLogoIfIrc(state.nameTag, rawName);
        }
    }
}
