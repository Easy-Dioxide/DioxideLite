package mixins.krs;

import com.dioxideliteng.krs.KrsBridge;
import com.dioxideliteng.krs.events.features.KeyboardEvent;
import com.dioxideliteng.krs.events.features.MouseClickEvent;
import com.dioxideliteng.krs.events.features.MouseScrollEvent;
import com.dioxideliteng.krs.utils.input.Keys;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raises KRS's scroll / click events (mouse buttons are also reported as keys, like in KRS). */
@Mixin(MouseHandler.class)
public abstract class MixinKrsMouse {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void krs$scrollEvent(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!KrsBridge.has(MouseScrollEvent.class))
            return;

        if (KrsBridge.call(new MouseScrollEvent(horizontal, vertical)))
            ci.cancel();
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void krs$clickEvent(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (KrsBridge.has(KeyboardEvent.class))
            KrsBridge.call(new KeyboardEvent(Keys.mouseKey(button.button()), action));

        if (!KrsBridge.has(MouseClickEvent.class))
            return;

        if (KrsBridge.call(new MouseClickEvent(window, button.button(), action, button.modifiers())))
            ci.cancel();
    }
}
