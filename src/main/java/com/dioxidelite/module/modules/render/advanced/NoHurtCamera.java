package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;

/**
 * 移植自 OpenOpal NoHurtCameraModule。
 * 禁用受伤时的镜头倾斜；可选禁用玩家模型受伤红光。
 */
public final class NoHurtCamera extends Module {

    public static final NoHurtCamera INSTANCE = new NoHurtCamera();

    public final BooleanSetting hideModelDamage = add(new BooleanSetting("No Player Model Hurt", false));

    private NoHurtCamera() {
        super("No Hurt Camera", Category.RENDER);
    }
}
