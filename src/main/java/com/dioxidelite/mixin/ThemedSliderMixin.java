package com.dioxidelite.mixin;

import com.dioxidelite.ui.screen.VanillaButtonOverlay;
import com.dioxidelite.ui.screen.VanillaScreenTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Options sliders extend {@code AbstractWidget.WithInactiveMessage}, not
 * {@code AbstractButton}, so {@link ThemedButtonMixin} never saw them and every
 * slider kept its vanilla grey track. Route them through the same themed button
 * overlay; the slider message already carries the formatted current value.
 */
@Mixin(AbstractSliderButton.class)
public abstract class ThemedSliderMixin extends AbstractWidget {

    protected ThemedSliderMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void DioxideLite$drawThemedSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                        float partialTick, CallbackInfo ci) {
        if (!VanillaScreenTheme.canReplaceVanillaButtons(Minecraft.getInstance().screen)) {
            return;
        }
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        // The slider is redrawn from scratch, so the normalised value has to travel
        // with it — otherwise the handle has nowhere sensible to sit.
        float value = (float) ((AbstractSliderButtonAccessor) this).dioxidelite$getValue();
        VanillaButtonOverlay.addSlider(self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                self.getMessage().getString(), self.isHoveredOrFocused(), self.active,
                self.getAlpha(), value);
        handleCursor(graphics);
        ci.cancel();
    }
}