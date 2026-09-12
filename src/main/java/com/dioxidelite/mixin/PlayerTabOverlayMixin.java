package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.util.legendwatch.LegendSuffixUtil;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void DioxideLite$appendLegendarySuffix(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        if (!LegendWatch.INSTANCE.isEnabled() || !LegendWatch.INSTANCE.tabListEnabled()) return;
        String username = info.getTabListDisplayName() != null
                ? info.getTabListDisplayName().getString()
                : info.getProfile().name();
        cir.setReturnValue(LegendSuffixUtil.appendIfLegendary(cir.getReturnValue(), username));
    }
}
