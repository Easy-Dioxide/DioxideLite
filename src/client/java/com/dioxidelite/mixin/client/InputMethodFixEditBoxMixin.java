package com.dioxidelite.mixin.client;

import com.dioxidelite.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class InputMethodFixEditBoxMixin {
    @Inject(method = "setFocused", at = @At("TAIL"))
    private void dioxide_lite$onFocusedChanged(boolean focused, CallbackInfo ci) {
        InputMethodFix.refreshForFocusedTextField(net.minecraft.client.Minecraft.getInstance());
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void dioxide_lite$onCharTyped(CharacterEvent characterEvent, CallbackInfoReturnable<Boolean> cir) {
        InputMethodFix.refreshForFocusedTextField(net.minecraft.client.Minecraft.getInstance());
    }
}
