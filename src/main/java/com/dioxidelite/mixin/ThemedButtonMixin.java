package com.dioxidelite.mixin;

import com.dioxidelite.ui.screen.VanillaButtonOverlay;
import com.dioxidelite.ui.screen.VanillaScreenTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class ThemedButtonMixin extends AbstractWidget {

    protected ThemedButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$drawThemedButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                        float partialTick, CallbackInfo ci) {
        if (!VanillaScreenTheme.canReplaceVanillaButtons(Minecraft.getInstance().screen)) {
            return;
        }
        // Checkboxes are buttons too, but the tick box is drawn from inside
        // extractWidgetRenderState, so cancelling the chrome cancels the tick as well.
        // Handing the state to the overlay is what keeps on/off readable.
        if ((Object) this instanceof Checkbox checkbox) {
            VanillaButtonOverlay.addCheckbox(getX(), getY(), width, height,
                    getMessage().getString(), isHoveredOrFocused(), active, getAlpha(),
                    checkbox.selected());
        } else if ((Object) this instanceof CycleButton<?> cycle
                && cycle.getValue() instanceof Boolean on) {
            // Boolean option buttons carry their state in the message text; drawing it
            // as a switch instead makes a column of them scannable at a glance.
            VanillaButtonOverlay.addSwitch(getX(), getY(), width, height,
                    getMessage().getString(), isHoveredOrFocused(), active, getAlpha(), on);
        } else {
            VanillaButtonOverlay.add(getX(), getY(), width, height, getMessage().getString(),
                    isHoveredOrFocused(), active, getAlpha());
        }
        handleCursor(graphics);
        ci.cancel();
    }
}
