package mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.misc.NoBreakCooldown2;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
   @Shadow
   public int destroyDelay;

   @ModifyExpressionValue(method = "ensureHasSentCarriedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedSlot()I"))
   private int dioxideliteng$rodServerSlot(int original) {
      return FeatureManager.autoRod == null ? original : FeatureManager.autoRod.serverSlot(original);
   }

   @Inject(method = "attack", at = @At("HEAD"))
   private void dioxideliteng$recordAttack(Player player, Entity target, CallbackInfo callback) {
      com.dioxideliteng.module.visual.Hud.SessionHud.SessionTracker.attack(target);
   }

   // Krs parity: AttackEvent at MultiPlayerGameMode#attack HEAD (cancellable).
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$attackEvent(Player player, Entity target, CallbackInfo callback) {
      com.dioxideliteng.event.impl.EventAttack event = new com.dioxideliteng.event.impl.EventAttack(target);
      event.call();
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   // Krs parity: CancelBlockBreakingEvent at MultiPlayerGameMode#stopDestroyBlock HEAD (cancellable).
   @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$cancelBlockBreaking(CallbackInfo callback) {
      com.dioxideliteng.event.impl.EventCancelBlockBreaking event = new com.dioxideliteng.event.impl.EventCancelBlockBreaking();
      event.call();
      if (event.isCancelled()) {
         callback.cancel();
      }
   }

   // Krs parity: NoBreakCooldown (level) — zero the first destroyDelay read of continueDestroyBlock.
   @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.GETFIELD, ordinal = 0))
   private int dioxideliteng$noBreakCooldown(MultiPlayerGameMode instance) {
      if (NoBreakCooldown2.isActive())
         return 0;

      return this.destroyDelay;
   }
}
