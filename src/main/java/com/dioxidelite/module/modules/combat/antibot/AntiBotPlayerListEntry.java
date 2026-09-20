package com.dioxidelite.module.modules.combat.antibot;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/combat/antibot/AntiBotPlayerListEntry.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.util.UUID;

public record AntiBotPlayerListEntry(
        UUID profileId,
        boolean displayNamePresent,
        int displayNameSiblingCount,
        CombatGameMode gameMode) {

    public AntiBotPlayerListEntry {
        if (displayNameSiblingCount < 0) {
            throw new IllegalArgumentException("displayNameSiblingCount must be non-negative");
        }
    }

    public boolean isSpawnCandidate() {
        return profileId != null
                && displayNamePresent
                && displayNameSiblingCount == 0
                && gameMode == CombatGameMode.SURVIVAL;
    }
}
