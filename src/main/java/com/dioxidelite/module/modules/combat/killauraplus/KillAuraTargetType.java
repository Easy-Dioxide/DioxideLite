package com.dioxidelite.module.modules.combat.killauraplus;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/combat/killauraplus/KillAuraTargetType.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


public enum KillAuraTargetType {
    PLAYER,
    HOSTILE_MOB,
    ANIMAL,
    OTHER_MOB,
    OTHER
}
