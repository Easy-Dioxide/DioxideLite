package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.setting.settings.StringSetting;
import com.dioxidelite.util.render.WorldToScreen;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.multiplayer.ClientLevel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 移植自 来源客户端 {@code features/render/JumpCircles}：玩家起跳时在地面留下一个
 * 向外扩散并淡出的圆环（来源 用 {@code textures/effect/jump_circle.png} 的公告板
 * 四边形，这里按同样的半径/透明度曲线画程序化圆盘）。
 * <p>
 * 1:1 对应：Mode（来源 FeatureMode_305：圆环 / 圆环+文字）、Text {@code "DIOXIDE"}、
 * Text size 0.22/0.05..0.6/0.01、Spin speed 40/-360..360/5、Color {@code 0xFF64AFFF}、
 * Radius 1.5/0.3..6.0/0.1、Opacity 70/5..100、Duration 2.0s/0.25..8.0、
 * Other players（默认关）、Through walls（默认关）。
 * <p>
 * 起跳判定沿用 来源的 {@code JumpCircles.L(EntityPlayer, Vec3)}：离地瞬间
 * {@code motionY > 0 || posY > 记录落地点 + 0.01}；半径曲线沿用
 * {@code Radius * (1 - (1 - t)^4)}，alpha 曲线为 {@code Opacity * (1 - t)}。
 * 圆盘画在 {@link Render3DEvent} 的世界空间层；环绕文字（来源 在 3D 里逐字形
 * 排布）改用 {@link WorldToScreen} 投影后在 {@link Render2DEvent} 的 Skija
 * 画布上沿同一半径环绕绘制，Spin speed 作用在这圈文字上。
 */
public final class JumpCircles extends Module {

    public static final JumpCircles INSTANCE = new JumpCircles();

    /** 来源 {@code FeatureMode_305}：H = 纯圆环（默认）、f = 圆环 + 环绕文字。 */
    public enum Mode {
        CIRCLE,
        TEXT
    }

    /** 程序化圆盘的细分数（替代 来源的贴图四边形）。 */
    private static final int DISC_SEGMENTS = 64;
    // PORT-NOTE: 需要 framebuffer/屏幕拷贝 hook（来源 FeatureSupport_256 + RenderSupport_204 的玻璃样式会抓屏做模糊/折射圆环）
    // 和 3D 字形几何（来源 RenderSupport_203 自带 3D 字体排布）；本端口只实现了纯色圆盘 + 投影到 2D 画布上的环绕文字。
    /** 来源的贴地偏移：{@code posY + 0.015}。 */
    private static final double GROUND_OFFSET = 0.015D;
    /** 圆盘边缘 alpha 系数，模拟贴图边缘的软过渡。 */
    private static final float EDGE_ALPHA = 0.12F;
    /** 圆环收到该像素半径以下时不再画环绕文字。 */
    private static final float MIN_TEXT_RING_PIXELS = 8.0F;

    private static final RenderPipeline DISC_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_jump_circles"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .build();

    private static final RenderPipeline DISC_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_jump_circles_no_depth"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .build();

