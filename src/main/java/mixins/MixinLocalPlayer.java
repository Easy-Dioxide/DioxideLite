package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPreMotion;
import com.dioxideliteng.event.impl.EventSlowdown;
import com.dioxideliteng.event.impl.EventSprint;
import com.dioxideliteng.module.movement.MovementFix;
import com.dioxideliteng.module.movement.NoSlow2;
import com.dioxideliteng.module.movement.Sprint2;
import com.dioxideliteng.util.krs.RotationManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
   @Shadow
   private int sprintTriggerTime;
   @Shadow
   protected abstract void sendIsSprintingIfNeeded();
   @Shadow
   private boolean isSlowDueToUsingItem() {
      return false;
   }

   @Shadow
   private boolean isSprintingPossible(boolean isFlying) {
      return false;
   }

   @Shadow
   public abstract boolean isMovingSlowly();

   @Shadow
   public ClientInput input;
   @Unique
   private EventPreMotion dioxideliteng$motion;
   @Unique
   private float dioxideliteng$motionYaw;
   @Unique
   private float dioxideliteng$motionPitch;

   public MixinLocalPlayer(ClientLevel level, GameProfile profile) {
      super(level, profile);
   }

   @ModifyExpressionValue(
      method = {"modifyInput"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"
      )
   )
   private float dioxideliteng$itemUseSpeedMultiplier(float original) {
      EventSlowdown slowdownEvent = Events.SLOWDOWN.reset(original);
      slowdownEvent.call();
      return slowdownEvent.getSpeedMultiplier();
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$dispatchRotation(CallbackInfo callback) {
      // Krs parity: RotationManager.update() runs at Player#aiStep (UpdateEvent).
      // LocalPlayer#tick HEAD is the earliest equivalent point per player tick,
      // and it runs after HANDLE_INPUT (Minecraft#tick -> Gui#overlay) where the
      // Krs modules step their rotations, and before sendPosition consumes them.
      RotationManager.INSTANCE.tick();
      Events.ROTATION.reset(this.getYRot(), this.getXRot()).call();
      // Krs parity: ModuleManager#onMotion global hook. When a Krs module owns the
      // rotation, its spoofed yaw/pitch override whatever modules wrote to the
      // event, so sendPosition transmits them to the server.
      if (RotationManager.INSTANCE.isRotating()) {
         Events.ROTATION.setYaw(RotationManager.INSTANCE.getRotationYaw());
         Events.ROTATION.setPitch(RotationManager.INSTANCE.getRotationPitch());
      }
      if (Events.ROTATION.isCancelled()) {
         callback.cancel();
      }
   }

   // Keep vanilla's sendPosition body intact: ViaFabricPlus injects its movement
   // threshold, idle packets, position reminder and sneaking protocol fixes here.
   @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
   private void dioxideliteng$preMotion(CallbackInfo ci) {
      this.dioxideliteng$motion = Events.PRE_MOTION.reset(this.getX(), this.getY(), this.getZ(), this.onGround(), this.horizontalCollision);
      this.dioxideliteng$motion.call();
      // Krs parity: ClientPlayerEntityMixin#krs$sendPosition calls
      // sendIsSprintingIfNeeded() before the MotionEvent and, when the event is
      // cancelled, returns without sending any move packet and without touching
      // the last-sent caches (xLast/yLast/zLast/yRotLast/xRotLast/lastOnGround/
      // lastHorizontalCollision/positionReminder). Cancelling the vanilla body
      // here reproduces exactly that, and the RETURN-injected post motion below
      // never runs - matching the Krs post MotionEvent that only fires when a
      // mode set callInPost during the (uncancelled) pre phase.
      if (this.dioxideliteng$motion.isCancelled()) {
         this.sendIsSprintingIfNeeded();
         ci.cancel();
         return;
      }
      // Krs parity: ModuleManager#onMotion reads rotationManager at MotionEvent
      // dispatch time (Client.eventManager.call(event) inside krs$sendPosition),
      // NOT at tick HEAD. Modules that step their rotations on Events.UPDATE
      // (Player#aiStep) — AutoPot, Step, AntiAFK2, SpinBot2, CivBreak, Nuker,
      // Breaker — would otherwise transmit one tick stale, because their
      // startRotation only lands in RotationManager after the tick-HEAD
      // snapshot in dioxideliteng$dispatchRotation already ran. Scaffold2 /
      // KillAura2 (Events.TICK, before the player tick) are unaffected; this
      // read is idempotent for them.
      if (RotationManager.INSTANCE.isRotating()) {
         Events.ROTATION.setYaw(RotationManager.INSTANCE.getRotationYaw());
         Events.ROTATION.setPitch(RotationManager.INSTANCE.getRotationPitch());
      }
      this.dioxideliteng$motionYaw = Events.ROTATION.getYaw();
      this.dioxideliteng$motionPitch = Events.ROTATION.getPitch();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
   private double dioxideliteng$motionX(double original) {
      return this.dioxideliteng$motion.getX();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
   private double dioxideliteng$motionY(double original) {
      return this.dioxideliteng$motion.getY();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
   private double dioxideliteng$motionZ(double original) {
      return this.dioxideliteng$motion.getZ();
   }

   // 26.3 constructs Pos/PosRot from position(), not from getX/Y/Z. Substitute
   // that Vec3 too, so packet coordinates match the deltas and last-sent cache.
   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
   private Vec3 dioxideliteng$motionPosition(Vec3 original) {
      return new Vec3(this.dioxideliteng$motion.getX(), this.dioxideliteng$motion.getY(), this.dioxideliteng$motion.getZ());
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
   private float dioxideliteng$motionYaw(float original) {
      return this.dioxideliteng$motionYaw;
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
   private float dioxideliteng$motionPitch(float original) {
      return this.dioxideliteng$motionPitch;
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
   private boolean dioxideliteng$motionOnGround(boolean original) {
      return this.dioxideliteng$motion.isOnGround();
   }

   @ModifyExpressionValue(method = "sendPosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
   private boolean dioxideliteng$motionHorizontalCollision(boolean original) {
      return this.dioxideliteng$motion.isHorizontalCollision();
   }

   @Inject(method = "sendPosition", at = @At("RETURN"))
   private void dioxideliteng$postMotion(CallbackInfo ci) {
      Events.POST_MOTION.call();
      // Krs parity: end of the rewritten sendPosition refreshes the render-side
      // interpolated rotations (prevRender*/render*) used by MixinLivingEntityRenderer.
      RotationManager.INSTANCE.postMotionVisualTick();
   }

   @ModifyExpressionValue(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"
      )
   )
   private boolean dioxideliteng$dispatchSprint(boolean original) {
      // Read the result after ViaFabricPlus's redirect instead of replacing it.
      EventSprint sprintEvent = Events.SPRINT.reset(this.sprintTriggerTime, original);
      sprintEvent.call();
      return sprintEvent.isSprinting();
   }

   // =====================================================================
   // Krs routine: MovementFix / Sprint sprint-condition hooks
   // (com.instrumentalist.mixin.injector.ClientPlayerEntityMixin)
   //
   // These three hooks make the vanilla sprint start/stop conditions read the
   // MovementFix-corrected input (the key presses remapped into the spoofed
   // rotation frame by MovementFix.updateInput) instead of the raw physical
   // input. Without them, a player who is rotated server-side (KillAura2 /
   // Scaffold2 telly) keeps the raw movement direction: sprint starts and
   // stops on the wrong impulse, which is both a functional bug (telly
   // back-turn sprint lock) and an anticheat tell.
   // =====================================================================

   // aiStep: the single ClientInput#hasForwardImpulse read feeding the
   // (swim) sprint start condition.
   @ModifyExpressionValue(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"
      )
   )
   private boolean movementFixSprintStartImpulse(boolean original) {
      if (MovementFix.shouldFixSprintCondition() || Sprint2.shouldSprintOmnidirectional())
         return MovementFix.hasForwardImpulseForSprint(this.input);

      return original;
   }

   // canStartSprinting: when vanilla says no, re-evaluate with the corrected
   // impulse and the NoSlow overrides (mirrors the vanilla body, only the
   // forward-impulse source and the NoSlow exemptions differ).
   @ModifyReturnValue(method = "canStartSprinting()Z", at = @At("RETURN"))
   private boolean canStartSprintingHook(boolean original) {
      if (original)
         return original;

      boolean noSlowActive = NoSlow2.noSlowHook();
      boolean shouldFixForwardImpulse = MovementFix.shouldFixSprintCondition() || Sprint2.shouldSprintOmnidirectional();

      if (!noSlowActive && !shouldFixForwardImpulse)
         return false;

      if (this.isSprinting() || !MovementFix.hasForwardImpulseForSprint(this.input) || !this.isSprintingPossible(this.getAbilities().flying))
         return false;

      if (this.isSlowDueToUsingItem() && !noSlowActive)
         return false;

      if (this.isFallFlying() && !this.isUnderWater())
         return false;

      return !this.isMovingSlowly() || this.isUnderWater() || noSlowActive && NoSlow2.shouldNoSlowSneak();
   }

   // shouldStopRunSprinting / shouldStopSwimSprinting: stop the sprint when the
   // corrected input loses its forward impulse (vanilla reads the raw one).
   @ModifyExpressionValue(
      method = {"shouldStopRunSprinting", "shouldStopSwimSprinting"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"
      )
   )
   private boolean movementFixSprintStopImpulse(boolean original) {
      if (MovementFix.shouldFixSprintCondition() || Sprint2.shouldSprintOmnidirectional())
         return MovementFix.hasForwardImpulseForSprint(this.input);

      return original;
   }
}
