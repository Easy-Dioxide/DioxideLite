package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（来源开源版）com/dioxidelite/mixin/LocalPlayerAccessor.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
// ---------------------------------------------------------------------------


import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {

    @Invoker("sendPosition")
    void dioxidelite$sendPosition();
}
