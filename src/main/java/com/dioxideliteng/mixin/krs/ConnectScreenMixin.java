package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.utils.ConnectionDetailsPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    @Shadow
    private volatile Connection connection;

    @Shadow
    private volatile boolean aborted;

    @Shadow
    private Component status;

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "connect", at = @At("HEAD"))
    private void krs$captureConnectionTarget(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData, TransferState transferState, CallbackInfo ci) {
        ConnectionDetailsPanel.captureTarget(serverAddress, serverData, transferState);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void krs$renderConnectionDetails(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (KrsClient.nanoVgManager == null || !ConnectionDetailsPanel.hasSnapshot())
            return;

        ConnectionDetailsPanel.captureConnection(connection, status, aborted);

        boolean hideAddress = minecraft != null && minecraft.options != null && minecraft.options.hideServerAddress;
        KrsClient.nanoVgManager.load(vg -> ConnectionDetailsPanel.renderConnecting(vg, hideAddress));
    }
}
