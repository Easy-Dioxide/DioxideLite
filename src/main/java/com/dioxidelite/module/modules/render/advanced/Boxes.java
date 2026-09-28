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
import com.dioxidelite.util.render.Render3DUtils;
import com.dioxidelite.util.render.WorldToScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 给实体套包围盒（移植自 来源客户端 {@code Boxes}）。
 * <p>
 * 两种形状：{@code RECTANGLE} 在 Render2D 层用 Skija 画屏幕矩形（由
 * {@link WorldToScreen#getEntityPositionsOn2D} 投影），{@code CUBOID} 在 Render3D 层用
 * {@link Render3DUtils} 画世界空间立方体。{@code LINES_FILL} 样式在描边下面再叠一层
 * 半透明黑色填充（对应 来源客户端 的 Fill opacity）。
 * 目标分组沿用 来源客户端 的 TargetType：Self / Enemy / Team / Mobs（Team = 好友）。
 */
public final class Boxes extends Module {

    public static final Boxes INSTANCE = new Boxes();

    /** 来源客户端 FeatureMode_280：平面矩形或世界里的立方体。 */
    public enum Shape {
        RECTANGLE,
        CUBOID
    }

    /** 来源客户端 FeatureMode_282：只有线，或线 + 半透明填充。 */
    public enum Style {
        LINES,
        LINES_FILL
    }

    /** 来源客户端 TargetType 的四组目标。 */
    private enum Kind {
        SELF,
        ENEMY,
        TEAM,
        MOBS
    }

    private record Target(LivingEntity entity, Kind kind) {
    }

    /** 来源客户端 的 0.06 包围盒外扩。 */
    private static final double BOX_INFLATE = 0.06;
    /** 潜行玩家顶部压掉的固定高度（来源客户端: min(0.125, height/2)）。 */
    private static final double CROUCH_CLIP = 0.125;

    public final EnumSetting<Shape> shape = add(new EnumSetting<>("Shape", Shape.RECTANGLE));
    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.LINES));
    public final DoubleSetting fillOpacity = add(new DoubleSetting("Fill opacity", 18.0, 1.0, 60.0, 1.0)
            .visibleWhen(() -> style.is(Style.LINES_FILL)));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line width", 1.5, 0.5, 5.0, 0.5));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through walls", true)
            .visibleWhen(() -> shape.is(Shape.CUBOID)));
    public final DoubleSetting distance = add(new DoubleSetting("Distance", 256.0, 8.0, 256.0, 8.0));
    public final BooleanSetting self = add(new BooleanSetting("Self", false));
    public final BooleanSetting enemies = add(new BooleanSetting("Enemies", true));
    public final BooleanSetting team = add(new BooleanSetting("Team", true));
    public final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));
    public final ColorSetting selfColor = add(new ColorSetting("Self color", new Color(120, 215, 255, 220)));
    public final ColorSetting enemyColor = add(new ColorSetting("Enemy color", new Color(255, 80, 80, 220)));
    public final ColorSetting teamColor = add(new ColorSetting("Team color", new Color(80, 255, 80, 220)));
    public final ColorSetting mobColor = add(new ColorSetting("Mob color", new Color(255, 220, 120, 220)));

    private Boxes() {
        super("Boxes", Category.RENDER);
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer() || !shape.is(Shape.CUBOID)) {
            return;
        }
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float thickness = lineWidth.get().floatValue();
        int fillColor = fillColor();
        boolean seeThrough = throughWalls.get();

        for (Target target : collectTargets()) {
            if (!seeThrough && !mc.player.hasLineOfSight(target.entity())) {
                continue;
            }
            AABB box = interpolatedBox(target.entity(), partialTick);
            if (style.is(Style.LINES_FILL)) {
                Render3DUtils.drawFilledBox(box, fillColor);
            }
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, colorOf(target.kind()), thickness);
        }
    }

    // PORT-NOTE: Render3DUtils 的 3D 管线恒为透视绘制，"Through walls" 关闭时这里改用
    //             hasLineOfSight 过滤目标（等价于只画看得见的实体，但不是真正的深度测试）。

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (noPlayer() || !shape.is(Shape.RECTANGLE)) {
            return;
        }
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float thickness = lineWidth.get().floatValue();
        int fillColor = fillColor();
        Canvas canvas = event.canvas();

        for (Target target : collectTargets()) {
            Vector4d projected = WorldToScreen.getEntityPositionsOn2D(target.entity(), partialTick);
            if (projected == null || projected.z - projected.x < 2.0 || projected.w - projected.y < 2.0) {
                continue;
            }
            float left = (float) projected.x;
            float top = (float) projected.y;
            float width = (float) (projected.z - projected.x);
            float height = (float) (projected.w - projected.y);

            if (style.is(Style.LINES_FILL)) {
                SkijaUi.fill(canvas, left, top, width, height, fillColor);
            }

            int color = colorOf(target.kind()).getRGB();
            float horizontalThickness = Math.min(thickness, width);
            float verticalThickness = Math.min(thickness, height);
            SkijaUi.fill(canvas, left, top, width, horizontalThickness, color);
            SkijaUi.fill(canvas, left, top + height - horizontalThickness, width, horizontalThickness, color);
            if (height > verticalThickness * 2.0F) {
                SkijaUi.fill(canvas, left, top + horizontalThickness, verticalThickness,
                        height - horizontalThickness * 2.0F, color);
                SkijaUi.fill(canvas, left + width - verticalThickness, top + horizontalThickness,
                        verticalThickness, height - horizontalThickness * 2.0F, color);
            }
        }
    }

    /** 收集当前开关下、距离内的目标，按距离从远到近排序（近的画在上层）。 */
    private List<Target> collectTargets() {
        List<Target> targets = new ArrayList<>();
        double maxRangeSq = distance.get() * distance.get();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            Kind kind = kindOf(living);
            if (kind == null || !enabled(kind)) {
                continue;
            }
            if (mc.player.distanceToSqr(living) > maxRangeSq) {
                continue;
            }
            targets.add(new Target(living, kind));
        }
        targets.sort(Comparator.comparingDouble(
                (Target target) -> mc.player.distanceToSqr(target.entity())).reversed());
        return targets;
    }

    /** 来源客户端 TargetType.L：Self（仅第三人称）/ Enemy / Team（好友）/ Mobs。 */
    private Kind kindOf(LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof ArmorStand) {
            return null;
        }
        if (entity instanceof Player player) {
            if (player.isSpectator()) {
                return null;
            }
            if (player == mc.player) {
                return mc.options.getCameraType().isFirstPerson() ? null : Kind.SELF;
            }
            return FriendManager.INSTANCE.isFriend(player) ? Kind.TEAM : Kind.ENEMY;
        }
        return Kind.MOBS;
    }

    private boolean enabled(Kind kind) {
        return switch (kind) {
            case SELF -> self.get();
            case ENEMY -> enemies.get();
            case TEAM -> team.get();
            case MOBS -> mobs.get();
        };
    }

    private Color colorOf(Kind kind) {
        return switch (kind) {
            case SELF -> selfColor.get();
            case ENEMY -> enemyColor.get();
            case TEAM -> teamColor.get();
            case MOBS -> mobColor.get();
        };
    }

    /** 插值到当前帧、外扩 0.06，并按 来源客户端 的做法压掉潜行玩家的顶部。 */
    private static AABB interpolatedBox(LivingEntity entity, float partialTick) {
        Vec3 position = WorldToScreen.interpolate(entity, partialTick);
        AABB box = entity.getBoundingBox()
                .move(position.x - entity.getX(), position.y - entity.getY(), position.z - entity.getZ())
                .inflate(BOX_INFLATE);
        if (entity instanceof Player player && player.isShiftKeyDown()) {
            double trim = Math.min(CROUCH_CLIP, (box.maxY - box.minY) * 0.5);
            box = new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY - trim, box.maxZ);
        }
        return box;
    }

    /** 来源客户端 的填充色是半透明黑，透明度来自 Fill opacity (%)。 */
    private int fillColor() {
        int alpha = Mth.clamp((int) (255.0 * fillOpacity.get() / 100.0), 0, 255);
        return alpha << 24;
    }
}
