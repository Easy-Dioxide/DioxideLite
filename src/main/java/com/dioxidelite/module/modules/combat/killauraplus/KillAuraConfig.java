package com.dioxidelite.module.modules.combat.killauraplus;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/combat/killauraplus/KillAuraConfig.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.util.Objects;

public record KillAuraConfig(
        KillAuraMode mode,
        KillAuraPriority priority,
        KillAuraRotationMode rotationMode,
        KillAuraCriticalMode criticalMode,
        double rotationRange,
        int rotationSpeed,
        double range,
        int fov,
        boolean onlyWeapon,
        boolean swing,
        boolean fakeBlock,
        boolean players,
        boolean mobs,
        boolean animals,
        boolean invisibles,
        boolean ignoreTeam,
        boolean ignoreFriends) {

    public KillAuraConfig {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(rotationMode, "rotationMode");
        Objects.requireNonNull(criticalMode, "criticalMode");
    }
}
