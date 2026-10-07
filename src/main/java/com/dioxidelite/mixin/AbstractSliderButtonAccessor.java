package com.dioxidelite.mixin;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code AbstractSliderButton.value} (normalised 0..1) so the themed slider
 * can draw where the handle actually sits instead of degrading to a plain button.
 */
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {

    @Accessor("value")
    double dioxidelite$getValue();
}
