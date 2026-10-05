package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;

import java.awt.Color;
import net.minecraft.util.Mth;

/**
 * 移植自 来源客户端 {@code features/render/Skybox.java}（"Animated sky with a fixed custom color"）。
 * <p>
 * 1:1 对应的设置与默认值：
 * <ul>
 *   <li>Preset（CLOUDS / THUNDER / PULSAR，默认 CLOUDS）——来源 {@code FeatureMode_240}，着色器用 ordinal；</li>
 *   <li>Color（0xFF7180FF，固定天空色）；</li>
 *   <li>Animation Speed（1.0, 0.1..5.0, 0.1，"x"）、Scale（5.0, 1.0..20.0, 0.5）、
 *       Intensity（1.0, 0.1..5.0, 0.1，"%"）；</li>
 *   <li>Strike Interval（4.0, 1.0..10.0, 0.5，"s"）、Strike Chance（0.65, 0.0..1.0, 0.05）、
 *       Strike Glow（1.0, 0.1..3.0, 0.1，"x"）——仅 THUNDER 预设可见；</li>
 *   <li>Quality（FAST / BALANCED / DETAILED，默认 BALANCED）——来源 {@code FeatureMode_303}，
 *       着色器用的层数是 3 / 4 / 6；Resolution（50.0, 25.0..100.0, 25.0，"%"）。</li>
 * </ul>
 * 来源 里这些值全部交给 {@code custom_sky} 着色器在天空阶段绘制（{@code RenderSupport_105}），
 * 暴露的 getter 与 来源原类一致，方便后续在天空 hook 里直接取用。
 */
// [v2.2.5 补全] 已通过 SkyRendererMixin 接入 SkyRenderer.renderSkyDisc：
// 启用时把天空圆盘颜色替换为 animatedSkyColor()（CLOUDS 波动 / THUNDER 压暗+闪电 / PULSAR 脉动）。
public final class Skybox extends Module {

    public static final Skybox INSTANCE = new Skybox();

    private static final long START_NANOS = System.nanoTime();

    /** 来源 {@code FeatureMode_240}：预设。着色器读 ordinal，风暴参数只对 THUNDER 生效。 */
    public enum Preset {
        CLOUDS,
        THUNDER,
        PULSAR
    }

    /** 来源 {@code FeatureMode_303}：画质。{@code layers} 是 来源 传给着色器的噪声层数。 */
    public enum Quality {
        FAST(3),
        BALANCED(4),
        DETAILED(6);

        private final int layers;

        Quality(int layers) {
            this.layers = layers;
        }

        public int layers() {
            return layers;
        }
    }

