package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 移植自 来源客户端 {@code features/render/Wings}：在玩家背后画一对带拍动动画的翅膀。
 * <p>
 * 1:1 对应：Wing type（来源 {@code RenderMode_171} 的 8 种剪影 ANGELIC/DRAGON/
 * BUTTERFLY/PHOENIX/CRYSTAL/MECHANICAL/FAIRY/DEMON，点集逐点照抄）、
 * Fill（Flat / Aurora 动画极光）、Targets（来源的 MultiSelectSetting 在
 * Dioxide 里拆成 Self / Others 两个开关，默认只画自己）、Scale 1.0/0.3..3.0、
 * Angle 20/0..90、Height 1.5/0.8..2.5、Depth 0.15/0..0.5、Flapping（默认开）、
 * Flap strength 30/5..60、Flap speed 3.0/0.5..8.0、Through walls（默认关）、
 * Color {@code 0xFFFFFFFF}、Opacity 100/5..100。
 * <p>
 * 变换链沿用 来源 {@code RenderSupport_172}：相机相对平移 → 绕 Y 轴
 * {@code 180 - bodyYaw} → 姿态的 preTranslate / pitch / yaw / roll →
 * 每侧 {@code translate(side * sideOffset, 0, sideZOffset)} → 绕 Y 轴
 * {@code side * open} → sideRoll → sidePitch → 三角形扇网格（1.22x 外圈光晕 +
 * 0.84x 内膜，顶点 alpha 按剪影自带的 alphaMul 调制）+ 0.96x 描边线。
 * {@code open} = {@code (拍动 + 水平速度张开 - Angle) * openMultiplier}，
 * 站姿 anchorY 1.38 / 潜行 0.96 + Height 偏移（Height - 1.5），
 * Depth 偏移（Depth - 0.15），落水时跳过。全部画在 {@link Render3DEvent} 的
 * 世界空间层。
 */
public final class Wings extends Module {

    public static final Wings INSTANCE = new Wings();

