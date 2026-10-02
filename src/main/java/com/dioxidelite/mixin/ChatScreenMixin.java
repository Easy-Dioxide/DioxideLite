package com.dioxidelite.mixin;

import com.dioxidelite.command.CommandManager;
import com.dioxidelite.irc.IrcChatHandler;
import com.dioxidelite.ui.hud.HudEditorScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intercepts dot commands before the chat screen sends them to the server. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    /** 打开聊天栏时自动触发 HUD 编辑 overlay，让用户在聊天界面就能拖拽 HUD 元素。 */
    @Inject(method = "init", at = @At("TAIL"))
    private void DioxideLite$openHudOverlayOnChatOpen(CallbackInfo ci) {
        HudEditorScreen.openOverlay();
    }

    /** 关闭聊天栏时同步关闭 HUD 编辑 overlay，避免残留状态。 */
    @Inject(method = "removed", at = @At("HEAD"))
    private void DioxideLite$closeHudOverlayOnChatClose(CallbackInfo ci) {
        HudEditorScreen.closeOverlay();
    }

    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$handleClientCommand(String message, boolean addToHistory,
                                                 org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (CommandManager.INSTANCE.handle(message) || IrcChatHandler.handle(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void DioxideLite$hudEditorMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.overlayMouseClicked(event, doubleClick)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 0)
    private void DioxideLite$hudEditorMouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.overlayMouseDragged(event, dragX, dragY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void DioxideLite$hudEditorMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.overlayMouseReleased(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void DioxideLite$hudEditorMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.overlayMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 0)
    private void DioxideLite$hudEditorCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.overlayCharTyped(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void DioxideLite$completeClientCommand(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (HudEditorScreen.overlayKeyPressed(event)) {
            cir.setReturnValue(true);
            return;
        }
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
