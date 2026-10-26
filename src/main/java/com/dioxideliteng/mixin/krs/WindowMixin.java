package com.dioxideliteng.mixin.krs;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dioxideliteng.krs.imguimc.imgui.ImguiLoader;

@Mixin(Window.class)
public abstract class WindowMixin {

    @Inject(method = "handleEvent", at = @At("HEAD"))
    private void forwardWindowEventToImgui(SDL_Event event, CallbackInfo ci) {
        ImguiLoader.onSdlEvent(event);
    }
}
