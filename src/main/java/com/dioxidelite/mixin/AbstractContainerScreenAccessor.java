package com.dioxidelite.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@code leftPos} and {@code topPos} fields from
 * {@link AbstractContainerScreen} so other mixins can read the container
 * background position without shadowing inherited fields.
 */
// [DioxideLite 修复] accessor 前缀由 DioxideLite$ 统一为小写 dioxidelite$。
// 原大写形式是机械改名残留，且与移植代码的调用点不一致，会编译失败。
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int dioxidelite$getLeftPos();

    @Accessor("topPos")
    int dioxidelite$getTopPos();
}
