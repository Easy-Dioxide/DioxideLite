package mixins.krs;

import com.dioxideliteng.krs.KrsBridge;
import com.dioxideliteng.krs.events.features.KeyboardEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raises KRS's {@link KeyboardEvent} for every key press/release (KRS's ImGui capture logic is not ported). */
@Mixin(KeyboardHandler.class)
public abstract class MixinKrsKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void krs$keyEvent(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (keyEvent.key() > 0 && KrsBridge.has(KeyboardEvent.class)) {
            KrsBridge.call(new KeyboardEvent(keyEvent.key(), action));
        }
    }
}
