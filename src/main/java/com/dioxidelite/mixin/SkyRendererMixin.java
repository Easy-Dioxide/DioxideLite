package com.dioxidelite.mixin;

import com.dioxidelite.module.modules.render.advanced.Skybox;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [v2.2.5 补全] Skybox 模块的真实实现：在 {@link SkyRenderer#renderSkyDisc(int)} 处
 * 把天空圆盘颜色替换为 Skybox 模块基于预设（CLOUDS / THUNDER / PULSAR）与时间驱动的
 * 动画色。模块关闭时原样返回，不影响原版天空。
 */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {

    @ModifyVariable(method = "renderSkyDisc", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int dioxidelite$skyColor(int skyColor) {
        Skybox skybox = Skybox.INSTANCE;
        return skybox.isActive() ? skybox.animatedSkyColor() : skyColor;
    }
}
