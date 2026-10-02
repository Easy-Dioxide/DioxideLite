package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.ui.UiTheme;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import net.minecraft.util.Mth;

import java.awt.Color;

/**
 * 移植自 来源客户端 {@code features/render/Crosshair.java}：会随视角移动漂移、
 * 空闲时呼吸缩放的动态准星，绘制在 2D 层（Skija 画布，GUI 缩放坐标）。
 * <p>
 * 1:1 对应：Size 5.0/2..16/0.5（环半径 px）、Thickness 1.5/0.5..4/0.25、Center dot、
 * Color（Rainbow / Accent / Custom，对应 来源的 FeatureMode_239）、Follow 40/0..100/5 %、
 * Pulse；漂移数学沿用 来源：yaw 差值 * (Follow/100 * 1.1)，钳制 ±4px，按
 * {@code 1 - exp(-dt / 70ms)} 平滑。
 */
public final class Crosshair extends Module {

    // PORT-NOTE: 需要 mixin（Gui 的原版准星绘制）才能隐藏原版准星，本端口只实现了自定义准星的叠加绘制。

    public static final Crosshair INSTANCE = new Crosshair();

    /** 来源的 {@code FeatureMode_239}：彩虹微光 / 全局强调色 / 固定颜色。 */
    public enum ColorMode {
        RAINBOW,
        ACCENT,
        CUSTOM
    }

    public final DoubleSetting size = add(new DoubleSetting("Size", 5.0, 2.0, 16.0, 0.5));
    public final DoubleSetting thickness = add(new DoubleSetting("Thickness", 1.5, 0.5, 4.0, 0.25));
    public final BooleanSetting centerDot = add(new BooleanSetting("Center dot", true));
    public final EnumSetting<ColorMode> colorMode = add(new EnumSetting<>("Color", ColorMode.RAINBOW));
    public final ColorSetting customColor = add(new ColorSetting("Custom colour", new Color(255, 255, 255, 255)))
            .visibleWhen(() -> colorMode.is(ColorMode.CUSTOM));
    public final DoubleSetting follow = add(new DoubleSetting("Follow", 40.0, 0.0, 100.0, 5.0));
    public final BooleanSetting pulse = add(new BooleanSetting("Pulse", true));

    private static final long START_NANOS = System.nanoTime();
    /** 来源 把漂移钳制在 ±4px。 */
    private static final float MAX_DRIFT = 4.0F;
    /** 来源的平滑项：1 - exp(-dt / 70.0)。 */
    private static final float DRIFT_SMOOTH_MS = 70.0F;
    /** 来源的 Follow 系数：Follow / 100 * 1.1。 */
    private static final float FOLLOW_SCALE = 1.1F;
    /** 来源的呼吸动画：1 + 0.05 * sin(t)。 */
    private static final float PULSE_AMOUNT = 0.05F;
    private static final float PULSE_SPEED = 4.0F;
    /** 彩虹模式的色相角速度（来源 用 HSB(hue, 0.75, 1.0)）。 */
    private static final float RAINBOW_HUE_SPEED = 0.12F;

    private float driftX;
    private float lastYaw;
    private boolean hasLastYaw;
    private long lastFrameNanos;

    private Crosshair() {
        super("Crosshair", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        resetMotion();
    }

    @Override
    protected void onDisable() {
        resetMotion();
    }

    private void resetMotion() {
        driftX = 0.0F;
        hasLastYaw = false;
        lastFrameNanos = 0L;
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) {
            return;
        }
        // 来源的 I()：只在第一人称（thirdPersonView == 0）显示。
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        long now = System.nanoTime();
        float dtMs = lastFrameNanos == 0L
                ? 0.0F
                : Math.min((now - lastFrameNanos) / 1_000_000.0F, 100.0F);
        lastFrameNanos = now;
        updateDrift(dtMs);

        float time = (now - START_NANOS) / 1.0E9F;
        float radius = size.get().floatValue()
                * (pulse.get() ? 1.0F + PULSE_AMOUNT * Mth.sin(time * PULSE_SPEED) : 1.0F);
        float strokeWidth = thickness.get().floatValue();
        float centerX = event.width() / 2.0F + driftX;
        float centerY = event.height() / 2.0F;
        int color = resolveColor(time);

        Canvas canvas = event.canvas();
        Paint paint = new Paint().setAntiAlias(true);
        paint.setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth).setColor(color);
        canvas.drawCircle(centerX, centerY, Math.max(1.0F, radius), paint);
        if (centerDot.get()) {
            // 来源：中心点半径 max(thickness * 0.9, 0.75)。
            paint.setMode(PaintMode.FILL);
            canvas.drawCircle(centerX, centerY, Math.max(strokeWidth * 0.9F, 0.75F), paint);
        }
    }

    /**
     * 来源的漂移：目标 = clamp(-yawDelta * follow, ±4)，再按 dt 做指数平滑。
     * 反编译里 Y 轴目标恒为 0（直接衰减回中心），这里保持一致。
     */
    private void updateDrift(float dtMs) {
        float yaw = mc.player.getYRot();
        if (!hasLastYaw) {
            hasLastYaw = true;
            lastYaw = yaw;
            return;
        }
        float deltaYaw = Mth.wrapDegrees(yaw - lastYaw);
        lastYaw = yaw;

        float amount = follow.get().floatValue() / 100.0F * FOLLOW_SCALE;
        float target = Mth.clamp(-deltaYaw * amount, -MAX_DRIFT, MAX_DRIFT);
        float smoothing = dtMs <= 0.0F ? 1.0F : 1.0F - (float) Math.exp(-dtMs / DRIFT_SMOOTH_MS);
        driftX += (target - driftX) * smoothing;
    }

    private int resolveColor(float time) {
        return switch (colorMode.get()) {
            case RAINBOW -> {
                // 来源 case 0：ThemeSupport_064.L(hue, 0.75, 1.0, 255)。
                Color base = Color.getHSBColor((time * RAINBOW_HUE_SPEED) % 1.0F, 0.75F, 1.0F);
                yield new Color(base.getRed(), base.getGreen(), base.getBlue(), 255).getRGB();
            }
            case ACCENT -> UiTheme.accent();
            case CUSTOM -> customColor.argb();
        };
    }
}
