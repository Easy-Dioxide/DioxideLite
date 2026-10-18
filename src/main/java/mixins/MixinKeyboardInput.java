package mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventMoveInput;
import com.dioxideliteng.event.impl.EventPostMoveInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class MixinKeyboardInput {
   @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
   private Input dioxideliteng$movementInput(Input input) {
      EventMoveInput moveInputEvent = Events.MOVE_INPUT.reset(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), input.sprint());
      moveInputEvent.call();
      return new Input(moveInputEvent.isForward(), moveInputEvent.isBackward(), moveInputEvent.isLeft(), moveInputEvent.isRight(), moveInputEvent.isJump(), moveInputEvent.isSneak(), moveInputEvent.isSprint());
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void dioxideliteng$dispatchPostMoveInput(CallbackInfo callback) {
      // Krs parity: KeyboardInputMixin tick TAIL runs the Krs routine first:
      // MovementFix.updateInput remaps the raw key presses into the spoofed
      // rotation frame (sector input + sprint correction) and rewrites both
      // keyPresses and moveVector. It must run BEFORE the module input hooks
      // (Scaffold2 edgeSafeSneak / tellyJump / autoInputFix via POST_MOVE_INPUT,
      // Velocity2, WTap) so they observe the corrected input.
      com.dioxideliteng.module.movement.MovementFix.updateInput((net.minecraft.client.player.ClientInput) (Object) this);
      EventPostMoveInput postMoveInputEvent = Events.POST_MOVE_INPUT;
      postMoveInputEvent.call();
      // Krs parity: the remaining module input hooks (Velocity.hookJumpResetInput /
      // WTap.hookWTapInput) run after the movement input fixes, so their
      // modifications land last.
      com.dioxideliteng.module.combat.Velocity2.hookJumpResetInput((net.minecraft.client.player.ClientInput) (Object) this);
      com.dioxideliteng.module.combat.WTap.hookWTapInput((net.minecraft.client.player.ClientInput) (Object) this);
   }
}
