package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;

/**
 * 移植自 OpenOpal NoFOVModule。
 * 锁定 FOV，禁用速度/缓行等效果带来的 FOV 变化。
 */
public final class NoFOV extends Module {

    public static final NoFOV INSTANCE = new NoFOV();

    private NoFOV() {
        super("No FOV", Category.RENDER);
    }
}
