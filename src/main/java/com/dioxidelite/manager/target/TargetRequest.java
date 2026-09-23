package com.dioxidelite.manager.target;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/manager/target/TargetRequest.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

public record TargetRequest(
        double range,
        float fov,
        boolean player,
        boolean mob,
        boolean animal,
        boolean villager,
        boolean invisible,
        Predicate<LivingEntity> extraFilter,
        int maxTargets
) {
    public TargetRequest {
        if (range < 0.0) range = 0.0;
        if (fov < 0.0f) fov = 0.0f;
        if (fov > 360.0f) fov = 360.0f;
        if (extraFilter == null) extraFilter = living -> true;
        if (maxTargets < 1) maxTargets = 1;
    }

    public static TargetRequest of(
            double range, float fov, boolean player, boolean mob, boolean animal,
            boolean villager, boolean invisible, int maxTargets) {
        return new TargetRequest(range, fov, player, mob, animal, villager, invisible, living -> true, maxTargets);
    }

    public static TargetRequest of(
            double range, float fov, boolean player, boolean mob, boolean animal,
            boolean villager, boolean invisible, Predicate<LivingEntity> extraFilter, int maxTargets) {
        return new TargetRequest(range, fov, player, mob, animal, villager, invisible, extraFilter, maxTargets);
    }
}
