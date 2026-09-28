package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.util.render.ColorUtils;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * 头顶的锥形帽子（移植自 来源客户端 {@code ChinaHat}）。
 * <p>
 * 几何与 来源客户端 的 {@code RenderSupport_142}/{@code RenderSupport_144} 对应：帽体按 8 段高度、
 * 48 分段绕圈插值，半径 {@code r(f) = R * (1 - f)}、高度 {@code h(f) = H * (f^1.35 + 0.12 * (1-f)^5)}，
 * 顶点颜色支持 STATIC / GRADIENT / RAINBOW 三种模式；Glow 在帽檐外再叠一圈低透明度光晕，
 * Trail 画出绕帽旋转的亮线和身后的渐隐拖尾，Through walls 切换深度测试管线。
 * 帽子挂在玩家头顶（基础高度 = 碰撞箱高度 + Offset，潜行时再压低 0.22），并跟随头部偏航与
 * Tilt% 的俯仰。全部几何都在 Render3D 层的世界空间绘制，仅在第三人称可见（与 来源客户端 一致）。
 */
public final class ChinaHat extends Module {

    public static final ChinaHat INSTANCE = new ChinaHat();

    /** 来源客户端 RenderMode_140：单色 / 双色渐变 / 绕圈彩虹。 */
    public enum ColorMode {
        STATIC,
        GRADIENT,
        RAINBOW
    }

    /** 绕圈分段数（来源客户端 用 48）。 */
    private static final int SEGMENTS = 48;
    /** 帽体高度分段数（来源客户端 用 8）。 */
    private static final int BANDS = 8;
    /** 拖尾的分段数。 */
    private static final int WAKE_SEGMENTS = 24;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    /** 潜行时帽子压低的高度（来源客户端: 0.22）。 */
    private static final float CROUCH_DROP = 0.22F;
    /** 帽檐描边的最小宽度（相对帽半径自适应的下限）。 */
    private static final float RIM_WIDTH = 0.02F;
    /** 扫描线宽度（相对帽半径）。 */
    private static final float SWEEP_WIDTH = 0.05F;
    /** Glow 光晕相对帽檐的外扩比例（来源客户端: 1.07）。 */
    private static final float GLOW_SCALE = 1.07F;
    /** 帽体表面的透明度系数（来源客户端: 0.7 * Opacity）。 */
    private static final float BODY_ALPHA = 0.7F;
    /** Glow 光晕的透明度系数（来源客户端: 0.22 * Opacity）。 */
    private static final float GLOW_ALPHA = 0.22F;

    private static final RenderType HAT_THROUGH_WALLS = createRenderType(
            "pipeline/custom_china_hat_through_walls",
            "DioxideLite_custom_china_hat_through_walls",
            CompareOp.ALWAYS_PASS);
    private static final RenderType HAT_DEPTH_TESTED = createRenderType(
            "pipeline/custom_china_hat_depth_tested",
            "DioxideLite_custom_china_hat_depth_tested",
            CompareOp.LESS_THAN_OR_EQUAL);

