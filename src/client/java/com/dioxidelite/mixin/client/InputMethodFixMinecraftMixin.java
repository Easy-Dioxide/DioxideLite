package com.dioxidelite.mixin.client;

import com.dioxidelite.client.modules.impl.Optimize.InputMethodFix.InputMethodFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class InputMethodFixMinecraftMixin {
    @Shadow public net.minecraft.client.gui.screens.Screen screen;

    @Inject(method = "setWindowActive", at = @At("TAIL"))
    private void dioxide_lite$onWindowActiveChanged(boolean active, CallbackInfo ci) {
        InputMethodFix.onWindowActiveChanged(active, (Minecraft) (Object) this);
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void dioxide_lite$onScreenChanged(Screen screen, CallbackInfo ci) {
        InputMethodFix.onScreenChanged(screen, (Minecraft) (Object) this);
    }
}