    private static final Function<RenderPipeline, RenderType> DISC_LAYER = Util.memoize(
            // 同 Wings：TRIANGLES 缓冲不能开 sortOnUpload()，否则顶点数据错位成巨大三角形。
            pipeline -> RenderType.create("dioxidelite_custom_jump_circles", RenderSetup.builder(pipeline)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .createRenderSetup()));

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.CIRCLE));
    public final StringSetting text = add(new StringSetting("Text", "DIOXIDE")
            .visibleWhen(() -> mode.is(Mode.TEXT)));
    public final DoubleSetting textSize = add(new DoubleSetting("Text size", 0.22, 0.05, 0.6, 0.01)
            .visibleWhen(() -> mode.is(Mode.TEXT)));
    public final DoubleSetting spinSpeed = add(new DoubleSetting("Spin speed", 40.0, -360.0, 360.0, 5.0));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(100, 175, 255)));
    public final DoubleSetting radius = add(new DoubleSetting("Radius", 1.5, 0.3, 6.0, 0.1));
    public final DoubleSetting opacity = add(new DoubleSetting("Opacity", 70.0, 5.0, 100.0, 1.0));
    public final DoubleSetting duration = add(new DoubleSetting("Duration", 2.0, 0.25, 8.0, 0.25));
    public final BooleanSetting otherPlayers = add(new BooleanSetting("Other players", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", false));

    /** 一次起跳留下的圆环（来源的 {@code RenderSupport_206}）。 */
    private record Circle(Vec3 position, long time) {
    }

    /** 本帧投影到屏幕的一圈文字（来源的 3D 逐字形排布改成 2D 环绕）。 */
    private record Ring(Vec3 position, float radius, int argb, double ageSeconds) {
    }

    private final List<Circle> circles = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    /** 每个玩家最近一次落地时的位置（来源的 {@code Map<Integer, Vec3> d}）。 */
    private final Map<Integer, Vec3> groundPositions = new HashMap<>();
    private ClientLevel trackedLevel;

    private JumpCircles() {
        super("JumpCircles", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        circles.clear();
        rings.clear();
        groundPositions.clear();
    }

    /** 来源的 {@code TickEndEvent} 处理：维护落地点并在起跳帧生成圆环。 */
    @Listen
    private void onTick(TickEvent.Post event) {
        if (noPlayer()) {
            circles.clear();
            groundPositions.clear();
            return;
        }
        ensureLevel();

        long now = System.currentTimeMillis();
        long lifetime = lifetimeMillis();
        circles.removeIf(circle -> now - circle.time() >= lifetime);

        Set<Integer> alive = new HashSet<>();
        for (Player player : mc.level.players()) {
            if (!player.isAlive()) {
                continue;
            }
            if (player != mc.player && !otherPlayers.get()) {
                continue;
            }
            alive.add(player.getId());

            if (player.onGround()) {
                groundPositions.put(player.getId(), new Vec3(player.getX(), player.getY(), player.getZ()));
                continue;
            }
            Vec3 takeoff = groundPositions.remove(player.getId());
            if (takeoff == null) {
                continue;
            }
            // 来源：motionY > 0 或者已经高出记录的落地点 0.01 才算一次起跳。
            if (player.getDeltaMovement().y > 0.0D || player.getY() > takeoff.y + 0.01D) {
                circles.add(new Circle(takeoff, now));
            }
        }
        groundPositions.keySet().retainAll(alive);
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        rings.clear();
        if (noPlayer() || circles.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long lifetime = lifetimeMillis();
        if (lifetime <= 0L) {
            return;
        }
        circles.removeIf(circle -> now - circle.time() >= lifetime);
        if (circles.isEmpty()) {
            return;
        }

        Vec3 camera = mc.getEntityRenderDispatcher().camera.position();
        PoseStack stack = event.getPoseStack();
        Color base = color.get();
        float alphaScale = (base.getAlpha() / 255.0F) * (opacity.get().floatValue() / 100.0F);
        RenderType layer = DISC_LAYER.apply(throughWalls.get() ? DISC_NO_DEPTH_PIPELINE : DISC_PIPELINE);
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (Circle circle : circles) {
            long age = now - circle.time();
            float progress = Mth.clamp(age / (float) lifetime, 0.0F, 1.0F);
            // 来源：半径 = Radius * (1 - (1 - t)^4)，透明度 = Opacity * (1 - t)。
            float currentRadius = Math.max(0.05F,
                    radius.get().floatValue() * (1.0F - (float) Math.pow(1.0F - progress, 4.0D)));
            float fade = 1.0F - progress;
            int centerColor = pack(base, alphaScale * fade);
            int edgeColor = pack(base, alphaScale * fade * EDGE_ALPHA);

            stack.pushPose();
            stack.translate(circle.position().x - camera.x,
                    circle.position().y + GROUND_OFFSET - camera.y,
                    circle.position().z - camera.z);
            emitDisc(buffer, stack.last().pose(), currentRadius, centerColor, edgeColor);
            stack.popPose();

            rings.add(new Ring(circle.position(), currentRadius, centerColor, age / 1000.0D));
        }
        layer.draw(buffer.buildOrThrow());
    }

    /**
     * 来源的环绕文字：3D 里逐字形沿圆周排布；这里把圆心与半径投影到屏幕后，
     * 用 Skija 在同一个屏幕上沿同一圆周重复绘制（重复次数 = 周长 / 文本宽度）。
     */
    @Listen
    private void onRender2D(Render2DEvent event) {
        if (!mode.is(Mode.TEXT) || rings.isEmpty()) {
            return;
        }
        String label = text.get();
        if (label == null || label.isBlank()) {
            return;
        }
        // 来源 在文本后补两个空格后再环绕填充（RenderSupport_203.h = "  "）。
        String run = label + "  ";
        Canvas canvas = event.canvas();

        for (Ring ring : rings) {
            if (ring.radius() <= 1.0E-4F) {
                continue;
            }
            // 圆环贴地，投影到屏幕是一个椭圆：分别用 +X / +Z 方向的半径采样长短轴。
            Vector3f center = WorldToScreen.getWorldPositionToScreen(ring.position());
            Vector3f edgeX = WorldToScreen.getWorldPositionToScreen(
                    ring.position().add(ring.radius(), 0.0D, 0.0D));
            Vector3f edgeZ = WorldToScreen.getWorldPositionToScreen(
                    ring.position().add(0.0D, 0.0D, ring.radius()));
            if (center == null || edgeX == null || edgeZ == null) {
                continue;
            }
            float dx = edgeX.x - center.x;
            float dy = edgeX.y - center.y;
            float radiusX = (float) Math.sqrt(dx * dx + dy * dy);
            float dzx = edgeZ.x - center.x;
            float dzy = edgeZ.y - center.y;
            float radiusZ = (float) Math.sqrt(dzx * dzx + dzy * dzy);
            if (radiusX < MIN_TEXT_RING_PIXELS || radiusZ < MIN_TEXT_RING_PIXELS) {
                continue;
            }
            float pixelsPerBlock = (radiusX + radiusZ) / (2.0F * ring.radius());
            float textPixels = (float) (textSize.get() * pixelsPerBlock);
            if (textPixels < 3.0F) {
                continue;
            }
            float runWidth = SkijaUi.textWidth(run, textPixels);
            if (runWidth < 1.0F) {
                continue;
            }

            float circumference = (float) (Math.PI * (radiusX + radiusZ));
            int repeats = Math.max(1, Math.round(circumference / runWidth));
            double step = Math.PI * 2.0D / repeats;
            double spin = Math.toRadians(spinSpeed.get() * ring.ageSeconds());

            for (int i = 0; i < repeats; i++) {
                double angle = spin + step * i;
                float x = center.x + (float) (Math.cos(angle) * radiusX);
                float y = center.y + (float) (Math.sin(angle) * radiusZ);
                canvas.save();
                canvas.translate(x, y);
                SkijaUi.text(canvas, run, -runWidth * 0.5F, -textPixels * 0.5F,
                        textPixels, ring.argb(), textPixels);
                canvas.restore();
            }
        }
    }

    /** 三角扇圆盘：中心为实心色，边缘接近透明（替代 来源的 jump_circle.png）。 */
    private static void emitDisc(BufferBuilder buffer, Matrix4f matrix, float radius,
                                 int centerColor, int edgeColor) {
        for (int i = 0; i < DISC_SEGMENTS; i++) {
            double from = Math.PI * 2.0D * i / DISC_SEGMENTS;
            double to = Math.PI * 2.0D * (i + 1) / DISC_SEGMENTS;
            buffer.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(centerColor);
            buffer.addVertex(matrix, (float) (Math.cos(from) * radius), 0.0F,
                    (float) (Math.sin(from) * radius)).setColor(edgeColor);
            buffer.addVertex(matrix, (float) (Math.cos(to) * radius), 0.0F,
                    (float) (Math.sin(to) * radius)).setColor(edgeColor);
        }
    }

    private static int pack(Color color, float alpha) {
        int a = Mth.clamp((int) (255.0F * alpha), 0, 255);
        return (a << 24) | (color.getRGB() & 0xFFFFFF);
    }

    private long lifetimeMillis() {
        return (long) (duration.get() * 1000.0D);
    }

    private void ensureLevel() {
        if (trackedLevel == mc.level) {
            return;
        }
        trackedLevel = mc.level;
        circles.clear();
        rings.clear();
        groundPositions.clear();
    }
}