    public final DoubleSetting radius = add(new DoubleSetting("Radius", 0.65, 0.2, 3.0, 0.05));
    public final DoubleSetting height = add(new DoubleSetting("Height", 0.35, 0.05, 1.5, 0.05));
    public final DoubleSetting offset = add(new DoubleSetting("Offset", -0.02, -0.5, 1.5, 0.02));
    public final DoubleSetting tilt = add(new DoubleSetting("Tilt", 100.0, 0.0, 100.0, 5.0));
    public final EnumSetting<ColorMode> colors = add(new EnumSetting<>("Colors", ColorMode.STATIC));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(100, 175, 255, 255)));
    public final ColorSetting secondColor = add(new ColorSetting("Second color", new Color(180, 120, 255, 255))
            .visibleWhen(() -> colors.is(ColorMode.GRADIENT)));
    public final DoubleSetting colorSpeed = add(new DoubleSetting("Color speed", 40.0, 0.0, 360.0, 5.0)
            .visibleWhen(() -> !colors.is(ColorMode.STATIC)));
    public final DoubleSetting opacity = add(new DoubleSetting("Opacity", 90.0, 5.0, 100.0, 1.0));
    public final DoubleSetting spinSpeed = add(new DoubleSetting("Spin speed", 65.0, 0.0, 360.0, 5.0));
    public final BooleanSetting glow = add(new BooleanSetting("Glow", true));
    public final BooleanSetting trail = add(new BooleanSetting("Trail", true));
    public final DoubleSetting trailLength = add(new DoubleSetting("Trail length", 350.0, 20.0, 360.0, 10.0)
            .visibleWhen(trail::get));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    private float sweep;
    private float colorPhase;
    private long lastFrameNanos;
    /** 来源客户端 RenderSupport_144 在渲染失败后会停用绘制，这里保持一致。 */
    private boolean renderFailed;

    private ChinaHat() {
        super("ChinaHat", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        sweep = 0.0F;
        colorPhase = 0.0F;
        lastFrameNanos = 0L;
        renderFailed = false;
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (renderFailed || noPlayer() || !mc.player.isAlive()) {
            return;
        }
        // 来源客户端 只在第三人称绘制帽子（gameSettings.thirdPersonView != 0）。
        if (mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        advanceAnimation();

        PoseStack stack = event.getPoseStack();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 camera = mc.getEntityRenderDispatcher().camera.position();

        double x = Mth.lerp(partialTick, mc.player.xOld, mc.player.getX());
        double y = Mth.lerp(partialTick, mc.player.yOld, mc.player.getY())
                + mc.player.getBbHeight() + offset.get();
        double z = Mth.lerp(partialTick, mc.player.zOld, mc.player.getZ());
        if (mc.player.isShiftKeyDown()) {
            y -= CROUCH_DROP;
        }
        float yaw = Mth.lerp(partialTick, mc.player.yHeadRotO, mc.player.yHeadRot);
        float pitch = mc.player.getXRot(partialTick) * (tilt.get().floatValue() / 100.0F);

        stack.pushPose();
        try {
            stack.translate(x - camera.x, y - camera.y, z - camera.z);
            stack.mulPose(Axis.YP.rotationDegrees(-yaw));
            stack.mulPose(Axis.XP.rotationDegrees(pitch));
            Matrix4f matrix = stack.last().pose();

            BufferBuilder buffer = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buildHat(buffer, matrix);
            (throughWalls.get() ? HAT_THROUGH_WALLS : HAT_DEPTH_TESTED).draw(buffer.buildOrThrow());
        } catch (Throwable throwable) {
            // 与原模块一致：任何一次渲染失败后停用绘制，避免每帧刷错误。
            renderFailed = true;
        } finally {
            stack.popPose();
        }
    }

    private void buildHat(BufferBuilder buffer, Matrix4f matrix) {
        float hatRadius = radius.get().floatValue();
        float hatHeight = height.get().floatValue();
        float alphaScale = opacity.get().floatValue() / 100.0F;
        float brimY = hatHeight * (float) profile(0.0F);

        // 1) 帽体锥面：来源客户端 的 8 段 × 48 分段填充，顶点色随角度变化。
        for (int band = 0; band < BANDS; band++) {
            float f0 = (float) band / BANDS;
            float f1 = (float) (band + 1) / BANDS;
            float r0 = hatRadius * (1.0F - f0);
            float r1 = hatRadius * (1.0F - f1);
            float y0 = hatHeight * (float) profile(f0);
            float y1 = hatHeight * (float) profile(f1);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = TWO_PI * i / SEGMENTS;
                float a1 = TWO_PI * (i + 1) / SEGMENTS;
                float cos0 = Mth.cos(a0);
                float sin0 = Mth.sin(a0);
                float cos1 = Mth.cos(a1);
                float sin1 = Mth.sin(a1);
                int c0 = angleColor(a0, alphaScale * BODY_ALPHA);
                int c1 = angleColor(a1, alphaScale * BODY_ALPHA);
                addQuad(buffer, matrix,
                        r0 * cos0, y0, r0 * sin0, c0,
                        r0 * cos1, y0, r0 * sin1, c1,
                        r1 * cos1, y1, r1 * sin1, c1,
                        r1 * cos0, y1, r1 * sin0, c0);
            }
        }

        // 2) Glow：帽檐外一圈低透明度的光晕（来源客户端 的 1.07 倍外扩环）。
        if (glow.get()) {
            ringBand(buffer, matrix, brimY, hatRadius, hatRadius * GLOW_SCALE, alphaScale * GLOW_ALPHA);
        }

        // 3) 帽檐描边。
        float rimInner = Math.max(0.0001F, hatRadius - Math.max(RIM_WIDTH, hatRadius * 0.04F));
        ringBand(buffer, matrix, brimY, rimInner, hatRadius, alphaScale);

        // 4) Trail：绕帽旋转的亮线 + 身后的渐隐拖尾（拖尾是锥面上的一片渐隐色带）。
        if (trail.get()) {
            float span = (float) Math.toRadians(trailLength.get());
            meridianRibbon(buffer, matrix, hatRadius, hatHeight, sweep,
                    Math.max(0.0001F, hatRadius * SWEEP_WIDTH), alphaScale);
            wakeSector(buffer, matrix, hatRadius, hatHeight, sweep, span, alphaScale);
        }
    }

    /** 在给定高度画一整圈环形带（内/外半径，透明度均匀）。 */
    private void ringBand(BufferBuilder buffer, Matrix4f matrix, float y,
                          float inner, float outer, float alphaScale) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = TWO_PI * i / SEGMENTS;
            float a1 = TWO_PI * (i + 1) / SEGMENTS;
            addRingSegment(buffer, matrix, y, inner, outer, a0, a1, alphaScale, alphaScale);
        }
    }

    /** 从 startAngle 往回 span 弧度的锥面拖尾色带，越靠后越透明。 */
    private void wakeSector(BufferBuilder buffer, Matrix4f matrix, float hatRadius, float hatHeight,
                            float startAngle, float span, float alphaScale) {
        for (int i = 0; i < WAKE_SEGMENTS; i++) {
            float t0 = (float) i / WAKE_SEGMENTS;
            float t1 = (float) (i + 1) / WAKE_SEGMENTS;
            float a0 = startAngle - span * t0;
            float a1 = startAngle - span * t1;
            float fade0 = 1.0F - t0;
            float fade1 = 1.0F - t1;
            float cos0 = Mth.cos(a0);
            float sin0 = Mth.sin(a0);
            float cos1 = Mth.cos(a1);
            float sin1 = Mth.sin(a1);
            for (int band = 0; band < BANDS; band++) {
                float f0 = (float) band / BANDS;
                float f1 = (float) (band + 1) / BANDS;
                float r0 = hatRadius * (1.0F - f0);
                float r1 = hatRadius * (1.0F - f1);
                float y0 = hatHeight * (float) profile(f0);
                float y1 = hatHeight * (float) profile(f1);
                int c0 = angleColor(a0, alphaScale * BODY_ALPHA * fade0);
                int c1 = angleColor(a1, alphaScale * BODY_ALPHA * fade1);
                addQuad(buffer, matrix,
                        r0 * cos0, y0, r0 * sin0, c0,
                        r0 * cos1, y0, r0 * sin1, c1,
                        r1 * cos1, y1, r1 * sin1, c1,
                        r1 * cos0, y1, r1 * sin0, c0);
            }
        }
    }

    private void addRingSegment(BufferBuilder buffer, Matrix4f matrix, float y,
                                float inner, float outer,
                                float a0, float a1, float alpha0, float alpha1) {
        float cos0 = Mth.cos(a0);
        float sin0 = Mth.sin(a0);
        float cos1 = Mth.cos(a1);
        float sin1 = Mth.sin(a1);
        addQuad(buffer, matrix,
                inner * cos0, y, inner * sin0, angleColor(a0, alpha0),
                inner * cos1, y, inner * sin1, angleColor(a1, alpha1),
                outer * cos1, y, outer * sin1, angleColor(a1, alpha1),
                outer * cos0, y, outer * sin0, angleColor(a0, alpha0));
    }

    /** 从帽檐到帽尖、沿角 angle 的一条细子午线带（Trail 的扫描亮线）。 */
    private void meridianRibbon(BufferBuilder buffer, Matrix4f matrix, float hatRadius, float hatHeight,
                                float angle, float width, float alphaScale) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float perpX = -sin;
        float perpZ = cos;
        int color = angleColor(angle, alphaScale);
        for (int band = 0; band < BANDS; band++) {
            float f0 = (float) band / BANDS;
            float f1 = (float) (band + 1) / BANDS;
            float r0 = hatRadius * (1.0F - f0);
            float r1 = hatRadius * (1.0F - f1);
            float y0 = hatHeight * (float) profile(f0);
            float y1 = hatHeight * (float) profile(f1);
            float w0 = Math.max(0.0001F, width * (1.0F - f0));
            float w1 = Math.max(0.0001F, width * (1.0F - f1));
            addQuad(buffer, matrix,
                    r0 * cos - perpX * w0, y0, r0 * sin - perpZ * w0, color,
                    r0 * cos + perpX * w0, y0, r0 * sin + perpZ * w0, color,
                    r1 * cos + perpX * w1, y1, r1 * sin + perpZ * w1, color,
                    r1 * cos - perpX * w1, y1, r1 * sin - perpZ * w1, color);
        }
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float z1, int color1,
                                float x2, float y2, float z2, int color2,
                                float x3, float y3, float z3, int color3,
                                float x4, float y4, float z4, int color4) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(color1);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color2);
        buffer.addVertex(matrix, x3, y3, z3).setColor(color3);
        buffer.addVertex(matrix, x4, y4, z4).setColor(color4);
    }

    /** 环上某角度的顶点色，按 Colors 模式取值后再乘透明度系数。 */
    private int angleColor(float angle, float alphaScale) {
        Color base = switch (colors.get()) {
            case STATIC -> color.get();
            case GRADIENT -> ColorUtils.interpolate(color.get(), secondColor.get(),
                    0.5F - 0.5F * Mth.cos(angle + colorPhase));
            case RAINBOW -> rainbow(angle);
        };
        return applyAlpha(base, alphaScale);
    }

    /** RAINBOW：在第一个颜色的色相上按角度（+ 色相相位）绕一圈。 */
    private Color rainbow(float angle) {
        Color base = color.get();
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float hue = Mth.frac(hsb[0] + (float) Math.toDegrees(angle + colorPhase) / 360.0F);
        Color shifted = Color.getHSBColor(hue, hsb[1], hsb[2]);
        return new Color(shifted.getRed(), shifted.getGreen(), shifted.getBlue(), base.getAlpha());
    }

    private static int applyAlpha(Color color, float scale) {
        int alpha = Mth.clamp((int) (color.getAlpha() * scale), 0, 255);
        return (alpha << 24) | (color.getRGB() & 0xFFFFFF);
    }

    /** 来源客户端 RenderSupport_144.d：帽体高度曲线 h(f)。 */
    private static double profile(double fraction) {
        return Math.pow(fraction, 1.35) + 0.12 * Math.pow(1.0 - fraction, 5.0);
    }

    /** 每帧推进扫描线与色相相位（来源客户端 用 0.25s 截断的 delta time）。 */
    private void advanceAnimation() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        float deltaSeconds = Math.min((now - lastFrameNanos) / 1.0E9F, 0.25F);
        lastFrameNanos = now;
        sweep = (sweep + (float) Math.toRadians(spinSpeed.get()) * deltaSeconds) % TWO_PI;
        colorPhase = (colorPhase + (float) Math.toRadians(colorSpeed.get()) * deltaSeconds) % TWO_PI;
    }

    /** 按 Through walls 选择深度状态（false = 被方块遮挡，true = 透视）。 */
    private static RenderType createRenderType(String path, String name, CompareOp depth) {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("dioxide-lite", path))
                .withDepthStencilState(new DepthStencilState(depth, false))
                .withCull(false)
                .build();
        return RenderType.create(name, RenderSetup.builder(pipeline)
                .sortOnUpload()
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup());
    }
}
