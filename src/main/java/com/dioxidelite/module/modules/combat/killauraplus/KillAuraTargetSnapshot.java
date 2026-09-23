package com.dioxidelite.module.modules.combat.killauraplus;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/combat/killauraplus/KillAuraTargetSnapshot.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.util.Objects;

public record KillAuraTargetSnapshot(
        int entityId,
        KillAuraTargetType type,
        boolean localPlayer,
        boolean alive,
        boolean removed,
        boolean invisible,
        boolean visible,
        boolean friend,
        boolean teammate,
        boolean antiBot,
        double closestDistance,
        double distanceSquared,
        float health,
        float targetYaw,
        float targetPitch) {

    public KillAuraTargetSnapshot {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(closestDistance) || closestDistance < 0.0) {
            throw new IllegalArgumentException("closestDistance must be finite and non-negative");
        }
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0) {
            throw new IllegalArgumentException("distanceSquared must be finite and non-negative");
        }
        if (!Float.isFinite(health) || health < 0.0f) {
            throw new IllegalArgumentException("health must be finite and non-negative");
        }
        if (!Float.isFinite(targetYaw) || !Float.isFinite(targetPitch)) {
            throw new IllegalArgumentException("target rotation must be finite");
        }
    }
}