    /** 来源 {@code RenderMode_171}：每种剪影的点集（x = 外展、y = 上下、第三个 = alphaMul）。 */
    public enum WingShape {
        ANGELIC(
                new float[]{0.08F, 0.10F, 0.88F},
                new float[]{0.28F, 0.34F, 0.78F},
                new float[]{0.56F, 0.82F, 0.62F},
                new float[]{0.86F, 0.30F, 0.52F},
                new float[]{1.14F, 0.46F, 0.40F},
                new float[]{1.24F, 0.04F, 0.30F},
                new float[]{1.02F, -0.18F, 0.28F},
                new float[]{1.18F, -0.64F, 0.22F},
                new float[]{0.86F, -0.46F, 0.20F},
                new float[]{0.80F, -0.98F, 0.14F},
                new float[]{0.54F, -0.74F, 0.16F},
                new float[]{0.30F, -1.16F, 0.12F},
                new float[]{0.10F, -0.54F, 0.18F}),
        DRAGON(
                new float[]{0.10F, 0.12F, 0.90F},
                new float[]{0.22F, 0.40F, 0.80F},
                new float[]{0.48F, 0.72F, 0.65F},
                new float[]{0.80F, 0.60F, 0.55F},
                new float[]{1.10F, 0.70F, 0.42F},
                new float[]{1.30F, 0.30F, 0.35F},
                new float[]{1.20F, -0.10F, 0.30F},
                new float[]{1.05F, -0.50F, 0.25F},
                new float[]{0.70F, -0.35F, 0.22F},
                new float[]{0.50F, -0.70F, 0.18F},
                new float[]{0.20F, -0.50F, 0.15F},
                new float[]{0.05F, -0.30F, 0.20F}),
        BUTTERFLY(
                new float[]{0.12F, 0.15F, 0.92F},
                new float[]{0.30F, 0.50F, 0.85F},
                new float[]{0.50F, 0.90F, 0.70F},
                new float[]{0.70F, 0.80F, 0.60F},
                new float[]{0.85F, 0.55F, 0.50F},
                new float[]{0.75F, 0.20F, 0.45F},
                new float[]{0.55F, -0.15F, 0.40F},
                new float[]{0.40F, -0.60F, 0.30F},
                new float[]{0.25F, -0.85F, 0.20F},
                new float[]{0.12F, -0.65F, 0.25F},
                new float[]{0.06F, -0.35F, 0.30F}),
        PHOENIX(
                new float[]{0.10F, 0.14F, 0.90F},
                new float[]{0.25F, 0.45F, 0.82F},
                new float[]{0.52F, 0.78F, 0.68F},
                new float[]{0.82F, 0.50F, 0.55F},
                new float[]{1.15F, 0.55F, 0.42F},
                new float[]{1.28F, 0.15F, 0.32F},
                new float[]{1.20F, -0.25F, 0.28F},
                new float[]{1.10F, -0.55F, 0.24F},
                new float[]{1.25F, -0.85F, 0.18F},
                new float[]{0.90F, -0.65F, 0.16F},
                new float[]{0.60F, -0.90F, 0.14F},
                new float[]{0.30F, -0.70F, 0.12F},
                new float[]{0.08F, -0.40F, 0.16F}),
        CRYSTAL(
                new float[]{0.15F, 0.10F, 0.85F},
                new float[]{0.40F, 0.35F, 0.75F},
                new float[]{0.70F, 0.60F, 0.60F},
                new float[]{1.00F, 0.40F, 0.50F},
                new float[]{0.85F, 0.10F, 0.45F},
                new float[]{1.10F, -0.15F, 0.35F},
                new float[]{0.90F, -0.45F, 0.30F},
                new float[]{0.65F, -0.30F, 0.25F},
                new float[]{0.45F, -0.60F, 0.20F},
                new float[]{0.20F, -0.40F, 0.22F},
                new float[]{0.08F, -0.15F, 0.30F}),
        MECHANICAL(
                new float[]{0.08F, 0.08F, 0.90F},
                new float[]{0.20F, 0.25F, 0.82F},
                new float[]{0.45F, 0.40F, 0.70F},
                new float[]{0.70F, 0.35F, 0.58F},
                new float[]{0.95F, 0.25F, 0.48F},
                new float[]{0.90F, 0.00F, 0.40F},
                new float[]{1.10F, -0.15F, 0.32F},
                new float[]{0.80F, -0.30F, 0.28F},
                new float[]{0.55F, -0.20F, 0.25F},
                new float[]{0.40F, -0.45F, 0.20F},
                new float[]{0.15F, -0.30F, 0.22F},
                new float[]{0.05F, -0.10F, 0.28F}),
        FAIRY(
                new float[]{0.10F, 0.12F, 0.90F},
                new float[]{0.25F, 0.38F, 0.82F},
                new float[]{0.42F, 0.65F, 0.68F},
                new float[]{0.55F, 0.70F, 0.58F},
                new float[]{0.60F, 0.45F, 0.50F},
                new float[]{0.50F, 0.15F, 0.42F},
                new float[]{0.38F, -0.10F, 0.35F},
                new float[]{0.30F, -0.35F, 0.28F},
                new float[]{0.18F, -0.45F, 0.22F},
                new float[]{0.08F, -0.25F, 0.26F}),
        DEMON(
                new float[]{0.10F, 0.12F, 0.88F},
                new float[]{0.25F, 0.38F, 0.80F},
                new float[]{0.55F, 0.65F, 0.65F},
                new float[]{0.85F, 0.50F, 0.52F},
                new float[]{1.15F, 0.55F, 0.40F},
                new float[]{1.25F, 0.20F, 0.32F},
                new float[]{1.10F, -0.10F, 0.28F},
                new float[]{1.30F, -0.45F, 0.22F},
                new float[]{1.15F, -0.70F, 0.18F},
                new float[]{0.85F, -0.55F, 0.16F},
                new float[]{0.55F, -0.85F, 0.14F},
                new float[]{0.25F, -0.65F, 0.12F},
                new float[]{0.08F, -0.35F, 0.18F});

        private final float[][] points;

        WingShape(float[]... points) {
            this.points = points;
        }

        public float[][] points() {
            return points;
        }
    }

    /** 来源 {@code FeatureMode_248}：H = 纯色（默认）、另一种 = 动画极光。 */
    public enum Fill {
        FLAT,
        AURORA
    }

