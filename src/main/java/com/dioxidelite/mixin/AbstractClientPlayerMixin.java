package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.advanced.CapeChanger;
import com.dioxidelite.module.modules.render.advanced.SkinChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin Changer / Cape Changer：把本地玩家解析到的皮肤换成模块生成的自定义贴图。 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void DioxideLite$customSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (self != Minecraft.getInstance().player) {
            return;
        }
        PlayerSkin original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        ClientAsset.Texture body = original.body();
        ClientAsset.Texture cape = original.cape();
        PlayerModelType model = original.model();
        boolean changed = false;

        Identifier skinTexture = SkinChanger.appliedSkinTexture();
        if (SkinChanger.INSTANCE.isEnabled() && skinTexture != null) {
            body = new ClientAsset.ResourceTexture(skinTexture);
            PlayerModelType skinModel = SkinChanger.appliedSkinModel();
            if (skinModel != null) {
                model = skinModel;
            }
            changed = true;
        }
        Identifier capeTexture = CapeChanger.appliedCapeTexture();
        if (CapeChanger.INSTANCE.isEnabled() && capeTexture != null) {
            cape = new ClientAsset.ResourceTexture(capeTexture);
            changed = true;
        }
        if (changed) {
            cir.setReturnValue(PlayerSkin.insecure(body, cape, original.elytra(), model));
        }
    }
}
