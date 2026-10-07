package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;

/**
 * 移植自 来源客户端 <code>features/render/FogBlur</code>
 * （"Blurs the world based on distance from the camera"）。
 *
 * <p>来源原版本体只是一个设置容器：真正的画面处理在
 * <code>来源客户端.render.RenderSupport_098</code> 里，是一整套 framebuffer 后处理 pass
 * （半分辨率降采样 custom_fog_down / 升采样 custom_fog_up / 合成 custom_fog_composite，
 * 用深度纹理做距离衰减，uniform <code>uFog = (Distance, Fade, Tint, ClientColor)</code>，
 * 并在 Zoom 有效时按缩放量让强度从 1 淡出到 0）。</p>
 *
 * <p>MC 26.1.2 的 Dioxide 端没有暴露世界后处理 pass 的注册/绘制入口，
 * 因此本端口保留 1:1 的设置面 + 与来源 传参顺序一致的 {@link #fogUniforms()}，
 * 供以后接入 world post-effect hook 时直接读取。</p>
 */
// PORT-NOTE: 需要 GameRenderer/Framebuffer 后处理 hook（world post-effect + 两级 blur shader + 深度纹理）；本端口只实现了设置项、fogUniforms() 参数打包与 shouldBlur() 状态查询。
// 说明：屏幕后处理模糊本身没有实现 —— Dioxide 端没有暴露世界后处理 pass 的注册/绘制入口。
public final class FogBlur extends Module {

    public static final FogBlur INSTANCE = new FogBlur();

    /** 来源: Distance 24.0 (4.0..256.0, 1.0, 单位 m)。 */
    public final DoubleSetting distance = add(new DoubleSetting("Distance", 24.0, 4.0, 256.0, 1.0)
            .displayAs("Distance (m)"));

    /** 来源: Fade 48.0 (4.0..256.0, 1.0, 单位 m)。 */
    public final DoubleSetting fade = add(new DoubleSetting("Fade", 48.0, 4.0, 256.0, 1.0)
            .displayAs("Fade (m)"));

    /** 来源: Client Color，默认开。关闭后不吃客户端主题配色。 */
    public final BooleanSetting clientColor = add(new BooleanSetting("Client Color", true));

    /** 来源: Tint 0.5 (0.05..0.95, 0.05)，仅在 Client Color 打开时可见。 */
    public final DoubleSetting tint = add(new DoubleSetting("Tint", 0.5, 0.05, 0.95, 0.05)
            .visibleWhen(clientColor::get));

    /** 来源: Disable on zoom，默认开；Zoom 生效时模糊强度按缩放量淡出。 */
    public final BooleanSetting disableOnZoom = add(new BooleanSetting("Disable on zoom", true));

    private FogBlur() {
        super("Fog Blur", Category.RENDER);
    }

    /** 后处理 pass 的请求开关（世界模糊 hook 接入后由该 hook 查询）。 */
    public boolean shouldBlur() {
        return isEnabled();
    }

    /**
     * 来源 <code>RenderSupport_098</code> 里 <code>uFog</code> 的四个分量，顺序保持一致：
     * {@code [Distance, Fade, Tint, ClientColor ? 1 : 0]}。
     * Distance/Fade 单位为方块，Tint 为 0..1 的配色混合系数。
     */
    public float[] fogUniforms() {
        return new float[] {
                distance.get().floatValue(),
                fade.get().floatValue(),
                tint.get().floatValue(),
                clientColor.get() ? 1.0F : 0.0F
        };
    }
}
