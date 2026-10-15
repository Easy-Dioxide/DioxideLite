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
      EventPostMoveInput postMoveInputEvent = Events.POST_MOVE_INPUT;
      postMoveInputEvent.call();
      // Krs parity: KeyboardInputMixin tick TAIL runs the module input hooks
      // (Velocity.hookJumpResetInput / WTap.hookWTapInput) after the movement
      // input fixes, so their modifications land last.
      com.dioxideliteng.module.combat.Velocity2.hookJumpResetInput((net.minecraft.client.player.ClientInput) (Object) this);
      com.dioxideliteng.module.combat.WTap.hookWTapInput((net.minecraft.client.player.ClientInput) (Object) this);
   }
}
