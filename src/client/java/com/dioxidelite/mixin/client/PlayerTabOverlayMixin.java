package com.dioxidelite.mixin.client;

import com.dioxidelite.Config;
import com.dioxidelite.client.modules.impl.Render.DynamicIslandRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void dioxide_lite$dynamicIslandTabList(GuiGraphics guiGraphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        if (!Config.dynamicIsland) {
            return;
        }

        DynamicIslandRenderer.getInstance().requestTabListFrame();
        ci.cancel();
    }
}
