package com.dioxidelite.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// [DioxideLite 修复] accessor 前缀由 DioxideLite$ 统一为小写 dioxidelite$。
// 原大写形式是机械改名残留，且与移植代码的调用点不一致，会编译失败。
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {

    @Accessor("key")
    InputConstants.Key dioxidelite$getKey();
}
