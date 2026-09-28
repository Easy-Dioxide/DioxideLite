package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.RespawnEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import io.github.humbleui.skija.Canvas;
import net.minecraft.util.Mth;

import java.awt.Color;

/**
 * 受伤染色（移植自 来源客户端 {@code features/render/Hurtcam}）。
 *
 * <p>来源原实现：受击时用 <code>custom_post</code> / <code>custom_hurt</code> 后处理 pass 在帧缓冲上叠一层
 * 从屏幕边缘向内的颜色 wash，uniform 为 <code>uTint</code>(颜色 RGB)、
 * <code>uStrength</code>(Size% * 颜色 alpha)、<code>uReach</code>(当前染色量 * Intensity%)、
 * <code>uBias</code>(受击方向，来自 <code>player.getHurtDir()</code>)、
 * <code>uDirectional</code>(Directional 开关)。</p>
 *
 * <p>本端口用等价的 2D 层实现：{@link Render2DEvent} 的 Skija 画布上，从屏幕四边向内画线性渐隐带，
 * 带厚 = uReach（默认 45% 屏高/宽的一半），边缘浓度 = uStrength（默认 55%），
 * Directional 打开时按"伤害来源方位"给四边加权（伤害方向 = 玩家 yaw + hurtDir，换算到相机相对方位）。</p>
 */
// PORT-NOTE: 需要 GameRenderer/Framebuffer 后处理 hook（custom_post/custom_hurt shader pass 的注册与 uniform 上传）；本端口只实现了等价的 2D 层受损染色（Render2DEvent + Skija 画布四边向内渐隐，Directional 时按受击方位加权），washUniforms() 保留同一组 uniform 参数供以后接入后处理 hook。
public final class Hurtcam extends Module {

    public static final Hurtcam INSTANCE = new Hurtcam();

