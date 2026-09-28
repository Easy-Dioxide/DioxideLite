package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.manager.FriendManager;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * 移植自 来源客户端 {@code features/render/GlowESP.java}：给选中的实体加一层外发光描边，
 * 或一层"液态金属"填充。目标分组使用 来源的 {@code TargetType}
 * （SELF / ENEMY / TEAM / MOBS），每组可单独开关并指定颜色。
 * <p>
 * 1:1 对应：Mode（来源 默认值 {@code FeatureMode_262.H} = 描边；{@code e} = 填充）、
 * Radius(14 / 2~24 px，仅描边模式)、Intensity(40% / 10~100%)、每目标类型开关 + 颜色。
 */
public final class GlowESP extends Module {

    // PORT-NOTE: 需要实体渲染 hook（来源 通过渲染层名 custom_glow_outline / custom_glow_fill / entity_outline
    // 切换自定义辉光着色器，直接作用在实体模型上），本端口只实现了"世界空间投影 + Skija 画布辉光"的等价观感：
    // 在 Render3DEvent 里投影目标包围盒，再在 Render2DEvent 上用 SkijaUi.glowLayer 画出
    // 按 Radius 扩散、按 Intensity 定浓度的外发光。

    public static final GlowESP INSTANCE = new GlowESP();

    /** 来源 {@code FeatureMode_262}：OUTLINE = 外发光描边（原常量 H，默认），FILL = 液态金属填充（原常量 e）。 */
    public enum Mode {
        OUTLINE,
        FILL
    }

    /** 来源 {@code TargetType} 的四个分组。 */
    public enum TargetType {
        SELF,
        ENEMY,
        TEAM,
        MOBS
    }

    // ------------------------------------------------------------------ 设置
    // 顺序与来源 GlowESP 构造函数一致：Mode -> Radius -> Intensity，随后是 FeatureSupport_328 的每类型开关/颜色。

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.OUTLINE));

    public final DoubleSetting radius = add(new DoubleSetting("Radius", 14.0, 2.0, 24.0, 1.0)
            .visibleWhen(() -> mode.is(Mode.OUTLINE)));

    public final DoubleSetting intensity = add(new DoubleSetting("Intensity", 40.0, 10.0, 100.0, 5.0));

    public final BooleanSetting self = add(new BooleanSetting("Self", true));
    public final ColorSetting selfColor = add(new ColorSetting("Self Color", new Color(0xFF6AA0FF, true))
            .visibleWhen(self::get));

    public final BooleanSetting enemy = add(new BooleanSetting("Enemy", true));
    public final ColorSetting enemyColor = add(new ColorSetting("Enemy Color", new Color(0xFFFF5555, true))
            .visibleWhen(enemy::get));

    public final BooleanSetting team = add(new BooleanSetting("Team", true));
    public final ColorSetting teamColor = add(new ColorSetting("Team Color", new Color(0xFF55FF55, true))
            .visibleWhen(team::get));

    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final ColorSetting mobColor = add(new ColorSetting("Mob Color", new Color(0xFFFFCC44, true))
            .visibleWhen(mobs::get));

    // ------------------------------------------------------------------ 状态

    private final List<GlowTarget> drawList = new ArrayList<>();

    private GlowESP() {
        super("GlowESP", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        drawList.clear();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        drawList.clear();
        if (noPlayer()) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!living.isAlive() || living.isRemoved()) {
                continue;
            }
            if (living == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }

            Color color = colorFor(living);
            if (color == null) {
                continue;
            }

            Vector4d projected = WorldToScreen.getEntityPositionsOn2D(living, partialTick);
            if (projected == null) {
                continue;
            }
            float left = (float) projected.x;
            float top = (float) projected.y;
            float width = (float) (projected.z - projected.x);
            float height = (float) (projected.w - projected.y);
            if (width < 1.0f || height < 1.0f) {
                continue;
            }
            drawList.add(new GlowTarget(left, top, width, height, color.getRGB()));
        }
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (drawList.isEmpty()) {
            return;
        }

        Canvas canvas = event.canvas();
        // 来源的 Radius 是"辉光向外扩散的距离"（px），直接作为 Skija 模糊半径。
        float spread = radius.get().floatValue();
        // 来源的 Intensity 是外层辉光浓度（10~100%），映射到 glowLayer 的 1~10 档。
        int strength = Mth.clamp((int) Math.round(intensity.get() / 10.0), 1, 10);
        float opacity = (float) Mth.clamp(intensity.get() / 100.0, 0.0, 1.0);
        boolean fillMode = mode.is(Mode.FILL);

        for (GlowTarget target : drawList) {
            float left = target.left();
            float top = target.top();
            float width = target.width();
            float height = target.height();
            int base = target.color();
            int fill = argb(base, Math.round(alpha(base) * opacity));
            int outline = argb(base, Math.round(alpha(base) * Math.max(opacity, 0.55f)));

            SkijaUi.glowLayer(canvas, left, top, width, height, spread, strength, () -> {
                if (fillMode) {
                    SkijaUi.fill(canvas, left, top, width, height, fill);
                }
                SkijaUi.outline(canvas, left, top, width, height, 0.0f, fillMode ? 1.0f : 1.5f, outline);
            });
        }
    }

    // ------------------------------------------------------------------ 工具

    private Color colorFor(LivingEntity entity) {
        TargetType type = classify(entity);
        if (type == null || !enabled(type)) {
            return null;
        }
        return color(type);
    }

    /** 来源 {@code TargetType.L(EntityLivingBase)} 的目标分组判定。 */
    private TargetType classify(LivingEntity entity) {
        if (entity == mc.player) {
            return TargetType.SELF;
        }
        if (entity instanceof Player player) {
            return FriendManager.INSTANCE.isFriend(player) ? TargetType.TEAM : TargetType.ENEMY;
        }
        return TargetType.MOBS;
    }

    private boolean enabled(TargetType type) {
        return switch (type) {
            case SELF -> self.get();
            case ENEMY -> enemy.get();
            case TEAM -> team.get();
            case MOBS -> mobs.get();
        };
    }

    private Color color(TargetType type) {
        return switch (type) {
            case SELF -> selfColor.get();
            case ENEMY -> enemyColor.get();
            case TEAM -> teamColor.get();
            case MOBS -> mobColor.get();
        };
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int argb(int argb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (argb & 0xFFFFFF);
    }

    /** 一帧里投影好的目标包围盒（屏幕坐标）。 */
    private record GlowTarget(float left, float top, float width, float height, int color) {
    }
}
