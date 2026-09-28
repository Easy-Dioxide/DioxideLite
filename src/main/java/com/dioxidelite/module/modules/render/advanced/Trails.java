package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 移植自 来源客户端 {@code features/render/Trails}：在玩家身后留下一条随时间淡出的
 * 拖尾（来源 用 GL 四边形条带 + 两条上下包边线，点模式用 GL_POINTS）。
 * <p>
 * 1:1 对应：Mode（来源 {@code FeatureMode_254}：i = 线/墙、H = 点）、
 * Color {@code 0xFF64AFFF}、Length 1.0s/0.25..5.0/0.05、Opacity 60/5..100、
 * Line width 1.5px/0.5..5.0（仅线模式可见）、Point size 4px/1..12/0.5（仅点模式可见）、
 * First person（默认关）、Through walls（默认关）。
 * <p>
 * 采样沿用 来源的 {@code TickEndEvent}：每 tick 与上一个点的距离平方大于
 * {@code 1.0E-8} 才记录；渲染时补一个按 partialTick 插值的头部点，
 * 拖尾高度 = {@code getBbHeight() * (潜行 ? 0.8 : 1.0)}，alpha =
 * {@code 255 * min(1, Opacity * 2.5) * (1 - age / Length)}。
 * 点模式的 GL_POINTS 用相机朝向的公告板四边形替代，边长按投影矩阵换算成
 * 与 来源的 glPointSize 像素大小一致的屏幕尺寸。全部画在
 * {@link Render3DEvent} 的世界空间层。
 */
public final class Trails extends Module {

    public static final Trails INSTANCE = new Trails();

    /** 来源 {@code FeatureMode_254}：i = 线（四边形条带 + 两条包边线）、H = 点。 */
    public enum Mode {
        LINE,
        POINT
    }

    /** 来源的最小采样间距平方。 */
    private static final double MIN_STEP_SQUARED = 1.0E-8D;
    /** 来源 包边线相对底/顶的高度内缩。 */
    private static final float EDGE_INSET = 0.004F;
    /** 来源 点模式的竖直偏移（{@code y + 0.8}）。 */
    private static final double POINT_OFFSET = 0.8D;
    /** 来源的透明度增益：{@code min(1, Opacity * 2.5)}。 */
    private static final float OPACITY_GAIN = 2.5F;

