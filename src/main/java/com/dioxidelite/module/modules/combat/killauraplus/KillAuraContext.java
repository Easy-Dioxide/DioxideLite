package com.dioxidelite.module.modules.combat.killauraplus;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/combat/killauraplus/KillAuraContext.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.util.List;

public interface KillAuraContext {
    boolean isAvailable();

    boolean isScreenOpen();

    boolean isSupportedWeaponHeld();

    boolean isSwordHeld();

    boolean isInteractionAvailable();

    float playerYaw();

    float playerPitch();

    float attackCooldownProgress();

    LegitAuraCriticalState criticalState();

    List<KillAuraTargetSnapshot> targets(double searchRange);

    void requestRotation(float yaw, float pitch, float speed);

    boolean rotationRayHitsTarget(int entityId, double reach);

    void attackEntity(int entityId);

    void swingMainHand();
}
