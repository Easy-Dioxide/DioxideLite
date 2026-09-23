package com.dioxidelite.accessor;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/accessor/StrafeJumpPoseAccess.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


/** Render-only view of the virtual pose reported by Speed's 45-degree mode. */
public interface StrafeJumpPoseAccess {

    float dioxidelite$getVisualBodyOffset(float partialTick);

    float dioxidelite$getVisualHeadOffset(float partialTick);

    boolean dioxidelite$isSynchronizedStrafeTick();
}
