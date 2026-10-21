package mixins;

import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.combat.TargetStrafe;
import com.dioxideliteng.module.movement.MovementFix;
import com.dioxideliteng.module.movement.Sprint2;
import com.dioxideliteng.util.krs.MovementUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public class MixinLivingEntity {
   @ModifyExpressionValue(
      method = {"jumpFromGround"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
      )}
   )
   private float dioxideliteng$jumpYaw(float originalYaw) {
      // Krs parity: LivingEntityMixin#jumpEvent — the sprint-jump 0.2 boost
      // must be applied along the rotation the server simulates, not along
      // the client's real yaw. With Scaffold2 rotations active the client
      // would otherwise gain a sideways impulse at every jump while the
      // server sees a clean spoofed-rotation simulation (simulation VL).
      // The LocalPlayer guard reproduces Krs's (Object) this instanceof
      // LocalPlayer check — vanilla mobs keep their own yaw.
      //
      // 26.3 jumpFromGround reads getYRot exactly once (the sprint boost),
      // so replacing that expression is equivalent to Krs's full rewrite.
      if ((Object) this instanceof LocalPlayer) {
         float directionYaw = originalYaw;
         if (MovementFix.shouldFixMovement()) {
            directionYaw = MovementFix.getJumpMovementYaw();
         } else if (TargetStrafe.targetStrafeHook()) {
            directionYaw = MovementUtil.getPlayerDirection();
         } else if (Events.ROTATION.hasMovementCorrection()) {
            directionYaw = Events.ROTATION.usesClientRotation() ? Events.ROTATION.getYaw() : Events.PRE_MOTION.getYaw();
         }

         // Krs parity: Sprint#getOmnidirectionalJumpYaw is the unconditional
         // tail of the directionYaw chain; it self-guards on module state.
         return Sprint2.getOmnidirectionalJumpYaw(directionYaw);
      }

      return originalYaw;
   }
}
