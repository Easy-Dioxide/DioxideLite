package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.module.combat.TargetStrafe;
import com.dioxideliteng.module.movement.MovementFix;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class MixinEntity {
   @Overwrite
   public void moveRelative(float speed, Vec3 input) {
      // Krs parity: EntityMixin#updateVelocity — the strafe yaw feeding
      // getInputVector must match the rotation the server simulates against.
      // When MovementFix (Scaffold2/KillAura2 rotations) is active the WASD
      // impulse was already remapped into the spoofed rotation frame by
      // KeyboardInput#tick TAIL (MovementFix.updateInput), so the client must
      // also compute its movement vector with the spoofed yaw; otherwise the
      // client moves along its real yaw while the server simulates the
      // transmitted (spoofed) rotation — a per-tick position divergence that
      // GrimAC reports as simulation violations.
      Entity entity = (Entity)(Object)this;
      float strafe = entity.getYRot();
      if (entity == Minecraft.getInstance().player && entity instanceof LocalPlayer) {
         if (MovementFix.shouldFixMovement()) {
            strafe = MovementFix.getMovementYaw();
         } else if (TargetStrafe.targetStrafeHook()) {
            strafe = MovementUtil.getPlayerDirection();
         } else if (Events.ROTATION.hasMovementCorrection()) {
            strafe = Events.ROTATION.getYaw();
         }
      }

      Vec3 movement = getInputVector(input, speed, strafe);
      entity.setDeltaMovement(entity.getDeltaMovement().add(movement));
   }

   // Krs parity: EntityMixin#targetStrafeCollisionGuard — keep the TargetStrafe
   // movement from being eaten by horizontal collisions (same guard Krs applies
   // to the incoming movement vector of Entity#move).
   @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
   private Vec3 dioxideliteng$targetStrafeCollisionGuard(Vec3 movement) {
      if ((Object) this instanceof LocalPlayer && TargetStrafe.targetStrafeHook()) {
         return TargetStrafe.preventHorizontalCollision(movement);
      }

      return movement;
   }

   @Shadow
   protected static Vec3 getInputVector(Vec3 position, float speed, float yaw) {
      throw new AssertionError();
   }

   @Shadow
   public abstract void setSwimming(boolean swimming);

   @Inject(
      method = {"updateSwimming"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dioxideliteng$preventSwimming(CallbackInfo callback) {
      if (FeatureManager.antiSwim.isEnabled()) {
         this.setSwimming(false);
         callback.cancel();
      }
   }
}
