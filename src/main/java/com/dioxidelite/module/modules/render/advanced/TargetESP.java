package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.AttackEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.modules.combat.KillAura;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.util.render.ColorUtils;
import com.dioxidelite.util.render.Render3DUtils;
import com.dioxidelite.util.render.WorldToScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 移植自 来源客户端 {@code features/render/TargetESP.java}：在 KillAura 目标身上绘制
 * "环绕晶体 + 辉光"（来源 Style 默认值 {@code FeatureMode_246.i}）或"环绕碎片粒子"
 * （来源 Style {@code FeatureMode_246.e}）。全部几何都在世界空间 3D 层
 * （{@link Render3DEvent}）绘制，不需要任何 mixin。
 * <p>
 * 1:1 对应：Style、Color(0xFF78FF0F)、Glow color(0xFF78FF0F)、Glow(75%/0~300)、
 * Glow size(70%/25~300)、Crystal opacity(100%/0~100)、打击事件（来源 收到攻击事件时
 * 触发动画 {@code RenderSupport_152}，寿命常量 260ms）、碎片数量上限 400、
 * 每帧碎片生成公式 {@code (3 + rand(4)) * speed}、碎片重力 {@code -0.0022}、
 * 速度阻尼 {@code 0.96}、生命周期 {@code 34 + rand(28)} tick、18 个环绕晶体、
 * 角度步进 {@code i * 20°}（来源 {@code RenderSupport_158}）均按反编译常量保留。
 */
public final class TargetESP extends Module {

    // PORT-NOTE: 需要自定义着色器/纹理渲染层（来源 custom:effect/target_bloom.png 的 bloom 四边形 + additive 混合），
    // 本端口只实现了不依赖贴图的等价几何（三圈光环圆柱 + 18 个填充小方晶 + 打击扩散环 + 碎片粒子拖尾），
    // 使用 Render3DUtils 的绘制管线并保持 0xFF78FF0F 配色与全部数值参数。

    public static final TargetESP INSTANCE = new TargetESP();

    /** 来源 {@code FeatureMode_246}：SHARDS = 环绕碎片（原常量 e），CRYSTAL = 晶体 + 辉光（原常量 i，默认）。 */
    public enum Style {
        SHARDS,
        CRYSTAL
    }

    /** 来源 {@code RenderSupport_151.a} 的碎片数量上限。 */
    private static final int MAX_SHARDS = 400;
    /** 来源 {@code RenderSupport_152.f} 的打击动画寿命（毫秒）。 */
    private static final long STRIKE_TIME_MS = 260L;
    /** 来源 {@code TargetESP.L(float)} 里对帧间隔的上限（100ms）。 */
    private static final long FRAME_CAP_MS = 100L;
    /** 来源 {@code RenderSupport_154} 每帧绘制的环绕晶体数量（18）。 */
    private static final int CRYSTAL_SHARDS = 18;
    /** 来源 "redOnImpact" 命中变红用的颜色。 */
    private static final Color IMPACT_COLOR = new Color(255, 64, 48, 255);

