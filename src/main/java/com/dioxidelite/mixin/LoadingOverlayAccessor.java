package com.dioxidelite.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the smoothed reload progress the vanilla overlay renders internally. */
// [DioxideLite 修复] accessor 前缀由 DioxideLite$ 统一为小写 dioxidelite$。
// 原大写形式是机械改名残留，且与移植代码的调用点不一致，会编译失败。
@Mixin(LoadingOverlay.class)
public interface LoadingOverlayAccessor {

    @Accessor("currentProgress")
    float dioxidelite$getCurrentProgress();
}
