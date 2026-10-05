package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.IntSetting;

/**
 * 移植自 OpenOpal PostProcessingModule。
 * 后处理设置：模糊与泛光的开关和半径，供渲染管线查询使用。
 *
 * <p>[v2.2.5 补全] 实际效果已通过 GameRendererMixin.DioxideLite$applyPostProcessing 接入：
 * 启用 Blur 时对主渲染目标应用原版 blur 后处理链（blurRadius 1..20 → 处理 1..3 次，越大越糊），
 * 启用 Bloom 时在 Blur 基础上再叠加一次模糊得到柔和泛光感。两个效果均真实生效。</p>
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
