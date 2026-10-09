package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPreMotion;
import com.dioxideliteng.event.impl.EventSlowdown;
import com.dioxideliteng.event.impl.EventSprint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
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
      Events.ROTATION.reset(this.getYRot(), this.getXRot()).call();
      if (Events.ROTATION.isCancelled()) {
         callback.cancel();
      }
   }

   // Keep vanilla's sendPosition body intact: ViaFabricPlus injects its movement
   // threshold, idle packets, position reminder and sneaking protocol fixes here.
   @Inject(method = "sendPosition", at = @At("HEAD"))
   private void dioxideliteng$preMotion(CallbackInfo ci) {
      this.dioxideliteng$motion = Events.PRE_MOTION.reset(this.getX(), this.getY(), this.getZ(), this.onGround(), this.horizontalCollision);
      this.dioxideliteng$motion.call();
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
}
