package com.dioxideliteng.krs.imguimc.screen;

import com.dioxideliteng.krs.hacks.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EmptyScreen extends Screen {

    public EmptyScreen() {
        super(Component.literal("EmptyScreen"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ModuleManager.pullDebugScreen();
    }
}
