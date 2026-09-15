package com.dioxidelite.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.dioxidelite.command.CommandManager;
import com.dioxidelite.irc.IrcChatHandler;
import com.dioxidelite.ui.dioxide.DynamicIslandBridge;
import com.dioxidelite.ui.hud.HudEditorScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intercepts dot commands at the chat-screen send call, before signing. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    @WrapOperation(
            method = "handleChatInput(Ljava/lang/String;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"))
    private void DioxideLite$handleClientCommand(ClientPacketListener listener, String message,
                                            Operation<Void> original) {
        boolean handled = CommandManager.INSTANCE.handle(message) || IrcChatHandler.handle(message);
        DynamicIslandBridge.getInstance().onCommandSubmitted(message, handled);
        if (!handled) {
            original.call(listener, message);
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

    @Inject(method = "keyPressed", at = @At("TAIL"))
    private void DioxideLite$trackCommandInput(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (input != null) DynamicIslandBridge.getInstance().onChatInput(input.getValue());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hudEditorClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.handleChatOverlayClick(event, doubleClick)) cir.setReturnValue(true);
    }

}
