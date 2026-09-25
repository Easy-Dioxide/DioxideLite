package com.dioxidelite.mixin;

import com.dioxidelite.command.CommandManager;
import com.dioxidelite.irc.IrcChatHandler;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intercepts dot commands before the chat screen sends them to the server. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$handleClientCommand(String message, boolean addToHistory,
                                                 org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (CommandManager.INSTANCE.handle(message) || IrcChatHandler.handle(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$completeClientCommand(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() != GLFW.GLFW_KEY_TAB) {
            return;
        }
        String current = input.getValue();
        String completed = CommandManager.INSTANCE.complete(current, input.getCursorPosition());
        if (completed == null || completed.equals(current)) {
            return;
        }
        input.setValue(completed);
        input.setCursorPosition(completed.length());
        cir.setReturnValue(true);
    }
}
