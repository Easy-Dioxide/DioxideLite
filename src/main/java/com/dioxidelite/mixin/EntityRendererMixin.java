package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.advanced.SeeInvisibles;
import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import com.dioxidelite.module.modules.player.NameChanger;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    /** 移植版 ESP 的 Names 打开时隐藏原版头顶名字（非生物实体渲染器这条路径）。 */
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideVanillaNameTag(Entity entity, double distanceToCamera,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living
                && com.dioxidelite.module.modules.render.advanced.ESP.INSTANCE.hidesVanillaNameTag(living)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("RETURN"))
    private void DioxideLite$appendLegendarySuffix(T entity, S state, float partialTicks, CallbackInfo ci) {
        SeeInvisibles seeInvisibles = SeeInvisibles.INSTANCE;
        if (seeInvisibles.isEnabled() && entity instanceof LivingEntity living
                && seeInvisibles.shouldRender(living)) {
            state.isInvisible = false;
        }
        if (entity instanceof Player player) {
            String rawName = NameChanger.apply(player.getName().getString());
            if (state.nameTag == null) {
                // 26.1.2 leaves the local player's name tag unset; fill it so
                // the name-tag module (Legend Watch) can decorate it.
                state.nameTag = net.minecraft.network.chat.Component.literal(rawName);
                state.nameTagAttachment = player.getAttachments().getNullable(
                        net.minecraft.world.entity.EntityAttachment.NAME_TAG,
                        0, player.getYRot(partialTicks));
            }
            // The client logo is painted by NameTagLogoRenderer on the shared
            // Skija canvas; vanilla name tags cannot render the bitmap glyph.
            state.nameTag = LegendSuffixUtil.appendIfLegendary(state.nameTag, rawName);
        }
    }
}
