package com.dioxideliteng.mixin.krs;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.utils.ConnectionDetailsPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    @Shadow
    @Final
    private DisconnectionDetails details;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (KrsClient.nanoVgManager == null || !ConnectionDetailsPanel.hasSnapshot())
            return;

        boolean hideAddress = minecraft != null && minecraft.options != null && minecraft.options.hideServerAddress;
        Component reason = details == null ? null : details.reason();
        KrsClient.nanoVgManager.load(vg -> ConnectionDetailsPanel.renderDisconnected(vg, hideAddress, reason));
    }
}
