package com.dioxidelite.module.modules.movement;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/movement/NoJumpDelay.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;

/** Removes the local player's vanilla jump input cooldown. */
public final class NoJumpDelay extends Module {

    public static final NoJumpDelay INSTANCE = new NoJumpDelay();

    private NoJumpDelay() {
        super("No Jump Delay", Category.MOVEMENT);
    }
}
