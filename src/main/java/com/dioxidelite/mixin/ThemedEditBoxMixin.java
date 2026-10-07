package com.dioxidelite.mixin;

import com.dioxidelite.ui.screen.VanillaButtonOverlay;
import com.dioxidelite.ui.screen.VanillaScreenTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers themed text fields with the overlay without taking over their painting.
 *
 * <p>Every other widget here is redrawn from scratch, but an {@code EditBox} owns a
 * caret position, a selection and a scrolled view that Minecraft keeps private.
 * Suppressing its pass would mean re-implementing all three and losing them the
 * moment they drift from the original. So the vanilla pass stays and the overlay
 * only lays a themed frame on top, which is enough to stop the field from reading
 * as a stock black box on an otherwise branded screen.</p>
 */
@Mixin(EditBox.class)
public abstract class ThemedEditBoxMixin {

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void DioxideLite$registerThemedField(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                 float partialTick, CallbackInfo ci) {
        EditBox self = (EditBox) (Object) this;
        if (!self.isVisible() || !self.isBordered()) {
            return;
        }
        if (!VanillaScreenTheme.canReplaceVanillaButtons(Minecraft.getInstance().screen)) {
            return;
        }
        VanillaButtonOverlay.addField(self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                self.isFocused(), self.isActive(), self.getAlpha());
    }
}
