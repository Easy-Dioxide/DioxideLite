package com.dioxidelite.module.modules.movement.invmove;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/movement/invmove/InvMoveContext.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


public interface InvMoveContext {
    boolean isAvailable();

    InvMoveScreen screen();

    boolean isPhysicalKeyPressed(InvMoveKey key);

    void setLogicalKeyPressed(InvMoveKey key, boolean pressed);

    void sendCloseInventoryPacket();
}
