package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import com.dioxidelite.client.modules.impl.Render.HudEditOverlay;
import com.dioxidelite.client.render.skia.LiquidGlassVisualSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void dioxide_lite$liquidGlassChat(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Config.liquidGlassAllVisuals) {
            Minecraft mc = Minecraft.getInstance();
            LiquidGlassVisualSystem.renderChat(graphics, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), true);
        }
    }

    @Inject(method = "<init>(Ljava/lang/String;Z)V", at = @At("RETURN"))
    private void constructorHook(String string, boolean bl, CallbackInfo ci) {
        if (!Config.chatHudEditQuickEnable) return;
        HudEditOverlay.getInstance().startOpen();
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onCloseHook(CallbackInfo ci) {
        if (!Config.chatHudEditQuickEnable) return;
        HudEditOverlay.getInstance().startClose();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void removedHook(CallbackInfo ci) {
        if (!Config.chatHudEditQuickEnable) return;
        HudEditOverlay.getInstance().startClose();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.chatHudEditQuickEnable) return;
        if (HudEditOverlay.getInstance().mouseScrolled(mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}
