package com.dioxidelite.mixin;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/mixin/LocalPlayerAccessor.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {

    @Invoker("sendPosition")
    void dioxidelite$sendPosition();
}
