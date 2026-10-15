package mixins;

import com.dioxideliteng.event.impl.EventKey;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public class MixinKeyboardHandler {
   // Krs parity: KeyboardEvent at KeyboardHandler#keyPress HEAD.
   @Inject(method = "keyPress", at = @At("HEAD"))
   private void dioxideliteng$keyEvent(long window, int action, KeyEvent keyEvent, CallbackInfo callback) {
      if (keyEvent != null && keyEvent.key() != -1) {
         EventKey event = new EventKey(keyEvent.key(), action);
         event.call();
      }
   }
}