    // ------------------------------------------------------------------ 设置
    // 顺序与来源 TargetESP 构造函数一致：Style -> Color -> Glow color -> Glow -> Glow size -> Crystal opacity。

    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.CRYSTAL));

    public final ColorSetting color = add(new ColorSetting("Color", new Color(120, 255, 15, 255)));

    public final ColorSetting glowColor = add(new ColorSetting("Glow Color", new Color(120, 255, 15, 255))
            .visibleWhen(() -> style.is(Style.CRYSTAL)));

    public final DoubleSetting glow = add(new DoubleSetting("Glow", 75.0, 0.0, 300.0, 5.0)
            .visibleWhen(() -> style.is(Style.CRYSTAL)));

    public final DoubleSetting glowSize = add(new DoubleSetting("Glow Size", 70.0, 25.0, 300.0, 5.0)
            .visibleWhen(() -> style.is(Style.CRYSTAL)));

    public final DoubleSetting crystalOpacity = add(new DoubleSetting("Crystal Opacity", 100.0, 0.0, 100.0, 5.0)
            .visibleWhen(() -> style.is(Style.CRYSTAL)));

    // ---- 打击特效（来源 FeatureSupport_297：enabled / color / intensity / reach）----
    public final BooleanSetting strike = add(new BooleanSetting("Strike", true)
            .visibleWhen(() -> style.is(Style.CRYSTAL)));

    public final ColorSetting strikeColor = add(new ColorSetting("Strike Color", new Color(255, 255, 255, 255))
            .visibleWhen(() -> style.is(Style.CRYSTAL) && strike.get()));

    public final DoubleSetting strikeIntensity = add(new DoubleSetting("Strike Intensity", 100.0, 0.0, 100.0, 5.0)
            .visibleWhen(() -> style.is(Style.CRYSTAL) && strike.get()));

    public final DoubleSetting strikeReach = add(new DoubleSetting("Strike Reach", 100.0, 0.0, 300.0, 5.0)
            .visibleWhen(() -> style.is(Style.CRYSTAL) && strike.get()));

    // ---- 碎片粒子参数（来源 FeatureSupport_295 -> RenderSupport_156 的 15 个字段）----
    public final DoubleSetting shardSize = add(new DoubleSetting("Shard Size", 1.0, 0.2, 4.0, 0.1)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting shardSpeed = add(new DoubleSetting("Shard Speed", 1.0, 0.1, 8.0, 0.1)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting startPhase = add(new DoubleSetting("Start Phase", 0.0, 0.0, 360.0, 5.0)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting shardGlow = add(new DoubleSetting("Shard Glow", 100.0, 0.0, 300.0, 5.0)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final BooleanSetting gradient = add(new BooleanSetting("Gradient", true)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final BooleanSetting trail = add(new BooleanSetting("Trail", true)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting trailLength = add(new DoubleSetting("Trail Length", 1.0, 0.1, 4.0, 0.1)
            .visibleWhen(() -> style.is(Style.SHARDS) && trail.get()));

    public final DoubleSetting trailOpacity = add(new DoubleSetting("Trail Opacity", 70.0, 0.0, 100.0, 5.0)
            .visibleWhen(() -> style.is(Style.SHARDS) && trail.get()));

    public final BooleanSetting particles = add(new BooleanSetting("Particles", true)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting particleHeight = add(new DoubleSetting("Particle Height", 1.0, 0.0, 3.0, 0.05)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting particleLife = add(new DoubleSetting("Particle Life", 1.0, 0.2, 4.0, 0.1)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final BooleanSetting redOnImpact = add(new BooleanSetting("Red On Impact", false)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting impactFadeIn = add(new DoubleSetting("Impact Fade In", 0.1, 0.0, 1.0, 0.05)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting impactFadeOut = add(new DoubleSetting("Impact Fade Out", 0.6, 0.0, 1.0, 0.05)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    public final DoubleSetting impactIntensity = add(new DoubleSetting("Impact Intensity", 1.5, 0.0, 4.0, 0.1)
            .visibleWhen(() -> style.is(Style.SHARDS)));

    // ------------------------------------------------------------------ 状态

    private final List<Shard> shards = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrameNanos;
    private long lastImpactMs;
    private double spawnBudget;

    private TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        resetState();
    }

    private void resetState() {
        shards.clear();
        lastFrameNanos = 0L;
        lastImpactMs = 0L;
        spawnBudget = 0.0;
    }

    /** 来源的 {@code EventSupport_617}（攻击事件）里刷新打击动画。 */
    @Listen
    private void onAttack(AttackEvent event) {
        if (event.getAttacker() != mc.player) {
            return;
        }
        LivingEntity target = killAuraTarget();
        if (target == null || event.getTarget() != target) {
            return;
        }
        lastImpactMs = System.currentTimeMillis();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        long nowNanos = System.nanoTime();
        float deltaMs = lastFrameNanos == 0L
                ? 0.0f
                : Math.min((nowNanos - lastFrameNanos) / 1_000_000.0f, FRAME_CAP_MS);
        lastFrameNanos = nowNanos;

        LivingEntity target = killAuraTarget();
        long nowMs = System.currentTimeMillis();

        if (!style.is(Style.CRYSTAL)) {
            updateShards(deltaMs);
            if (target != null) {
                spawnShards(target, deltaMs);
            }
            renderShards(event.getPoseStack(), nowMs);
            return;
        }

        shards.clear();
        if (target == null) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        renderCrystal(event.getPoseStack(), target, WorldToScreen.interpolate(target, partialTick), nowMs);
    }

    // ------------------------------------------------------------------ CRYSTAL

    private void renderCrystal(PoseStack stack, LivingEntity target, Vec3 foot, long nowMs) {
        double height = Math.max(0.2, target.getBbHeight());
        double baseRadius = Math.max(0.3, target.getBbWidth() * 0.86 + 0.18);
        double sizeScale = Mth.clamp(glowSize.get() / 100.0, 0.25, 3.0);
        double ringRadius = baseRadius * (0.55 + 0.75 * sizeScale);
        float glowFactor = (float) Mth.clamp(glow.get() / 100.0, 0.0, 3.0);
        float opacity = (float) Mth.clamp(crystalOpacity.get() / 100.0, 0.0, 1.0);

        Color halo = glowColor.get();
        int haloAlpha = Math.round(halo.getAlpha() * glowFactor * opacity);

        // 来源 RenderSupport_154 的 bloom 光环：三层向外扩散的辉光。
        for (int ring = 0; ring < 3 && haloAlpha > 4; ring++) {
            double radius = ringRadius * (1.0 + ring * 0.30);
            int alpha = Math.round(haloAlpha / (float) (ring + 1));
            if (alpha <= 4) {
                continue;
            }
            Render3DUtils.drawCylinder(stack, foot, radius, height, argb(halo, alpha), 1.6f, 24);
        }

        // 来源 RenderSupport_154 的 18 个环绕晶体：角度 = i * 20° + 时间相位（RenderSupport_158）。
        double spinDegrees = (nowMs % 360_000L) / 2.5 * 0.3;
        int crystalArgb = argb(color.get(), Math.round(color.get().getAlpha() * opacity));
        int edgeArgb = argb(halo, Math.min(255, haloAlpha + 48));

        for (int i = 0; i < CRYSTAL_SHARDS; i++) {
            double angle = Math.toRadians(spinDegrees + i * 20.0);
            double radius = ringRadius * (0.88 + 0.22 * Math.sin(Math.toRadians(spinDegrees * 2.0 + i * 20.0)));
            double vertical = (Math.sin(Math.toRadians(spinDegrees * 1.3 + i * 47.0)) + 1.0) * 0.5;
            double y = foot.y + height * (0.12 + 0.76 * vertical);
            Vec3 center = new Vec3(foot.x + Math.cos(angle) * radius, y, foot.z + Math.sin(angle) * radius);
            double half = (0.045 + 0.035 * (0.5 + 0.5 * Math.sin(Math.toRadians(spinDegrees * 3.0 + i * 40.0)))) * sizeScale;
            AABB shard = new AABB(center.x - half, center.y - half, center.z - half,
                    center.x + half, center.y + half, center.z + half);
            Render3DUtils.drawFilledBox(shard, crystalArgb);
            Render3DUtils.drawOutlineBox(stack, shard, edgeArgb, 1.0f);
        }

        renderStrike(stack, foot, height, ringRadius, nowMs);
    }

    private void renderStrike(PoseStack stack, Vec3 foot, double height, double ringRadius, long nowMs) {
        if (!strike.get() || lastImpactMs == 0L) {
            return;
        }
        long elapsed = nowMs - lastImpactMs;
        if (elapsed < 0L || elapsed > STRIKE_TIME_MS) {
            return;
        }

        float progress = elapsed / (float) STRIKE_TIME_MS;
        double reach = Mth.clamp(strikeReach.get() / 100.0, 0.0, 3.0);
        double radius = ringRadius * (0.7 + 1.5 * progress) * reach;
        float strength = (float) Mth.clamp(strikeIntensity.get() / 100.0, 0.0, 1.0);
        Color strikeColour = strikeColor.get();
        int alpha = Math.round(strikeColour.getAlpha() * strength * (1.0f - progress));
        if (alpha <= 4) {
            return;
        }
        Render3DUtils.drawCylinder(stack, foot, radius, height * 0.7, argb(strikeColour, alpha), 2.0f, 24);
    }

    // ------------------------------------------------------------------ SHARDS

    private void spawnShards(LivingEntity target, float deltaMs) {
        double perSecond = 20.0 * Math.max(0.15, shardSpeed.get());
        spawnBudget += Math.max(0.0f, deltaMs) / 1000.0 * perSecond;
        int guard = 0;
        while (spawnBudget >= 1.0 && shards.size() < MAX_SHARDS && guard++ < 64) {
            spawnBudget -= 1.0;
            int count = 3 + random.nextInt(4);
            for (int i = 0; i < count && shards.size() < MAX_SHARDS; i++) {
                shards.add(createShard(target));
            }
        }
    }

    /** 来源 {@code RenderSupport_151.L(EntityLivingBase, float, RenderSupport_156)} 的等价实现。 */
    private Shard createShard(LivingEntity target) {
        double height = Math.max(0.2, target.getBbHeight());
        double minY = target.getY() + 0.03;
        double maxY = target.getY() + height - 0.03;
        double heightSpread = Mth.clamp(particleHeight.get(), 0.0, 3.0);
        double phaseClock = (System.currentTimeMillis() % 3_600_000L) * 0.36;
        double centrePhase = (Math.sin(Math.toRadians(startPhase.get() + phaseClock)) + 1.0) * 0.5;
        // 来源: particleHeight >= 0.98 时在整个身高上均匀分布，否则围绕当前相位聚集。
        double vertical = heightSpread >= 0.98
                ? random.nextDouble()
                : Mth.clamp(centrePhase + (random.nextDouble() - 0.5) * heightSpread, 0.0, 1.0);
        double y = minY + (maxY - minY) * vertical;

        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = (target.getBbWidth() * 0.86 + 0.18)
                * Math.max(0.1, shardSize.get())
                * (0.88 + random.nextDouble() * 0.22);
        double x = target.getX() + Math.cos(angle) * radius;
        double z = target.getZ() + Math.sin(angle) * radius;

        Shard shard = new Shard();
        shard.x = x;
        shard.y = y;
        shard.z = z;
        shard.prevX = x;
        shard.prevY = y;
        shard.prevZ = z;
        double tangent = (random.nextDouble() - 0.5) * 0.01;
        shard.motionX = Math.cos(angle) * tangent;
        shard.motionZ = Math.sin(angle) * tangent;
        shard.motionY = -8.0E-4 - random.nextDouble() * 0.006;
        shard.floorY = target.getY() - 0.08;
        shard.age = 0.0;
        shard.life = Math.max(1, (int) Math.round((34.0 + random.nextInt(28)) * Math.max(0.1, particleLife.get())));
        shard.halfSize = (float) ((0.0065 + random.nextFloat() * 0.014) * Math.max(0.1, shardSize.get()));
        shard.hot = redOnImpact.get() && target.hurtTime > 0;
        return shard;
    }

    /** 来源 {@code RenderSupport_151.L()}：阻尼 0.96、重力 -0.0022、寿命到期或落地即回收。 */
    private void updateShards(float deltaMs) {
        double ticks = Math.max(0.0f, deltaMs) / 50.0;
        Iterator<Shard> iterator = shards.iterator();
        while (iterator.hasNext()) {
            Shard shard = iterator.next();
            shard.prevX = shard.x;
            shard.prevY = shard.y;
            shard.prevZ = shard.z;
            shard.x += shard.motionX;
            shard.y += shard.motionY;
            shard.z += shard.motionZ;
            shard.motionX *= 0.96;
            shard.motionZ *= 0.96;
            shard.motionY -= 0.0022;
            shard.age += ticks;
            if (shard.age >= shard.life || shard.y < shard.floorY) {
                iterator.remove();
            }
        }
    }

    private void renderShards(PoseStack stack, long nowMs) {
        if (shards.isEmpty()) {
            return;
        }

        float impact = impactEnvelope(nowMs);
        float glowFactor = (float) Mth.clamp(shardGlow.get() / 100.0, 0.0, 3.0);
        float trailAlpha = (float) Mth.clamp(trailOpacity.get() / 100.0, 0.0, 1.0);
        double trailStretch = Math.max(1.0, trailLength.get());
        boolean drawTrail = trail.get();
        boolean drawParticles = particles.get();

        for (Shard shard : shards) {
            float progress = Mth.clamp((float) (shard.age / shard.life), 0.0f, 1.0f);
            float fade = 1.0f - progress;
            Color base = shard.hot ? IMPACT_COLOR : color.get();
            Color tinted = gradient.get() ? ColorUtils.interpolate(base, glowColor.get(), progress) : base;
            float brightness = fade * Math.min(1.0f, Math.max(0.05f, glowFactor))
                    * (1.0f + (float) (impactIntensity.get() - 1.0) * impact);
            int alpha = Math.round(tinted.getAlpha() * brightness);
            if (alpha <= 2) {
                continue;
            }

            if (drawTrail) {
                Vec3 from = new Vec3(shard.prevX, shard.prevY, shard.prevZ);
                Vec3 to = new Vec3(shard.x, shard.y, shard.z);
                if (trailStretch > 1.0) {
                    from = from.add(from.subtract(to).scale(trailStretch - 1.0));
                }
                Render3DUtils.drawLine(stack, from, to, argb(tinted, Math.round(alpha * trailAlpha)), 1.5f);
            }

            if (drawParticles) {
                double half = Math.max(0.004, shard.halfSize * (1.0 + 0.6 * impact));
                AABB box = new AABB(shard.x - half, shard.y - half, shard.z - half,
                        shard.x + half, shard.y + half, shard.z + half);
                Render3DUtils.drawFilledBox(box, argb(tinted, alpha));
            }
        }
    }

    /** 来源 {@code RenderSupport_152} 的打击包络：先 fadeIn 后 fadeOut。 */
    private float impactEnvelope(long nowMs) {
        if (lastImpactMs == 0L) {
            return 0.0f;
        }
        long elapsed = nowMs - lastImpactMs;
        if (elapsed < 0L || elapsed > STRIKE_TIME_MS) {
            return 0.0f;
        }

        float progress = elapsed / (float) STRIKE_TIME_MS;
        double fadeIn = Mth.clamp(impactFadeIn.get(), 0.0, 1.0);
        if (progress < fadeIn) {
            return (float) (progress / Math.max(fadeIn, 1.0e-4));
        }
        double fadeOut = Math.max(1.0e-4, Mth.clamp(impactFadeOut.get(), 0.0, 1.0));
        return (float) Mth.clamp(1.0 - (progress - fadeIn) / fadeOut, 0.0, 1.0);
    }

    // ------------------------------------------------------------------ 工具

    /** 来源 {@code 来源Client.l.u.D()}：取 KillAura 当前锁定的目标。 */
    private LivingEntity killAuraTarget() {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        LivingEntity target = KillAura.INSTANCE.target;
        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != mc.level) {
            return null;
        }
        return target;
    }

    private static int argb(Color color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color.getRGB() & 0xFFFFFF);
    }

    /** 来源 {@code RenderSupport_157} 的等价物：一个环绕碎片。 */
    private static final class Shard {
        private double x;
        private double y;
        private double z;
        private double prevX;
        private double prevY;
        private double prevZ;
        private double motionX;
        private double motionY;
        private double motionZ;
        private double floorY;
        private double age;
        private int life;
        private float halfSize;
        private boolean hot;
    }
}
