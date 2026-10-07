package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.IntSetting;

/**
 * 移植自 OpenOpal PostProcessingModule。
 * 后处理设置：模糊与泛光的开关和半径，供渲染管线查询使用。
 */
public final class PostProcessing extends Module {

    public static final PostProcessing INSTANCE = new PostProcessing();

    public final BooleanSetting blur = add(new BooleanSetting("Blur", true));
    public final IntSetting blurRadius = add(new IntSetting("Blur Radius", 7, 1, 20, 1)
            .visibleWhen(blur::get));

    public final BooleanSetting bloom = add(new BooleanSetting("Bloom", true)
            .visibleWhen(blur::get));
    public final IntSetting bloomRadius = add(new IntSetting("Bloom Radius", 7, 1, 20, 1)
            .visibleWhen(() -> blur.get() && bloom.get()));

    private PostProcessing() {
        super("Post Processing", Category.RENDER);
    }

    public boolean isBlur() {
        return isEnabled() && blur.get();
    }

    public boolean isBloom() {
        return isBlur() && bloom.get();
    }

    public int getBlurRadius() {
        return blurRadius.get();
    }

    public int getBloomRadius() {
        return bloomRadius.get();
    }
}
