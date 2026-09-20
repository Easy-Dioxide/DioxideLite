package com.dioxidelite.module.modules.combat.killauraplus;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/combat/killauraplus/LegitAuraCriticalState.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


public record LegitAuraCriticalState(
        double verticalVelocity,
        float fallDistance,
        boolean onGround,
        boolean touchingWater,
        boolean inLava,
        boolean climbing,
        boolean blindness,
        boolean hasVehicle,
        boolean flying,
        boolean sprinting,
        float attackCooldownPeriodTicks) {

    public LegitAuraCriticalState {
        if (!Double.isFinite(verticalVelocity)) {
            throw new IllegalArgumentException("verticalVelocity must be finite");
        }
        if (!Float.isFinite(fallDistance) || fallDistance < 0.0f) {
            throw new IllegalArgumentException("fallDistance must be finite and non-negative");
        }
        if (!Float.isFinite(attackCooldownPeriodTicks)) {
            throw new IllegalArgumentException("attackCooldownPeriodTicks must be finite");
        }
    }

    public boolean supportsAirCritical() {
        return !touchingWater
                && !inLava
                && !climbing
                && !blindness
                && !hasVehicle
                && !flying;
    }

    public boolean isCriticalNow(float cooldownProgress) {
        return supportsAirCritical()
                && !onGround
                && fallDistance > 0.0f
                && cooldownProgress > 0.9f
                && !sprinting;
    }
}
