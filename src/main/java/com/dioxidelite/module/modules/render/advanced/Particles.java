package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.AttackEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.util.render.ColorUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * 移植自 来源客户端 {@code features/render/Particles.java}：命中目标与走动时喷出的
 * 发光萤火粒子（世界空间公告板四边形，画在 Render3DEvent 的 3D 层）。
 * <p>
 * 1:1 对应：Spawn at（来源的 MultiSelectSetting → 这里映射成两个布尔：命中 / 走动）、
 * Particles on hit 3/1..25、Particles while walking 2/1..15、Random color、
 * Size 1.0/0.5..2.5、Duration 1.0/0.25..3.0、Physics、Physics speed 1.0/0.25..3.0。
 * 生命期沿用 来源的 {@code RenderSupport_148.L}：500ms 淡入 → 保持 → 500ms 淡出，
 * 峰值 alpha 0.5019608，再取 pow(x, 0.72) 作为最终 alpha。
 * <p>
 * 说明：来源 用主题强调色+次色的色相振荡上色，Dioxide 没有对应主题色对，
 * 非随机模式下用 {@link ColorUtils#rainbow(long, int)} 的时间色相同步近似；
 * Random color 打开时逐粒子随机 HSB（0.75~1.0 饱和度/亮度）。
 */
public final class Particles extends Module {

    public static final Particles INSTANCE = new Particles();

    private static final Identifier PARTICLE_TEXTURE = Identifier.fromNamespaceAndPath(
            DioxideLite.MOD_ID, "textures/particles/firefly.png");

    /** 与来源 一致：粒子四边形半边长 = 0.1 * Size 设置。 */
    private static final double QUAD_HALF_EXTENT = 0.1D;
    /** 来源的片元 alpha 上限。 */
    private static final float PEAK_ALPHA = 0.5019608F;
    /** 来源的淡入 / 淡出时长。 */
    private static final long FADE_MS = 500L;
    private static final long RAINBOW_PERIOD_MS = 2400L;
    private static final float CORE_SCALE = 0.5F;

    private static final RenderPipeline PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(DioxideLite.MOD_ID, "pipeline/custom_particles"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    private static final Function<RenderPipeline, RenderType> PARTICLE_LAYER = Util.memoize(
            renderPipeline -> RenderType.create("dioxidelite_custom_particles", RenderSetup.builder(renderPipeline)
                    .withTexture("Sampler0", PARTICLE_TEXTURE)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .createRenderSetup()));

    // 来源的 MultiSelectSetting("Spawn at", [On hit, While walking]) 在这里拆成两个开关。
    public final BooleanSetting spawnOnHit = add(new BooleanSetting("Spawn on hit", true));
    public final BooleanSetting spawnWhileWalking = add(new BooleanSetting("Spawn while walking", true));
    public final IntSetting particlesOnHit = add(new IntSetting("Particles on hit", 3, 1, 25, 1))
            .visibleWhen(spawnOnHit::get);
    public final IntSetting particlesWhileWalking = add(new IntSetting("Particles while walking", 2, 1, 15, 1))
            .visibleWhen(spawnWhileWalking::get);
    public final BooleanSetting randomColor = add(new BooleanSetting("Random color", false));
    public final DoubleSetting size = add(new DoubleSetting("Size", 1.0, 0.5, 2.5, 0.05));
    public final DoubleSetting duration = add(new DoubleSetting("Duration", 1.0, 0.25, 3.0, 0.05));
    public final BooleanSetting physics = add(new BooleanSetting("Physics", true));
    public final DoubleSetting physicsSpeed = add(new DoubleSetting("Physics speed", 1.0, 0.25, 3.0, 0.05))
            .visibleWhen(physics::get);

    private final List<Firefly> fireflies = new ArrayList<>();
    private final Random random = new Random();

    /** 场上的一颗萤火（来源的 RenderSupport_147）。 */
    private static final class Firefly {
        double x;
        double y;
        double z;
        double vx;
        double vy;
        double vz;
        final long birthMs;
        final int rgb;

        Firefly(double x, double y, double z, double vx, double vy, double vz, long birthMs, int rgb) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.birthMs = birthMs;
            this.rgb = rgb;
        }
    }

    /** 用于检测世界切换（来源的 WorldChangeEvent → 清空粒子）。 */
    private Object lastLevel;

    private Particles() {
        super("Particles", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        clearParticles();
    }

    @Override
    protected void onDisable() {
        clearParticles();
    }

    private void clearParticles() {
        fireflies.clear();
    }

    /** 来源的 EventSupport_617（攻击命中）：在被击中的实体身上喷粒子。 */
    @Listen
    private void onAttack(AttackEvent event) {
        if (!spawnOnHit.get() || mc.player == null || mc.level == null) {
            return;
        }
        if (event.getAttacker() != mc.player) {
            return;
        }
        Entity target = event.getTarget();
        if (target == null) {
            return;
        }
        int count = particlesOnHit.get();
        for (int i = 0; i < count; i++) {
            double y = target.getY() + random.nextDouble() * target.getBbHeight();
            spawn(target.getX(), y, target.getZ(),
                    randomRange(-2.0D, 2.0D), 0.0D, randomRange(-2.0D, 2.0D));
        }
    }

    /** 来源的 TickEndEvent：走动时在自己脚下喷粒子（第一人称不刷，原实现即如此）。 */
    @Listen
    private void onTick(TickEvent.Post event) {
        if (!spawnWhileWalking.get() || mc.player == null || mc.level == null) {
            return;
        }
        if (mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (mc.player.xOld == mc.player.getX()
                && mc.player.yOld == mc.player.getY()
                && mc.player.zOld == mc.player.getZ()) {
            return;
        }
        int count = particlesWhileWalking.get();
        double boost = 2.0D * (1.0D + random.nextDouble());
        for (int i = 0; i < count; i++) {
            double x = mc.player.getX() + randomRange(-0.5D, 0.5D);
            double y = mc.player.getY() + random.nextDouble() * mc.player.getBbHeight();
            double z = mc.player.getZ() + randomRange(-0.5D, 0.5D);
            double vx = (mc.player.getDeltaMovement().x + randomRange(-0.1D, 0.1D)) * boost;
            double vz = (mc.player.getDeltaMovement().z + randomRange(-0.1D, 0.1D)) * boost;
            spawn(x, y, z, vx, 0.0D, vz);
        }
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (mc.level == null) {
            return;
        }
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            fireflies.clear();
        }

        long now = System.currentTimeMillis();
        long lifeMs = Math.max(1L, Math.round(duration.get() * 1000.0D));
        Iterator<Firefly> iterator = fireflies.iterator();
        while (iterator.hasNext()) {
            Firefly firefly = iterator.next();
            if (now - firefly.birthMs >= lifeMs) {
                iterator.remove();
                continue;
            }
            if (physics.get()) {
                simulate(firefly, physicsSpeed.get());
            }
        }
        if (fireflies.isEmpty()) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        PoseStack stack = event.getPoseStack();
        float half = (float) (QUAD_HALF_EXTENT * size.get().floatValue());

        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Firefly firefly : fireflies) {
            long age = now - firefly.birthMs;
            float fade = lifeAlpha(age, lifeMs);
            if (fade <= 0.001F) {
                continue;
            }
            int alpha = Math.round(Mth.clamp((float) Math.pow(fade, 0.72D), 0.0F, 1.0F) * 255.0F);
            // 来源 每颗粒子画两层：着色层 + 半尺寸白色核心。
            billboard(stack, buffer, firefly.x - cameraPos.x, firefly.y - cameraPos.y, firefly.z - cameraPos.z,
                    half, (alpha << 24) | (firefly.rgb & 0xFFFFFF), camera);
            billboard(stack, buffer, firefly.x - cameraPos.x, firefly.y - cameraPos.y, firefly.z - cameraPos.z,
                    half * CORE_SCALE, (alpha << 24) | 0xFFFFFF, camera);
        }
        PARTICLE_LAYER.apply(PARTICLE_PIPELINE).draw(buffer.buildOrThrow());
    }

    private void spawn(double x, double y, double z, double vx, double vy, double vz) {
        if (mc.level == null) {
            return;
        }
        fireflies.add(new Firefly(x, y, z, vx, vy, vz, System.currentTimeMillis(), nextColor()));
    }

    private int nextColor() {
        if (randomColor.get()) {
            // 来源：ThemeSupport_064.L(randHue, 0.75 + r * 0.25, 0.75 + r * 0.25, 255)。
            Color color = Color.getHSBColor(random.nextFloat(),
                    0.75F + random.nextFloat() * 0.25F,
                    0.75F + random.nextFloat() * 0.25F);
            return color.getRGB() & 0xFFFFFF;
        }
        return ColorUtils.rainbow(RAINBOW_PERIOD_MS, 255).getRGB() & 0xFFFFFF;
    }

    /** 来源的 RenderSupport_148.L(age, life)：500ms 淡入、500ms 淡出、峰值 0.5019608。 */
    private static float lifeAlpha(long ageMs, long lifeMs) {
        float fade;
        if (ageMs < FADE_MS) {
            fade = (float) ageMs / (float) FADE_MS;
        } else if (ageMs < lifeMs) {
            fade = 1.0F;
        } else if (ageMs < lifeMs + FADE_MS) {
            fade = 1.0F - (float) (ageMs - lifeMs) / (float) FADE_MS;
        } else {
            fade = 0.0F;
        }
        return PEAK_ALPHA * Mth.clamp(fade, 0.0F, 1.0F);
    }

    /**
     * 来源的 RenderSupport_147.d(double)：地面反弹（竖直速度 * -0.7）、撞墙反向
     * （水平速度 * -0.8）、低速阻尼 0.999999^delta 与缓慢重力 5.0E-5 * delta。
     * delta 就是 Physics speed 设置值（来源 同样按帧步进而非帧时长）。
     */
    private void simulate(Firefly firefly, double delta) {
        double radius = QUAD_HALF_EXTENT * size.get() + 1.0E-7D;

        if (firefly.vy <= 0.0D) {
            double below = groundY(firefly.x, firefly.y + firefly.vy * delta - radius, firefly.z);
            if (Double.isFinite(below)) {
                firefly.vx *= 0.999D;
                firefly.vz *= 0.999D;
                firefly.vy *= -0.7D;
            }
        }

        double newY = firefly.y + firefly.vy * delta;
        double floor = groundY(firefly.x, newY, firefly.z);
        if (Double.isFinite(floor)) {
            newY = Math.max(newY, floor + radius);
        }

        if (isSolidAt(firefly.x + firefly.vx * delta, firefly.y, firefly.z)) {
            firefly.vx *= -0.8D;
        }
        if (isSolidAt(firefly.x, firefly.y, firefly.z + firefly.vz * delta)) {
            firefly.vz *= -0.8D;
        }

        firefly.x += firefly.vx * delta;
        firefly.y = newY;
        firefly.z += firefly.vz * delta;

        double drag = Math.pow(0.999999D, delta);
        firefly.vx /= drag;
        firefly.vy -= 5.0E-5D * delta;
        firefly.vz /= drag;
    }

    /** 来源的 RenderSupport_147.L(x, y, z)：实心方块顶面高度，否则 NaN。 */
    private double groundY(double x, double y, double z) {
        if (mc.level == null) {
            return Double.NaN;
        }
        BlockPos pos = BlockPos.containing(x, y - 1.0E-7D, z);
        BlockState state = mc.level.getBlockState(pos);
        if (state.getCollisionShape(mc.level, pos).isEmpty()) {
            return Double.NaN;
        }
        return pos.getY() + 1.0D;
    }

    private boolean isSolidAt(double x, double y, double z) {
        if (mc.level == null) {
            return false;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        return !mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty();
    }

    private void billboard(PoseStack stack, BufferBuilder buffer,
                           double x, double y, double z, float half, int color, Camera camera) {
        float extent = Math.max(half, 0.01F);
        stack.pushPose();
        stack.translate(x, y, z);
        stack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        stack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        Matrix4f matrix = stack.last().pose();
        buffer.addVertex(matrix, -extent, extent, 0.0F).setUv(0.0F, 1.0F).setColor(color);
        buffer.addVertex(matrix, extent, extent, 0.0F).setUv(1.0F, 1.0F).setColor(color);
        buffer.addVertex(matrix, extent, -extent, 0.0F).setUv(1.0F, 0.0F).setColor(color);
        buffer.addVertex(matrix, -extent, -extent, 0.0F).setUv(0.0F, 0.0F).setColor(color);
        stack.popPose();
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
