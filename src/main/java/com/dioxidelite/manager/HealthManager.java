package com.dioxidelite.manager;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/manager/HealthManager.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.util.player.HealthDetectionUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Compatibility facade for the shared health source used by combat modules. */
public final class HealthManager {

    public static final HealthManager INSTANCE = new HealthManager();

    private HealthManager() {
    }

    public float getHealth(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return HealthDetectionUtils.getHealth(livingEntity);
        }
        return 0f;
    }
}
