package mixins;

import com.dioxideliteng.ClientBranding;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.ui.mainmenu.window.WindowBranding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public class MixinMinecraft {
   private boolean modulesInitialized = false;

   @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$windowTitle(CallbackInfoReturnable<String> callback) {
      callback.setReturnValue(ClientBranding.WINDOW_TITLE);
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void dioxideliteng$windowIcon(CallbackInfo callback) {
      WindowBranding.applyIcon(((Minecraft)(Object)this).getWindow().handle());
   }

   @Inject(
      method = {"onResourceLoadFinished"},
      at = {@At("TAIL")}
   )
   private void dioxideliteng$initializeModules(CallbackInfo callback) {
      if (!this.modulesInitialized) {
         FeatureManager.loadEnabled();
         com.dioxideliteng.config.ConfigManager.loadState();
         this.modulesInitialized = true;
      }
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;handleKeybinds()V",
         shift = Shift.AFTER
      )}
   )
   private void dioxideliteng$dispatchTick(CallbackInfo callback) {
      Events.TICK.call();
   }

   // Krs parity: fire HANDLE_INPUT at the same point Krs does (Minecraft#tick, at the
   // Gui.overlay() call that gates vanilla input handling). Cancelling the event skips
   // the rest of tick(), matching Krs's HandleInputEvent semantics. This runs before
   // ClientLevel#tickEntities (player tick -> ROTATION dispatch -> sendPosition), so
   // rotations stepped by Krs modules here reach the server on the same tick.
   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;",
         ordinal = 0
      )},
      cancellable = true
   )
   private void dioxideliteng$dispatchHandleInput(CallbackInfo callback) {
      if (((Minecraft)(Object)this).player == null) return;
      Events.HANDLE_INPUT.call();
      if (Events.HANDLE_INPUT.isCancelled()) {
         callback.cancel();
      }
   }

   // Krs parity: WorldEvent at Minecraft#setLevel HEAD (join, leave, dimension swap).
   @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$dispatchWorld(net.minecraft.client.multiplayer.ClientLevel world, CallbackInfo callback) {
      EventWorld event = new EventWorld(((Minecraft) (Object) this).level, world);
      event.call();
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void dioxideliteng$trackSessionAndSave(CallbackInfo callback) {
      FeatureManager.clientTickEnd();
      com.dioxideliteng.config.ConfigManager.tick();
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void dioxideliteng$initializeRestoredWorldModules(CallbackInfo callback) {
      if (this.modulesInitialized) FeatureManager.clientTick();
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void dioxideliteng$flushState(CallbackInfo callback) {
      com.dioxideliteng.config.ConfigManager.flush();
      com.dioxideliteng.util.render.NVGRenderer.close();
   }
}
