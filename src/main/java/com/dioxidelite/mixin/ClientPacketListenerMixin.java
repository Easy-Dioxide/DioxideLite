package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/mixin/ClientPacketListenerMixin.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.RespawnEvent;
import com.dioxidelite.module.modules.render.ESP;
import com.dioxidelite.module.modules.render.TeamViewer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Publishes world-session resets after vanilla finishes applying a respawn packet. */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void dioxidelite$afterRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        TeamViewer.INSTANCE.clearTeam();
        EventBus.INSTANCE.post(new RespawnEvent());
    }

    @Inject(method = "handleOpenScreen", at = @At("RETURN"))
    private void dioxidelite$afterOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        ESP.INSTANCE.confirmContainerOpened(packet);
    }

    @Inject(method = "handleBlockEvent", at = @At("RETURN"))
    private void dioxidelite$afterBlockEvent(ClientboundBlockEventPacket packet, CallbackInfo ci) {
        ESP.INSTANCE.handleContainerBlockEvent(packet);
    }
}
