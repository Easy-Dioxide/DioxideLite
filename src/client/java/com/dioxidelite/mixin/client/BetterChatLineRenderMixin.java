package com.dioxidelite.mixin.client;

import com.mojang.authlib.GameProfile;
import com.dioxidelite.Config;
import com.dioxidelite.client.modules.impl.Render.BetterChat.BetterChatLineProfileAccessor;
import com.dioxidelite.client.modules.impl.Render.BetterChat.BetterChatRenderState;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public abstract class BetterChatLineRenderMixin {
    @Shadow @Final private int val$chatBottom;
    @Shadow @Final private int val$entryHeight;
    @Shadow @Final private int val$entryBottomToMessageY;
    @Shadow @Final private ChatComponent.ChatGraphicsAccess val$graphics;
    @Shadow @Final private ChatComponent field_63873;

    @Unique private static final int AVATAR_SIZE = 8;
    @Unique private static final int AVATAR_GAP = 2;
    @Unique private static final int LINE_SHIFT = AVATAR_SIZE + AVATAR_GAP;
    @Unique private static final int AVATAR_X = 0;

    @Unique
    private boolean dioxide_lite$hasAvatar(GuiMessage.Line line) {
        if (!Config.betterChat || !Config.betterChatAvatar || line == null) return false;
        BetterChatLineProfileAccessor lineAccessor = (BetterChatLineProfileAccessor) (Object) line;
        return lineAccessor.dioxide_lite$getOwnerProfile() != null;
    }

    @Inject(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z"))
    private void dioxide_lite$beginAvatarTextShift(GuiMessage.Line line, int index, float alpha, CallbackInfo ci) {
        BetterChatRenderState.setShiftingAvatarLine(dioxide_lite$hasAvatar(line));
    }

    @Inject(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z", shift = At.Shift.AFTER))
    private void dioxide_lite$endAvatarTextShift(GuiMessage.Line line, int index, float alpha, CallbackInfo ci) {
        BetterChatRenderState.setShiftingAvatarLine(false);
    }

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleMessage(IFLnet/minecraft/util/FormattedCharSequence;)Z"))
    private void dioxide_lite$shiftMessageX(Args args, GuiMessage.Line line, int index, float alpha) {
        if (!Config.betterChat || !Config.betterChatAvatar) return;
        BetterChatLineProfileAccessor lineAccessor = line == null ? null : (BetterChatLineProfileAccessor) (Object) line;
        GameProfile profile = lineAccessor == null ? null : lineAccessor.dioxide_lite$getOwnerProfile();
        if (profile != null && lineAccessor.dioxide_lite$shouldDrawAvatar()) {
            int lineBottom = this.val$chatBottom - index * this.val$entryHeight;
            int lineTop = lineBottom - this.val$entryHeight;
            int avatarY = lineTop + ((this.val$entryHeight - AVATAR_SIZE) / 2);

            Minecraft client = ((ChatComponentAccessor) this.field_63873).dioxide_lite$getMinecraft();
            if (client != null) {
                try {
                    GuiGraphics graphics = this.dioxide_lite$getGraphics();
                    PlayerSkin skin = client.getSkinManager().createLookup(profile, false).get();
                    PlayerFaceRenderer.draw(graphics, skin, AVATAR_X, avatarY, AVATAR_SIZE);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Unique
    private GuiGraphics dioxide_lite$getGraphics() {
        if (this.val$graphics instanceof ChatComponentDrawingBackgroundGraphicsAccessor background) {
            return background.dioxide_lite$getGraphics();
        }
        if (this.val$graphics instanceof ChatComponentDrawingFocusedGraphicsAccessor focused) {
            return focused.dioxide_lite$getGraphics();
        }
        throw new IllegalStateException("Unsupported ChatGraphicsAccess: " + this.val$graphics.getClass().getName());
    }

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleTag(IIIIFLnet/minecraft/client/GuiMessageTag;)V"))
    private void dioxide_lite$shiftTagX(Args args, GuiMessage.Line line, int index, float alpha) {
    }

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;handleTagIcon(IIZLnet/minecraft/client/GuiMessageTag;Lnet/minecraft/client/GuiMessageTag$Icon;)V"))
    private void dioxide_lite$shiftTagIconX(Args args, GuiMessage.Line line, int index, float alpha) {
    }
}