    private static final RenderPipeline SHAPE_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_trails_shape"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static final RenderPipeline SHAPE_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_trails_shape_no_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static final Function<RenderPipeline, RenderType> SHAPE_LAYER = Util.memoize(
            pipeline -> RenderType.create("dioxidelite_custom_trails", RenderSetup.builder(pipeline)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .createRenderSetup()));

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.LINE));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(100, 175, 255)));
    public final DoubleSetting length = add(new DoubleSetting("Length", 1.0, 0.25, 5.0, 0.05));
    public final DoubleSetting opacity = add(new DoubleSetting("Opacity", 60.0, 5.0, 100.0, 1.0));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line width", 1.5, 0.5, 5.0, 0.5)
            .visibleWhen(() -> mode.is(Mode.LINE)));
    public final DoubleSetting pointSize = add(new DoubleSetting("Point size", 4.0, 1.0, 12.0, 0.5)
            .visibleWhen(() -> mode.is(Mode.POINT)));
    public final BooleanSetting firstPerson = add(new BooleanSetting("First person", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    /** 拖尾上的一个采样点（来源的 {@code RenderSupport_164}）。 */
    private record Point(Vec3 position, long time) {
    }

    private final List<Point> trail = new ArrayList<>();
    private ClientLevel trackedLevel;

    private Trails() {
        super("Trails", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        trail.clear();
    }

    /** 来源的 {@code TickEndEvent}：每 tick 追加一个移动过的采样点。 */
    @Listen
    private void onTick(TickEvent.Post event) {
        if (noPlayer()) {
            trail.clear();
            return;
        }
        ensureLevel();

        long now = System.currentTimeMillis();
        long lifetime = lifetimeMillis();
        trail.removeIf(point -> now - point.time() >= lifetime);

        Vec3 current = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (trail.isEmpty()
                || trail.get(trail.size() - 1).position().distanceToSqr(current) > MIN_STEP_SQUARED) {
            trail.add(new Point(current, now));
        }
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }
        // 来源：第三人称才画，除非打开 First person。
        if (!firstPerson.get() && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lifetime = lifetimeMillis();
        trail.removeIf(point -> now - point.time() >= lifetime);

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        List<Point> points = new ArrayList<>(trail.size() + 1);
        points.addAll(trail);
        points.add(new Point(new Vec3(
                Mth.lerp(partial, mc.player.xOld, mc.player.getX()),
                Mth.lerp(partial, mc.player.yOld, mc.player.getY()),
                Mth.lerp(partial, mc.player.zOld, mc.player.getZ())), now));

        if (mode.is(Mode.LINE) && points.size() < 2) {
            return;
        }
        if (points.isEmpty()) {
            return;
        }

        Vec3 camera = mc.getEntityRenderDispatcher().camera.position();
        PoseStack stack = event.getPoseStack();
        PoseStack.Pose entry = stack.last();
        Color base = color.get();
        float alphaScale = (base.getAlpha() / 255.0F)
                * Math.min(1.0F, opacity.get().floatValue() / 100.0F * OPACITY_GAIN);
        boolean noDepth = throughWalls.get();
        // 来源：拖尾高度 = 身高 * (潜行 ? 0.8 : 1.0)。
        double height = mc.player.getBbHeight() * (mc.player.isShiftKeyDown() ? 0.8D : 1.0D);

        if (mode.is(Mode.LINE)) {
            RenderType shapeLayer = SHAPE_LAYER.apply(noDepth ? SHAPE_NO_DEPTH_PIPELINE : SHAPE_PIPELINE);
            // 上下包边线不再走 GL_LINES（该管线在本版本上渲染异常），改成同面的细四边形。
            BufferBuilder ribbon = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float thickness = lineWidth.get().floatValue();

            for (int i = 0; i < points.size() - 1; i++) {
                Point from = points.get(i);
                Point to = points.get(i + 1);
                int colorFrom = fadeColor(base, from, now, lifetime, alphaScale);
                int colorTo = fadeColor(base, to, now, lifetime, alphaScale);

                ribbonQuad(ribbon, entry, from.position(), to.position(), 0.0D, height, camera, colorFrom, colorTo);
                // 来源的两条包边线：y = 0.004 与 y = height - 0.004（细四边形，宽度按像素→世界换算）。
                float edgeThickness = worldSize(thickness, camera.distanceTo(from.position()));
                ribbonQuad(ribbon, entry, from.position(), to.position(), EDGE_INSET,
                        edgeThickness, camera, colorFrom, colorTo);
                ribbonQuad(ribbon, entry, from.position(), to.position(), height - EDGE_INSET - edgeThickness,
                        edgeThickness, camera, colorFrom, colorTo);
            }
            shapeLayer.draw(ribbon.buildOrThrow());
            return;
        }

        RenderType layer = SHAPE_LAYER.apply(noDepth ? SHAPE_NO_DEPTH_PIPELINE : SHAPE_PIPELINE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float pixels = pointSize.get().floatValue();
        for (Point point : points) {
            int argb = fadeColor(base, point, now, lifetime, alphaScale);
            billboard(stack, buffer, point.position().add(0.0D, POINT_OFFSET, 0.0D), camera, pixels, argb);
        }
        layer.draw(buffer.buildOrThrow());
    }

    /** 来源的四边形条带：每段由 底-顶-顶-底 四个顶点组成（{@code yBase} 为底边抬高量）。 */
    private static void ribbonQuad(BufferBuilder buffer, PoseStack.Pose entry,
                                   Vec3 from, Vec3 to, double yBase, double height, Vec3 camera,
                                   int colorFrom, int colorTo) {
        Matrix4f matrix = entry.pose();
        float fromX = (float) (from.x - camera.x);
        float fromY = (float) (from.y - camera.y + yBase);
        float fromZ = (float) (from.z - camera.z);
        float toX = (float) (to.x - camera.x);
        float toY = (float) (to.y - camera.y + yBase);
        float toZ = (float) (to.z - camera.z);
        float top = (float) height;

        buffer.addVertex(matrix, fromX, fromY, fromZ).setColor(colorFrom);
        buffer.addVertex(matrix, fromX, fromY + top, fromZ).setColor(colorFrom);
        buffer.addVertex(matrix, toX, toY + top, toZ).setColor(colorTo);
        buffer.addVertex(matrix, toX, toY, toZ).setColor(colorTo);
    }

    /** 点模式：相机朝向的公告板，边长把 Point size(px) 换算成世界尺寸。 */
    private void billboard(PoseStack stack, BufferBuilder buffer, Vec3 position,
                           Vec3 camera, float pixels, int argb) {
        double distance = camera.distanceTo(position);
        float half = worldSize(pixels, distance) * 0.5F;
        if (half <= 0.0F) {
            return;
        }
        Camera main = mc.gameRenderer.getMainCamera();
        stack.pushPose();
        stack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
        stack.mulPose(Axis.YP.rotationDegrees(-main.yRot()));
        stack.mulPose(Axis.XP.rotationDegrees(main.xRot()));
        Matrix4f matrix = stack.last().pose();
        buffer.addVertex(matrix, -half, half, 0.0F).setColor(argb);
        buffer.addVertex(matrix, half, half, 0.0F).setColor(argb);
        buffer.addVertex(matrix, half, -half, 0.0F).setColor(argb);
        buffer.addVertex(matrix, -half, -half, 0.0F).setColor(argb);
        stack.popPose();
    }

    /**
     * 把 custom 的 glPointSize 像素大小换算成该距离上的世界尺寸：
     * 透视投影下 {@code px = size_world * m11 / depth * height / 2}。
     */
    private float worldSize(float pixels, double distance) {
        try {
            Matrix4f projection = mc.gameRenderer.getGameRenderState()
                    .levelRenderState.cameraRenderState.projectionMatrix;
            float m11 = projection.m11();
            int screenHeight = mc.getWindow().getHeight();
            if (m11 > 1.0E-4F && screenHeight > 0 && distance > 0.01D) {
                return (float) (pixels * distance * 2.0D / (m11 * screenHeight));
            }
        } catch (RuntimeException ignored) {
            // 投影矩阵不可用时退回一个近似值。
        }
        return pixels * 0.005F;
    }

    /** 来源：alpha = 255 * min(1, Opacity * 2.5) * (1 - age / Length)。 */
    private static int fadeColor(Color base, Point point, long now, long lifetime, float alphaScale) {
        float age = Mth.clamp((now - point.time()) / (float) lifetime, 0.0F, 1.0F);
        float alpha = alphaScale * (1.0F - age);
        int a = Mth.clamp((int) (255.0F * alpha), 0, 255);
        return (a << 24) | (base.getRGB() & 0xFFFFFF);
    }


    private long lifetimeMillis() {
        return (long) (length.get() * 1000.0D);
    }

    private void ensureLevel() {
        if (trackedLevel == mc.level) {
            return;
        }
        trackedLevel = mc.level;
        trail.clear();
    }
}
