package com.dioxidelite.module.modules.combat.antibot;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/combat/antibot/CombatPlayerIdentity.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.util.Objects;
import java.util.UUID;

public record CombatPlayerIdentity(int entityId, UUID profileId) {
    public CombatPlayerIdentity {
        Objects.requireNonNull(profileId, "profileId");
    }
}
