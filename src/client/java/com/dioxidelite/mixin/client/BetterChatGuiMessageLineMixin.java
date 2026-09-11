package com.dioxidelite.mixin.client;

import com.mojang.authlib.GameProfile;
import com.dioxidelite.client.modules.impl.Render.BetterChat.BetterChatHeadsState;
import com.dioxidelite.client.modules.impl.Render.BetterChat.BetterChatLineProfileAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMessage.Line.class)
public abstract class BetterChatGuiMessageLineMixin implements BetterChatLineProfileAccessor {
    @Unique private GameProfile dioxide_lite$ownerProfile;
    @Unique private boolean dioxide_lite$drawAvatar;

    @Inject(method = "<init>(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
    private void dioxide_lite$onInit(int addedTime, FormattedCharSequence content, GuiMessageTag tag, boolean endOfEntry, CallbackInfo ci) {
        BetterChatHeadsState state = BetterChatHeadsState.getInstance();
        this.dioxide_lite$ownerProfile = state.prepareLineProfile(endOfEntry);
        this.dioxide_lite$drawAvatar = state.shouldDrawAvatarForCurrentLine();
    }

    @Override
    public GameProfile dioxide_lite$getOwnerProfile() {
        return dioxide_lite$ownerProfile;
    }

    @Override
    public boolean dioxide_lite$shouldDrawAvatar() {
        return dioxide_lite$drawAvatar;
    }
}
