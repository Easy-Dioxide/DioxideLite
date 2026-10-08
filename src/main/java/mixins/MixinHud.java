package mixins;

import com.dioxideliteng.module.FeatureManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Hud.class})
public class MixinHud {
   @Inject(method="extractEffects",at=@At("HEAD"),cancellable=true)
   private void dioxideliteng$replacePotionEffects(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo callback) {
      if (com.dioxideliteng.module.visual.Hud.enabled(com.dioxideliteng.module.visual.Hud.Widget.POTION_STATUS)) callback.cancel();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 3
   )
   private int dioxideliteng$offsetScoreboardBottom(int coordinate) {
      return coordinate - this.dioxideliteng$scoreboardOffsetY();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 1
   )
   private int dioxideliteng$offsetScoreboardTop(int coordinate) {
      return coordinate - this.dioxideliteng$scoreboardOffsetY();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 0
   )
   private int dioxideliteng$offsetScoreboardLeft(int coordinate) {
      return coordinate - this.dioxideliteng$scoreboardOffsetX();
   }

   private int dioxideliteng$scoreboardOffsetX() {
      return FeatureManager.scoreboard.isEnabled() ? (int)FeatureManager.scoreboard.x.getValue() : 0;
   }

   private int dioxideliteng$scoreboardOffsetY() {
      return FeatureManager.scoreboard.isEnabled() ? (int)FeatureManager.scoreboard.y.getValue() : 0;
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
      ),
      index = 2
   )
   private int dioxideliteng$offsetScoreboardRight(int coordinate) {
      return coordinate - this.dioxideliteng$scoreboardOffsetX();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      ),
      index = 2
   )
   private int dioxideliteng$offsetScoreboardTextX(int coordinate) {
      return coordinate - this.dioxideliteng$scoreboardOffsetX();
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
      ),
      index = 3
   )
   private int dioxideliteng$offsetScoreboardTextY(int coordinate) {
      return coordinate - this.dioxideliteng$scoreboardOffsetY();
   }
}
