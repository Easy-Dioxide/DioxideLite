package com.dioxidelite.module.modules.combat.antibot;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/combat/antibot/AntiBotEngine.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Minecraft-independent copy of Clap's player-list/spawn correlation logic. */
public final class AntiBotEngine {
    public static final long SPAWN_ASSOCIATION_WINDOW_MILLIS = 500L;

    private final Map<UUID, Long> pendingPlayerSpawns = new HashMap<>();
    private final Set<Integer> botEntityIds = new HashSet<>();

    public void handlePlayerListAdd(List<AntiBotPlayerListEntry> entries, long nowMillis) {
        Objects.requireNonNull(entries, "entries");
        for (AntiBotPlayerListEntry entry : entries) {
            if (Objects.requireNonNull(entry, "entry").isSpawnCandidate()) {
                pendingPlayerSpawns.put(entry.profileId(), nowMillis);
            }
        }
    }

    public boolean handlePlayerSpawn(UUID profileId, int entityId) {
        Objects.requireNonNull(profileId, "profileId");
        if (pendingPlayerSpawns.remove(profileId) == null) {
            return false;
        }
        botEntityIds.add(entityId);
        return true;
    }

    public int handleEntitiesDestroyed(List<Integer> entityIds) {
        Objects.requireNonNull(entityIds, "entityIds");
        int removed = 0;
        for (Integer entityId : entityIds) {
            if (entityId != null && botEntityIds.remove(entityId)) {
                removed++;
            }
        }
        return removed;
    }

    public void tick(long nowMillis, boolean gameAvailable, int localPlayerAge) {
        if (!gameAvailable || localPlayerAge <= 1) {
            clearTracking();
            return;
        }
        pendingPlayerSpawns.entrySet().removeIf(
                entry -> nowMillis - entry.getValue() > SPAWN_ASSOCIATION_WINDOW_MILLIS);
    }

    public boolean isBot(CombatPlayerIdentity player, Set<UUID> networkPlayerUuids) {
        if (player == null || networkPlayerUuids == null) {
            return false;
        }
        return botEntityIds.contains(player.entityId())
                || !networkPlayerUuids.contains(player.profileId());
    }

    public Map<UUID, Long> pendingPlayerSpawns() {
        return Map.copyOf(pendingPlayerSpawns);
    }

    public Set<Integer> botEntityIds() {
        return Set.copyOf(botEntityIds);
    }

    public void clearTracking() {
        pendingPlayerSpawns.clear();
        botEntityIds.clear();
    }
}
