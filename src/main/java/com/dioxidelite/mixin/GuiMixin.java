package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.advanced.NoRender;
import com.dioxidelite.module.modules.render.advanced.Crosshair;
import com.dioxidelite.module.modules.render.DeltaForceStyle;
import com.dioxidelite.ui.hud.ScoreboardHUD;
import com.dioxidelite.ui.dioxide.DioxideDynamicIsland;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Unique
    private boolean DioxideLite$deltaHotbarPosePushed;

    /** 自定义准星开启时隐藏原版准星（准星本体由模块在 2D 画布层绘制）。 */
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideVanillaCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                                  CallbackInfo ci) {
        if (Crosshair.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

    /** NoRender：隐藏 BOSS 血条。 */
    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideBossOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                             CallbackInfo ci) {
        NoRender noRender = NoRender.INSTANCE;
        if (noRender.isEnabled() && noRender.bossBar.get()) {
            ci.cancel();
        }
    }


    @Unique
    private boolean DioxideLite$scoreboardPosePushed;

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"))
    private void DioxideLite$positionScoreboard(GuiGraphicsExtractor graphics,
                                            net.minecraft.world.scores.Objective objective,
                                            CallbackInfo ci) {
        DioxideLite$scoreboardPosePushed = ScoreboardHUD.INSTANCE.positionVanilla(graphics, objective);
    }

    @ModifyArg(
            method = "displayScoreboardSidebar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"),
            index = 4)
    private int DioxideLite$removeScoreboardBackground(int color) {
        return DioxideLite$scoreboardPosePushed && ScoreboardHUD.INSTANCE.shouldRemoveBackground()
                ? 0x00000000 : color;
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void DioxideLite$restoreScoreboardPose(GuiGraphicsExtractor graphics,
                                               net.minecraft.world.scores.Objective objective,
                                               CallbackInfo ci) {
        if (!DioxideLite$scoreboardPosePushed) return;
        graphics.pose().popMatrix();
        DioxideLite$scoreboardPosePushed = false;
    }

    @Inject(method = "extractTabList", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideVanillaTabList(GuiGraphicsExtractor graphics,
                                                  DeltaTracker deltaTracker, CallbackInfo ci) {
        if (DioxideDynamicIsland.getInstance().ownsInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideDeltaForceHotbar(GuiGraphicsExtractor graphics,
                                            DeltaTracker deltaTracker, CallbackInfo ci) {
        DioxideLite$deltaHotbarPosePushed = false;
        DeltaForceStyle deltaForce = DeltaForceStyle.INSTANCE;
        if (!deltaForce.isEnabled()) return;
        float progress = deltaForce.transitionProgress();
        if (progress >= 0.999F) {
            ci.cancel();
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, deltaForce.vanillaHotbarOffset());
        DioxideLite$deltaHotbarPosePushed = true;
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void DioxideLite$restoreDeltaForceHotbarPose(GuiGraphicsExtractor graphics,
                                                     DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!DioxideLite$deltaHotbarPosePushed) return;
        graphics.pose().popMatrix();
        DioxideLite$deltaHotbarPosePushed = false;
    }

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideDeltaForceHealth(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (DeltaForceStyle.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hideDeltaForceFood(GuiGraphicsExtractor graphics,
                                           net.minecraft.world.entity.player.Player player,
                                           int y, int unused, CallbackInfo ci) {
        if (DeltaForceStyle.INSTANCE.isEnabled()) ci.cancel();
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$hidePotionEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        NoRender noRender = NoRender.INSTANCE;
        if (noRender.isEnabled() && noRender.badEffects.get()) {
            ci.cancel();
        }
    }
}
