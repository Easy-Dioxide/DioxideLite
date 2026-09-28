package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import net.minecraft.client.renderer.fog.FogRenderer;

/**
 * 移植自 来源客户端 {@code features/render/FogRemove.java}（"Removes first-person environmental and distance fog"）。
 * <p>
 * 来源的做法是在雾设置处把 {@code fogStart=8.5070587E37}、{@code fogEnd=1.7014117E38}、
 * {@code fogDensity=0}（即把距离雾推到不可能到达的远处）。26.1.2 的雾统一由
 * {@link FogRenderer} 生成，原版自带一个等价的用户开关 {@link FogRenderer#toggleFog()}：
 * 关闭后 {@code getBuffer(...)} 返回全零缓冲，地形距离雾、天空雾和第一人称环境雾（水/岩浆/细雪）
 * 一起消失，因此这里直接复用原版开关，进入时记录原状态、退出时还原，不需要额外 mixin。
 */
public final class FogRemove extends Module {

    public static final FogRemove INSTANCE = new FogRemove();

    /** 进入模块前雾是否处于开启状态，用于退出时还原。 */
    private boolean fogWasEnabled = true;
    /** 本模块是否已经动过 {@link FogRenderer} 的雾开关。 */
    private boolean fogDisabled;

    private FogRemove() {
        super("FogRemove", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        if (fogDisabled) {
            return;
        }
        // toggleFog() 返回切换后的状态，所以切换前的状态就是它的取反。
        boolean wasEnabled = !FogRenderer.toggleFog();
        if (!wasEnabled) {
            // 雾此前就被（原版 F3+F 等）关掉了：再切一次，保持关闭，只记住原状态。
            FogRenderer.toggleFog();
        }
        fogWasEnabled = wasEnabled;
        fogDisabled = true;
    }

    @Override
    protected void onDisable() {
        if (!fogDisabled) {
            return;
        }
        fogDisabled = false;
        // 只在进入时雾是开着的场合还原；如果期间用户自己按过原版雾开关，这里以“退出即恢复开启”为准。
        if (fogWasEnabled) {
            FogRenderer.toggleFog();
        }
    }
}
