package com.dioxidelite.mixin.client;

import com.mojang.authlib.GameProfile;
import com.dioxidelite.client.modules.impl.Render.BetterChat.BetterChatHeadsState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class BetterChatPlayerChatMessageMixin {
    @Inject(method = "handlePlayerInfoUpdate", at = @At("HEAD"))
    private void dioxide_lite$cacheSender(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
            GameProfile profile = entry.profile();
            if (profile != null) {
                BetterChatHeadsState.getInstance().setPendingSender(profile);
            }
        }
    }

    @Inject(method = "handlePlayerChat", at = @At("HEAD"))
    private void dioxide_lite$cacheSender(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        GameProfile profile = BetterChatHeadsState.getInstance().findProfile(packet.sender());
        if (profile != null) {
            BetterChatHeadsState.getInstance().setPendingSender(profile);
        }
    }
}
