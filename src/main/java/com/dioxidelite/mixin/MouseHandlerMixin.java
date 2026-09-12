package com.dioxidelite.mixin;

import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.events.MouseButtonEvent;
import com.dioxidelite.event.events.MouseScrollEvent;
import com.dioxidelite.DioxideLite;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Publishes raw mouse-button events for module binds. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$onButton(long handle, MouseButtonInfo button, int action, CallbackInfo ci) {
        MouseHandler mouse = (MouseHandler) (Object) this;
        Window window = DioxideLite.mc().getWindow();
        double rawX = mouse.xpos();
        double rawY = mouse.ypos();
        MouseButtonEvent event = EventBus.INSTANCE.post(
                new MouseButtonEvent(
                        button.button(), action, button.modifiers(),
                        MouseHandler.getScaledXPos(window, rawX),
                        MouseHandler.getScaledYPos(window, rawY),
                        rawX, rawY));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$onScroll(
            long handle,
            double horizontal,
            double vertical,
            CallbackInfo ci) {
        MouseHandler mouse = (MouseHandler) (Object) this;
        Window window = DioxideLite.mc().getWindow();
        double rawX = mouse.xpos();
        double rawY = mouse.ypos();
        MouseScrollEvent event = EventBus.INSTANCE.post(new MouseScrollEvent(
                horizontal, vertical,
                MouseHandler.getScaledXPos(window, rawX),
                MouseHandler.getScaledYPos(window, rawY),
                rawX, rawY));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
