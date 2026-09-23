package com.dioxidelite.module.modules.combat.killauraplus;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/combat/killauraplus/KillAuraTargetEspStyle.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.awt.Color;
import java.util.Objects;

public record KillAuraTargetEspStyle(
        float radius,
        float alpha,
        Color sideColor,
        Color lineColor) {

    public KillAuraTargetEspStyle {
        if (!Float.isFinite(radius) || radius < 0.0f) {
            throw new IllegalArgumentException("radius must be finite and non-negative");
        }
        if (!Float.isFinite(alpha) || alpha < 0.0f) {
            throw new IllegalArgumentException("alpha must be finite and non-negative");
        }
        Objects.requireNonNull(sideColor, "sideColor");
        Objects.requireNonNull(lineColor, "lineColor");
    }
}