    public final EnumSetting<Preset> preset = add(new EnumSetting<>("Preset", Preset.CLOUDS));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0xFF7180FF, true)));
    public final DoubleSetting animationSpeed = add(new DoubleSetting("Animation Speed", 1.0, 0.1, 5.0, 0.1));
    public final DoubleSetting scale = add(new DoubleSetting("Scale", 5.0, 1.0, 20.0, 0.5)
            .visibleWhen(this::presetHasPattern));
    public final DoubleSetting intensity = add(new DoubleSetting("Intensity", 1.0, 0.1, 5.0, 0.1)
            .visibleWhen(this::presetHasPattern));
    public final DoubleSetting strikeInterval = add(new DoubleSetting("Strike Interval", 4.0, 1.0, 10.0, 0.5)
            .visibleWhen(() -> preset.is(Preset.THUNDER)));
    public final DoubleSetting strikeChance = add(new DoubleSetting("Strike Chance", 0.65, 0.0, 1.0, 0.05)
            .visibleWhen(() -> preset.is(Preset.THUNDER)));
    public final DoubleSetting strikeGlow = add(new DoubleSetting("Strike Glow", 1.0, 0.1, 3.0, 0.1)
            .visibleWhen(() -> preset.is(Preset.THUNDER)));
    public final EnumSetting<Quality> quality = add(new EnumSetting<>("Quality", Quality.BALANCED));
    public final DoubleSetting resolution = add(new DoubleSetting("Resolution", 50.0, 25.0, 100.0, 25.0));

    private Skybox() {
        super("Skybox", Category.RENDER);
    }

    /** 来源 {@code FeatureMode_240.L()Z}：Scale / Intensity 只在这种预设下可见。 */
    private boolean presetHasPattern() {
        return preset.is(Preset.THUNDER) || preset.is(Preset.PULSAR);
    }

    public boolean isActive() {
        return isEnabled();
    }

    /** 来源 {@code Skybox.d()}：预设序号（着色器 uniform uParams 里的 preset）。 */
    public int presetId() {
        return preset.get().ordinal();
    }

    /** 来源 {@code Skybox.h()}：自类加载以来的秒数，天空动画的时间基准。 */
    public float animationTime() {
        return (System.nanoTime() - START_NANOS) / 1.0E9F;
    }

    /** 来源 {@code Skybox.D()}：固定天空色（ARGB）。 */
    public int skyColorArgb() {
        return color.argb();
    }

    public float animationSpeed() {
        return animationSpeed.get().floatValue();
    }

    public float scale() {
        return scale.get().floatValue();
    }

    /** 来源 {@code Skybox.I()}：Intensity / 100。 */
    public float intensityFraction() {
        return intensity.get().floatValue() / 100.0F;
    }

    /** 来源 {@code Skybox.k()}：Strike Interval（秒）。 */
    public float strikeInterval() {
        return strikeInterval.get().floatValue();
    }

    /** 来源 {@code Skybox.i()}：Strike Chance（0..1）。 */
    public float strikeChance() {
        return strikeChance.get().floatValue();
    }

    /** 来源 {@code Skybox.D()}：Strike Glow（倍率）。 */
    public float strikeGlow() {
        return strikeGlow.get().floatValue();
    }

    /** 来源 {@code Skybox.L()}（Quality 重载）：噪声层数 3 / 4 / 6。 */
    public int qualityLayers() {
        return quality.get().layers();
    }

    /** 来源 {@code Skybox.d()}（Resolution 重载）：分辨率比例，钳制在 0.25..1.0。 */
    public float resolutionFraction() {
        return (float) Math.clamp(resolution.get() / 100.0, 0.25, 1.0);
    }

    /** 来源的常量 {@code Skybox.l()}。 */
    public float fixedDarkness() {
        return 0.85F;
    }

    /**
     * [v2.2.5 补全] 基于预设与时间驱动的动画天空色（ARGB）。模块启用时由
     * SkyRendererMixin 把 renderSkyDisc 的天空颜色替换为该方法返回值。
     * <ul>
     *   <li>CLOUDS：在基底色上做轻微亮度波动（云流动感）；</li>
     *   <li>THUNDER：整体压暗，并按 Strike Interval 周期性闪电闪亮（Strike Glow 控制强度）；</li>
     *   <li>PULSAR：亮度随 Animation Speed 脉动。</li>
     * </ul>
     */
    public int animatedSkyColor() {
        float t = animationTime() * Math.max(animationSpeed(), 0.01F);
        int argb = color.argb();
        float r = (float) ((argb >> 16) & 0xFF);
        float g = (float) ((argb >> 8) & 0xFF);
        float b = (float) (argb & 0xFF);
        float intensity = 1.0F;
        switch (preset.get()) {
            case CLOUDS:
                intensity = 1.0F + 0.06F * Mth.sin(t * 1.5F);
                break;
            case THUNDER:
                intensity = 0.55F + strikeFlash();
                break;
            case PULSAR:
                intensity = 0.78F + 0.25F * (0.5F + 0.5F * Mth.sin(t * 2.0F));
                break;
        }
        intensity = Mth.clamp(intensity, 0.0F, 3.0F);
        r = Mth.clamp(r * intensity, 0.0F, 255.0F);
        g = Mth.clamp(g * intensity, 0.0F, 255.0F);
        b = Mth.clamp(b * intensity, 0.0F, 255.0F);
        return (0xFF << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
    }

    /** 雷暴预设的闪电闪亮：按 Strike Interval 周期性点亮一次，Strike Glow 控制强度。 */
    private float strikeFlash() {
        float interval = Math.max(strikeInterval(), 0.1F);
        float cycle = (animationTime() % interval) / interval;
        if (cycle > 0.55F && cycle < 0.65F) {
            return 0.6F * Math.max(strikeGlow(), 0.0F);
        }
        return 0.0F;
    }
}
