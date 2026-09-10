package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiMessage.Line.class)
public class BetterChatGuiMessageMixin {
    @Inject(method = "tag", at = @At("HEAD"), cancellable = true)
    private void dioxide_lite$removeIndicator(CallbackInfoReturnable<GuiMessageTag> cir) {
        if (Config.betterChat) {
            cir.setReturnValue(null);
        }
    }
}