    /** 来源: Color，默认不透明红 (0xFFFF4040)，提示语 "Tint of the damage wash"。 */
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFFF4040, true)));

    /** 来源: Intensity 45.0 (5.0..100.0, 5.0, "%")，提示语 "How strong the wash gets at full health loss"。 */
    public final DoubleSetting intensity = add(new DoubleSetting("Intensity", 45.0, 5.0, 100.0, 5.0));

    /** 来源: Size 55.0 (10.0..100.0, 5.0, "%")，提示语 "How far in from the edge the wash reaches"。 */
    public final DoubleSetting size = add(new DoubleSetting("Size", 55.0, 10.0, 100.0, 5.0));

    /** 来源: Duration 450.0 (100.0..1500.0, 50.0, "ms")，提示语 "How long the wash takes to fade out"。 */
    public final DoubleSetting duration = add(new DoubleSetting("Duration", 450.0, 100.0, 1500.0, 50.0));

    /** 来源: "Scale with damage"，默认 true，提示语 "Bigger hits flash brighter"。 */
    public final BooleanSetting scaleWithDamage = add(new BooleanSetting("Scale with damage", true));

    /** 来源: "Directional"，默认 false，提示语 "Bias the wash towards where the hit came from"。 */
    public final BooleanSetting directional = add(new BooleanSetting("Directional", false));

    /** 来源的静态常量 <code>j = 100.0f</code>（百分比换算）。 */
    private static final float PERCENT = 100.0F;

    /** 来源: 染色量低于 0.002 时直接跳过。 */
    private static final float MIN_WASH = 0.002F;

    /** 来源: 单帧 dt 上限 100ms（防卡顿导致一次跳完）。 */
    private static final float MAX_DELTA_MS = 100.0F;

    /** 来源 成员 f：当前染色量 0..1。 */
    private float wash;

    /** 来源 成员 e：本次受击的闪光强度。 */
    private float flash;

    /** 来源 成员 m：受击瞬间记录的 <code>hurtDir</code>。 */
    private float bias;

    /** 来源 成员 l：上一帧的 hurtTime，用于检测新的一次受击。 */
    private int lastHurtTime;

    /** 来源 成员 k：上一帧的 nanoTime。 */
    private long lastFrameNanos;

    private Hurtcam() {
        super("Hurtcam", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        reset();
    }

    @Override
    protected void onDisable() {
        reset();
    }

    /** 来源的 F()：清空全部状态。 */
    private void reset() {
        wash = 0.0F;
        flash = 0.0F;
        bias = 0.0F;
        lastHurtTime = 0;
        lastFrameNanos = 0L;
    }

    /** 来源的 WorldChangeEvent 处理：换世界 / 重生后重置状态。 */
    @Listen
    private void onRespawn(RespawnEvent event) {
        reset();
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) {
            return;
        }
        update();
        if (wash <= MIN_WASH) {
            return;
        }
        renderWash(event);
    }

    /** 来源的 h()：按帧推进染色量（受击检测 + 线性淡出）。 */
    private void update() {
        long now = System.nanoTime();
        float deltaMs = lastFrameNanos == 0L
                ? 16.0F
                : Math.min((now - lastFrameNanos) / 1_000_000.0F, MAX_DELTA_MS);
        lastFrameNanos = now;

        int hurtTime = mc.player.hurtTime;
        if (hurtTime > lastHurtTime) {
            flash = 1.0F;
            if (scaleWithDamage.get()) {
                // 来源: clamp(hurtTime / max(1, maxHurtTime), 0.35, 1)
                flash = Mth.clamp(hurtTime / Math.max(1.0F, mc.player.hurtDuration), 0.35F, 1.0F);
            }
            wash = Math.max(wash, flash);
            bias = mc.player.getHurtDir();
        }
        lastHurtTime = hurtTime;

        float fadeMs = duration.get().floatValue();
        wash = fadeMs <= 0.0F ? 0.0F : Math.max(0.0F, wash - deltaMs / fadeMs);
    }

    /** 在 2D 画布上绘制等效于 来源 后处理 wash 的四边渐隐染色。 */
    private void renderWash(Render2DEvent event) {
        float width = event.width();
        float height = event.height();
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        Canvas canvas = event.canvas();
        float[] uniforms = washUniforms();
        float strength = uniforms[0];
        float reach = uniforms[1];

        // uReach 越大，染色从边缘向内的覆盖越深（1.0 = 较小屏幕边的一半）。
        float band = Math.max(1.0F, reach * Math.min(width, height) * 0.5F);
        Color tint = color.get();
        int edgeColor = withAlpha(tint, strength);
        int clearColor = edgeColor & 0x00FFFFFF;

        float left = 1.0F;
        float right = 1.0F;
        float top = 1.0F;
        float bottom = 1.0F;
        if (directional.get()) {
            // 伤害来源的世界方位 = 玩家 yaw + hurtDir（MC 约定），再换算成相机相对方位。
            float bearing = mc.player.getYRot() + bias;
            float relative = Mth.wrapDegrees(bearing - mc.gameRenderer.getMainCamera().yRot());
            float dirX = -Mth.sin(Math.toRadians(relative)); // > 0 表示在屏幕右侧
            float dirY = -Mth.cos(Math.toRadians(relative)); // > 0 表示在屏幕下方
            left = edgeWeight(-dirX);
            right = edgeWeight(dirX);
            top = edgeWeight(-dirY);
            bottom = edgeWeight(dirY);
        }

        SkijaUi.gradient(canvas, 0.0F, 0.0F, width, band,
                scaleAlpha(edgeColor, top), clearColor, true, 0.0F);
        SkijaUi.gradient(canvas, 0.0F, height - band, width, band,
                clearColor, scaleAlpha(edgeColor, bottom), true, 0.0F);
        SkijaUi.gradient(canvas, 0.0F, 0.0F, band, height,
                scaleAlpha(edgeColor, left), clearColor, false, 0.0F);
        SkijaUi.gradient(canvas, width - band, 0.0F, band, height,
                clearColor, scaleAlpha(edgeColor, right), false, 0.0F);
    }

    /**
     * 来源 <code>custom_hurt</code> 的 uniform 打包，顺序与后处理 pass 一致：
     * <code>[uStrength, uReach, uBias, uDirectional]</code>。
     * 未接后处理 hook 时，2D 染色直接使用同一组数值。
     */
    public float[] washUniforms() {
        float strength = size.get().floatValue() / PERCENT * (color.get().getAlpha() / 255.0F);
        float reach = wash * (intensity.get().floatValue() / PERCENT);
        return new float[] { strength, reach, bias, directional.get() ? 1.0F : 0.0F };
    }

    /** 当前染色量 0..1（来源 成员 f）。 */
    public float wash() {
        return wash;
    }

    /** 朝向某条边的加权：0.25..1.0，保留 25% 底噪以免对面完全消失。 */
    private static float edgeWeight(float component) {
        return 0.25F + 0.75F * Mth.clamp(0.5F + 0.5F * component, 0.0F, 1.0F);
    }

    /** 用 0..1 的 alpha 比例生成 ARGB。 */
    private static int withAlpha(Color color, float alphaFraction) {
        int alpha = Math.round(255.0F * Mth.clamp(alphaFraction, 0.0F, 1.0F));
        return (alpha << 24) | (color.getRGB() & 0x00FFFFFF);
    }

    /** 在保留 RGB 的前提下缩放现有 alpha。 */
    private static int scaleAlpha(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0.0F, 1.0F));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
