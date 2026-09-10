package com.dioxidelite.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.dioxidelite.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Gui.class, priority = 999)
public class HotbarSmoothMixin {
    @Unique private static final int SLOT_WIDTH = 20;
    @Unique private static final int SLOT_COUNT = 9;
    @Unique private static final int ROLLOVER_SPACE = 4;
    @Unique private float dioxide_lite$smoothSelectorPos;

    @WrapMethod(method = "renderHotbarAndDecorations")
    private void dioxide_lite$wrapHotbarAndDecorations(GuiGraphics graphics, DeltaTracker deltaTracker, Operation<Void> operation) {
        if (!Config.smoothHotbarScrolling) {
            operation.call(graphics, deltaTracker);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            operation.call(graphics, deltaTracker);
            return;
        }

        float target = getTargetPosition(player.getInventory().getSelectedSlot(), Config.hotbarRollover);
        float smoothness = Math.min(0.99f, Math.max(0.05f, Config.smoothHotbarAnimationSpeed));
        dioxide_lite$smoothSelectorPos += (target - dioxide_lite$smoothSelectorPos) * (1f - (float) Math.pow(smoothness, deltaTracker.getRealtimeDeltaTicks()));

        operation.call(graphics, deltaTracker);
    }

    @WrapOperation(
            method = "renderItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 1)
    )
    private void dioxide_lite$moveSelector(GuiGraphics graphics, RenderPipeline pipeline, Identifier texture, int x, int y, int width, int height, Operation<Void> operation) {
        if (!Config.smoothHotbarScrolling) {
            operation.call(graphics, pipeline, texture, x, y, width, height);
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        int selectedSlot = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getInventory().getSelectedSlot() : 0;
        pose.translate(dioxide_lite$smoothSelectorPos - (selectedSlot * SLOT_WIDTH), 0f);
        operation.call(graphics, pipeline, texture, x, y, width, height);
        pose.popMatrix();
    }

    @Unique
    private static float getTargetPosition(int selectedSlot, int rollover) {
        return (selectedSlot + rollover * SLOT_COUNT) * SLOT_WIDTH;
    }
}