    /** 来源 {@code RenderSupport_175} 里由 Wing 设置推导出来的翅膀姿态（RenderSupport_174）。 */
    private record Pose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                        float pitch, float yaw, float roll, float openMultiplier,
                        float scaleMultiplier, float motionSpreadBoost, float flapAmplitude,
                        float sideOffset, float sideZOffset, float sideRoll, float sidePitch,
                        float flapSpeed) {
    }

    /** 来源 站姿：anchorY 1.38，其余为共享值。 */
    private static final Pose STANDING_POSE = new Pose(
            0.0F, 0.0F, 1.38F, 0.10F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F,
            0.06F, 0.02F, -11.0F, -4.0F, 0.12F);
    /** 来源 潜行姿：anchorY 0.96 + 18 度俯仰。 */
    private static final Pose SNEAKING_POSE = new Pose(
            0.0F, 0.0F, 0.96F, 0.10F, 18.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F,
            0.06F, 0.02F, -11.0F, -4.0F, 0.12F);
    /** 来源 pose 的默认拍动幅度基准。 */
    private static final float DEFAULT_FLAP_AMPLITUDE = 4.5F;
    /** 来源 RenderSupport_172：外圈光晕 / 内膜 / 描边三档缩放。 */
    private static final float GLOW_SCALE = 1.22F;
    private static final float MEMBRANE_SCALE = 0.84F;
    private static final float OUTLINE_SCALE = 0.96F;
    /** 来源 描边线宽 2.2px —— 按世界尺度取半宽（约 0.006 格），改用四边形描边后不再用 GL 线宽。 */
    private static final float OUTLINE_HALF_WIDTH = 0.006F;
    /** 拍动角速度基准（rad/tick）：来源的时间常量被混淆，按默认 Flap speed = 3 取约 1 秒一拍。 */
    private static final float FLAP_BASE_SPEED = 0.3F;
    /** 极光填充色相循环周期。 */
    private static final long AURORA_PERIOD_MS = 6000L;
    // PORT-NOTE: 需要自定义 shader/FBO hook（来源的 Aurora 填充是屏幕空间着色器，且剪影内部的
    // RenderSupport_177 索引表在反编译源码里缺失）；本端口只实现了程序化顶点渐变的膜 + 轮廓/根部连线描边。

    private static final RenderPipeline MEMBRANE_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_wings"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .build();

    private static final RenderPipeline MEMBRANE_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_wings_no_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .build();

    private static final Function<RenderPipeline, RenderType> MEMBRANE_LAYER = Util.memoize(
            // 注意：这里不能开 sortOnUpload()。MC 的顶点排序只实现了 QUADS 布局，
            // 对 TRIANGLES 缓冲做排序会把顶点/颜色数据错位，整片膜会变成横跨屏幕的
            // 巨大三角形（用户看到的彩色竖条）。翅膀是半透明但不需要按深度排序。
            pipeline -> RenderType.create("dioxidelite_custom_wings", RenderSetup.builder(pipeline)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .createRenderSetup()));

    public final EnumSetting<WingShape> wingType = add(new EnumSetting<>("Wing type", WingShape.ANGELIC));
    public final EnumSetting<Fill> fill = add(new EnumSetting<>("Fill", Fill.FLAT));
    // 来源的 MultiSelectSetting("Targets", [Self, Others]) 在 Dioxide 拆成两个开关。
    public final BooleanSetting self = add(new BooleanSetting("Self", true));
    public final BooleanSetting others = add(new BooleanSetting("Others", false));
    public final DoubleSetting scale = add(new DoubleSetting("Scale", 1.0, 0.3, 3.0, 0.1));
    public final DoubleSetting angle = add(new DoubleSetting("Angle", 20.0, 0.0, 90.0, 1.0));
    public final DoubleSetting height = add(new DoubleSetting("Height", 1.5, 0.8, 2.5, 0.05));
    public final DoubleSetting depth = add(new DoubleSetting("Depth", 0.15, 0.0, 0.5, 0.01));
    public final BooleanSetting flapping = add(new BooleanSetting("Flapping", true));
    public final DoubleSetting flapStrength = add(new DoubleSetting("Flap strength", 30.0, 5.0, 60.0, 1.0)
            .visibleWhen(flapping::get));
    public final DoubleSetting flapSpeed = add(new DoubleSetting("Flap speed", 3.0, 0.5, 8.0, 0.5)
            .visibleWhen(flapping::get));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(255, 255, 255)));
    public final DoubleSetting opacity = add(new DoubleSetting("Opacity", 100.0, 5.0, 100.0, 1.0));

    private Wings() {
        super("Wings", Category.RENDER);
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        List<Player> targets = new ArrayList<>();
        // 来源：自己只在第三人称可见时绘制（gameSettings.thirdPersonView != 0）。
        if (self.get() && !mc.options.getCameraType().isFirstPerson() && mc.player.isAlive()) {
            targets.add(mc.player);
        }
        if (others.get()) {
            for (Player player : mc.level.players()) {
                if (player == mc.player || !player.isAlive()) {
                    continue;
                }
                targets.add(player);
            }
        }
        if (targets.isEmpty()) {
            return;
        }

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 camera = mc.getEntityRenderDispatcher().camera.position();
        PoseStack stack = event.getPoseStack();
        WingShape shape = wingType.get();
        Color base = color.get();
        float alpha = (base.getAlpha() / 255.0F) * (opacity.get().floatValue() / 100.0F);
        // Height / Depth 在 来源 里是叠加在姿态 anchor/offset 上的增量。
        float heightOffset = height.get().floatValue() - 1.5F;
        float depthOffset = depth.get().floatValue() - 0.15F;
        float time = (System.currentTimeMillis() % AURORA_PERIOD_MS) / (float) AURORA_PERIOD_MS;
        boolean noDepth = throughWalls.get();

        RenderType membraneLayer = MEMBRANE_LAYER.apply(noDepth ? MEMBRANE_NO_DEPTH_PIPELINE : MEMBRANE_PIPELINE);
        // 描边不再单开 LINES 图层：GL_LINES 管线在本版本上渲染异常（整片描边被拉成
        // 横跨屏幕的竖条），改成和膜共面的一层细四边形，走同一个（已验证可用的）三角形管线。
        BufferBuilder membrane = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (Player player : targets) {
            // 来源：落水时不画。
            if (player.isInWater()) {
                continue;
            }
            Pose pose = player.isShiftKeyDown() ? SNEAKING_POSE : STANDING_POSE;
            float bodyYaw = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot);
            float open = computeOpen(player, pose, partial);
            // 关键：锚点要用**插值后**的位置。用 tick 位置会在两 tick 之间跳，
            // 而玩家模型是插值渲染的 —— 翅膀就会相对身体来回漂（用户报的"漂泊不定"）。
            Vec3 anchor = new Vec3(
                    Mth.lerp(partial, player.xOld, player.getX()),
                    Mth.lerp(partial, player.yOld, player.getY()),
                    Mth.lerp(partial, player.zOld, player.getZ()));
            renderWingPair(stack, membrane, anchor, player, camera, pose, shape,
                    bodyYaw, open, alpha, heightOffset, depthOffset, time);
        }

        membraneLayer.draw(membrane.buildOrThrow());
    }

    /** 来源 {@code RenderSupport_172} 的变换链 + 两遍三角扇 + 描边。 */
    private void renderWingPair(PoseStack stack, BufferBuilder membrane,
                                Vec3 anchor, Player player, Vec3 camera, Pose pose, WingShape shape,
                                float bodyYaw, float open, float alpha,
                                float heightOffset, float depthOffset, float time) {
        stack.pushPose();
        stack.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (pose.preTranslateY() != 0.0F || pose.preTranslateZ() != 0.0F) {
            stack.translate(0.0D, pose.preTranslateY(), pose.preTranslateZ());
        }
        if (pose.pitch() != 0.0F) {
            stack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
        }
        if (pose.yaw() != 0.0F) {
            stack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
        }
        if (pose.roll() != 0.0F) {
            stack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
        }
        // 姿态锚点（站姿 y=1.38 / 潜行 y=0.96，z=0.10 背后偏移）+ 设置里的 Height / Depth。
        stack.translate(0.0D, pose.anchorY() + heightOffset, pose.anchorZ() + depthOffset);

        float wingScale = scale.get().floatValue() * pose.scaleMultiplier();

        for (int side = 1; side >= -1; side -= 2) {
            stack.pushPose();
            stack.translate(side * pose.sideOffset(), 0.0D, pose.sideZOffset());
            stack.mulPose(Axis.YP.rotationDegrees(side * open));
            stack.mulPose(Axis.ZP.rotationDegrees(pose.sideRoll()));
            stack.mulPose(Axis.XP.rotationDegrees(pose.sidePitch()));
            Matrix4f matrix = stack.last().pose();

            // 外圈光晕（1.22x）与内膜（0.84x），和 来源的两遍 fan 一致。
            emitFan(membrane, matrix, shape, side, wingScale * GLOW_SCALE, alpha, time);
            emitFan(membrane, matrix, shape, side, wingScale * MEMBRANE_SCALE, alpha, time);
            emitOutline(membrane, matrix, shape, side, wingScale * OUTLINE_SCALE, alpha, time);

            stack.popPose();
        }
        stack.popPose();
    }

    /** 以翅膀根部为原点的三角扇，顶点 alpha 按剪影自带的 alphaMul 调制。 */
    private void emitFan(BufferBuilder buffer, Matrix4f matrix, WingShape shape, int side,
                         float scale, float alpha, float time) {
        float[][] points = shape.points();
        for (int i = 0; i < points.length; i++) {
            float[] from = points[i];
            float[] to = points[(i + 1) % points.length];
            buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(shade(alpha, from[2], from[0], time));
            buffer.addVertex(matrix, side * from[0] * scale, from[1] * scale, 0.0F)
                    .setColor(shade(alpha, from[2], from[0], time));
            buffer.addVertex(matrix, side * to[0] * scale, to[1] * scale, 0.0F)
                    .setColor(shade(alpha, to[2], to[0], time));
        }
    }

    /** 剪影描边 + 从根部到各点的骨线（来源的 RenderSupport_177 索引表缺失，这里用根部连线近似）。 */
    private void emitOutline(BufferBuilder buffer, Matrix4f matrix, WingShape shape, int side,
                             float scale, float alpha, float time) {
        float[][] points = shape.points();
        for (int i = 0; i < points.length; i++) {
            float[] from = points[i];
            float[] to = points[(i + 1) % points.length];
            float fromX = side * from[0] * scale;
            float fromY = from[1] * scale;
            float toX = side * to[0] * scale;
            float toY = to[1] * scale;
            int fromColor = shade(alpha, from[2], from[0], time);
            int toColor = shade(alpha, to[2], to[0], time);
            inPlaneQuad(buffer, matrix, 0.0F, 0.0F, fromX, fromY, OUTLINE_HALF_WIDTH, fromColor, fromColor);
            inPlaneQuad(buffer, matrix, fromX, fromY, toX, toY, OUTLINE_HALF_WIDTH, fromColor, toColor);
        }
    }

    /** 翅膀平面内的一段细四边形（两个三角形）—— 替代原先的 GL_LINES 描边。 */
    private static void inPlaneQuad(BufferBuilder buffer, Matrix4f matrix,
                                    float x1, float y1, float x2, float y2,
                                    float half, int fromColor, int toColor) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-5F) {
            return;
        }
        float nx = -dy / length * half;
        float ny = dx / length * half;
        buffer.addVertex(matrix, x1 + nx, y1 + ny, 0.0F).setColor(fromColor);
        buffer.addVertex(matrix, x1 - nx, y1 - ny, 0.0F).setColor(fromColor);
        buffer.addVertex(matrix, x2 - nx, y2 - ny, 0.0F).setColor(toColor);
        buffer.addVertex(matrix, x1 + nx, y1 + ny, 0.0F).setColor(fromColor);
        buffer.addVertex(matrix, x2 - nx, y2 - ny, 0.0F).setColor(toColor);
        buffer.addVertex(matrix, x2 + nx, y2 + ny, 0.0F).setColor(toColor);
    }

    /**
     * 来源 {@code Wings.L(EntityPlayer, RenderSupport_174, float)}：
     * {@code (flap + horizontalSpeed * motionSpreadBoost - Angle) * openMultiplier}。
     */
    private float computeOpen(Player player, Pose pose, float partial) {
        float flap = 0.0F;
        if (flapping.get()) {
            float speed = FLAP_BASE_SPEED * (flapSpeed.get().floatValue() / 3.0F);
            float amplitude = (float) Math.toRadians(flapStrength.get().floatValue())
                    * (pose.flapAmplitude() / DEFAULT_FLAP_AMPLITUDE);
            flap = Mth.sin((player.tickCount + partial) * speed) * amplitude;
        }
        Vec3 motion = player.getDeltaMovement();
        float horizontal = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        float spread = Mth.clamp(horizontal * 10.0F, 0.0F, 1.0F);
        return (flap + spread * pose.motionSpreadBoost() - angle.get().floatValue()) * pose.openMultiplier();
    }

    /** Fill = FLAT 用设置颜色；AURORA 用沿翅膀外展方向 + 时间漂移的色相。 */
    private int shade(float alpha, float alphaMul, float outward, float time) {
        float factor = Mth.clamp(0.35F + 0.65F * alphaMul, 0.0F, 1.0F);
        if (fill.is(Fill.AURORA)) {
            float hue = Mth.frac(time + outward * 0.35F);
            return pack(Color.getHSBColor(hue, 0.75F, 1.0F), alpha * factor);
        }
        return pack(color.get(), alpha * factor);
    }

    private static int pack(Color color, float alpha) {
        int a = Mth.clamp((int) (255.0F * alpha), 0, 255);
        return (a << 24) | (color.getRGB() & 0xFFFFFF);
    }

}
