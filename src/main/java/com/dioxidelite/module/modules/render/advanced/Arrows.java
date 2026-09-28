package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.dioxidelite.util.render.ColorUtils;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.Point;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;

/**
 * 在准星四周画指针，指向范围内的其它玩家（移植自 来源客户端 {@code Arrows}）。
 * <p>
 * 位置与角度在 Render2D 层按当前视角计算：把目标相对坐标旋转进以玩家朝向为"上"的屏幕系，
 * 再用 Skija 画布在半径 {@code Distance} 的圆环上画三角形指针，颜色从 {@code Color} 渐变到
 * {@code Second color}；{@code Glow} 会在指针后面再叠一层放大、低透明度的同形状。
 * 来源客户端 的三张指针贴图（outline / solid / chevron）对应三种 {@code Design}。
 */
public final class Arrows extends Module {

    public static final Arrows INSTANCE = new Arrows();

    /** 来源客户端 的指针贴图：outline / solid / chevron。 */
    public enum Design {
        OUTLINE,
        SOLID,
        CHEVRON
    }

    /** 实心指针的渐变切片数（来源客户端 用三个顶点色，这里用切片近似）。 */
    private static final int GRADIENT_SLICES = 12;
    /** 轮廓/人字形指针每条边的渐变分段数。 */
    private static final int OUTLINE_SEGMENTS = 4;
    /** 打开背包界面时指针环外扩的像素（来源客户端: GuiInventory -> +100px）。 */
    private static final float INVENTORY_EXPANSION = 100.0F;
    /** 指针环半径的平滑时间常数（毫秒），取自 来源客户端 的 90ms。 */
    private static final double SMOOTHING_MS = 90.0;
    /** 帧间隔上限（毫秒），与 来源客户端 的 100ms 一致。 */
    private static final float MAX_FRAME_MS = 100.0F;
    /** 指针形状：尖端到中心 / 底边到中心 / 底边半宽（相对 Size）。 */
    private static final float TIP_LENGTH = 1.0F;
    private static final float BASE_LENGTH = 0.8F;
    private static final float BASE_HALF_WIDTH = 0.65F;

    public final EnumSetting<Design> design = add(new EnumSetting<>("Design", Design.OUTLINE));
    public final DoubleSetting distance = add(new DoubleSetting("Distance", 60.0, 20.0, 150.0, 1.0));
    public final DoubleSetting size = add(new DoubleSetting("Size", 18.0, 8.0, 40.0, 1.0));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(120, 215, 255, 255)));
    public final ColorSetting secondColor = add(new ColorSetting("Second color", new Color(181, 140, 255, 255)));
    public final DoubleSetting opacity = add(new DoubleSetting("Opacity", 49.0, 5.0, 100.0, 1.0));
    public final DoubleSetting glow = add(new DoubleSetting("Glow", 100.0, 0.0, 250.0, 5.0));
    public final DoubleSetting range = add(new DoubleSetting("Range", 128.0, 8.0, 256.0, 8.0));

    private float pointerRadius;
    private long lastFrameNanos;

    private Arrows() {
        super("Arrows", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        pointerRadius = 0.0F;
        lastFrameNanos = 0L;
    }

    @Listen
    private void onRender2D(Render2DEvent event) {
        if (noPlayer()) {
            return;
        }
        // 来源客户端 只在第一人称（gameSettings.thirdPersonView == 0）绘制指针。
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        float deltaMs = frameDeltaMs();
        float targetRadius = distance.get().floatValue()
                + (mc.screen instanceof InventoryScreen ? INVENTORY_EXPANSION : 0.0F);
        pointerRadius += (targetRadius - pointerRadius)
                * (float) (1.0 - Math.exp(-deltaMs / SMOOTHING_MS));

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double selfX = Mth.lerp(partialTick, mc.player.xOld, mc.player.getX());
        double selfZ = Mth.lerp(partialTick, mc.player.zOld, mc.player.getZ());
        float yaw = Mth.lerp(partialTick, mc.player.yRotO, mc.player.getYRot());
        double cosYaw = Math.cos(Math.toRadians(yaw));
        double sinYaw = Math.sin(Math.toRadians(yaw));

        float centerX = event.width() * 0.5F;
        float centerY = event.height() * 0.5F;
        float pointerSize = size.get().floatValue();
        double maxRangeSq = range.get() * range.get();
        Canvas canvas = event.canvas();

        for (Player target : mc.level.players()) {
            if (target == mc.player || !target.isAlive() || target.isInvisible()) {
                continue;
            }
            if (mc.player.distanceToSqr(target) > maxRangeSq) {
                continue;
            }

            double targetX = Mth.lerp(partialTick, target.xOld, target.getX());
            double targetZ = Mth.lerp(partialTick, target.zOld, target.getZ());
            double dx = targetX - selfX;
            double dz = targetZ - selfZ;

            // 旋转进屏幕系：屏幕上方 = 玩家前方，屏幕右方 = 玩家右手。
            // 与 来源客户端 的 atan2(-(dz*cos - dx*sin), -(dx*cos + dz*sin)) 等价。
            float angle = (float) Math.atan2(dx * sinYaw - dz * cosYaw,
                    -(dx * cosYaw + dz * sinYaw));
            float x = centerX + pointerRadius * Mth.cos(angle);
            float y = centerY + pointerRadius * Mth.sin(angle);
            drawPointer(canvas, x, y, angle, pointerSize);
        }
    }

    /** 画一枚指针：Glow 先叠一层放大低透明度的形状，再画本体。 */
    private void drawPointer(Canvas canvas, float x, float y, float angle, float size) {
        float rotation = (float) Math.toDegrees(angle) + 90.0F;
        float opacityScale = opacity.get().floatValue() / 100.0F;
        float glowAmount = glow.get().floatValue() / 100.0F;

        int save = canvas.save();
        try {
            canvas.translate(x, y);
            canvas.rotate(rotation);
            if (glowAmount > 0.01F) {
                float spread = 1.0F + 0.5F * Math.min(glowAmount, 2.0F);
                float glowAlpha = opacityScale * 0.22F * Math.min(glowAmount, 1.5F);
                drawShape(canvas, size * spread, glowAlpha);
            }
            drawShape(canvas, size, opacityScale);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawShape(Canvas canvas, float size, float alphaScale) {
        Color first = color.get();
        Color second = secondColor.get();
        switch (design.get()) {
            case SOLID -> drawSolid(canvas, size, first, second, alphaScale);
            case OUTLINE -> drawOutline(canvas, size, first, second, alphaScale);
            case CHEVRON -> drawChevron(canvas, size, first, second, alphaScale);
        }
    }

    /** 实心三角：沿尖端 -> 底边切成若干梯形，颜色随切片渐变。 */
    private static void drawSolid(Canvas canvas, float size, Color first, Color second, float alphaScale) {
        float tip = -size * TIP_LENGTH;
        float base = size * BASE_LENGTH;
        for (int i = 0; i < GRADIENT_SLICES; i++) {
            float t0 = (float) i / GRADIENT_SLICES;
            float t1 = (float) (i + 1) / GRADIENT_SLICES;
            float y0 = Mth.lerp(t0, tip, base);
            float y1 = Mth.lerp(t1, tip, base);
            float halfWidth0 = BASE_HALF_WIDTH * size * t0;
            float halfWidth1 = BASE_HALF_WIDTH * size * t1;
            int sliceColor = withAlpha(ColorUtils.interpolate(first, second, t0), alphaScale);
            Point[] points = {
                    new Point(-halfWidth0, y0),
                    new Point(halfWidth0, y0),
                    new Point(halfWidth1, y1),
                    new Point(-halfWidth1, y1)
            };
            try (Path path = Path.makePolygon(points, true)) {
                SkijaUi.fillPath(canvas, path, sliceColor);
            }
        }
    }

    /** 空心三角轮廓：两条侧边沿渐变分段，底边用第二个颜色。 */
    private static void drawOutline(Canvas canvas, float size, Color first, Color second, float alphaScale) {
        float tipY = -size * TIP_LENGTH;
        float baseY = size * BASE_LENGTH;
        float halfWidth = BASE_HALF_WIDTH * size;
        float thickness = Math.max(1.0F, size * 0.09F);
        gradientLine(canvas, 0.0F, tipY, -halfWidth, baseY, first, second, alphaScale, thickness);
        gradientLine(canvas, 0.0F, tipY, halfWidth, baseY, first, second, alphaScale, thickness);
        SkijaUi.line(canvas, -halfWidth, baseY, halfWidth, baseY, thickness,
                withAlpha(second, alphaScale));
    }

    /** 人字形箭头：一道 V 形主线 + 内侧一小段第二条颜色。 */
    private static void drawChevron(Canvas canvas, float size, Color first, Color second, float alphaScale) {
        float thickness = Math.max(1.0F, size * 0.13F);
        float tipY = -size * 0.5F;
        float tailY = size * 0.7F;
        float halfWidth = size * 0.6F;
        gradientLine(canvas, 0.0F, tipY, -halfWidth, tailY, first, second, alphaScale, thickness);
        gradientLine(canvas, 0.0F, tipY, halfWidth, tailY, first, second, alphaScale, thickness);
        float inner = 0.55F;
        SkijaUi.line(canvas, -halfWidth * inner, tailY * inner, 0.0F, tipY * inner,
                thickness * 0.75F, withAlpha(second, alphaScale * 0.8F));
        SkijaUi.line(canvas, 0.0F, tipY * inner, halfWidth * inner, tailY * inner,
                thickness * 0.75F, withAlpha(second, alphaScale * 0.8F));
    }

    /** 两点之间按分段插值颜色画线。 */
    private static void gradientLine(Canvas canvas, float x1, float y1, float x2, float y2,
                                     Color first, Color second, float alphaScale, float thickness) {
        for (int i = 0; i < OUTLINE_SEGMENTS; i++) {
            float t0 = (float) i / OUTLINE_SEGMENTS;
            float t1 = (float) (i + 1) / OUTLINE_SEGMENTS;
            SkijaUi.line(canvas,
                    Mth.lerp(t0, x1, x2), Mth.lerp(t0, y1, y2),
                    Mth.lerp(t1, x1, x2), Mth.lerp(t1, y1, y2),
                    thickness,
                    withAlpha(ColorUtils.interpolate(first, second, t0), alphaScale));
        }
    }

    private static int withAlpha(Color color, float scale) {
        int alpha = Mth.clamp((int) (color.getAlpha() * scale), 0, 255);
        return (alpha << 24) | (color.getRGB() & 0xFFFFFF);
    }

    /** 帧间隔（毫秒）：首帧按 16ms 处理，超过 100ms 截断（来源客户端 的做法）。 */
    private float frameDeltaMs() {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L
                ? 16.0F
                : Math.min((now - lastFrameNanos) / 1_000_000.0F, MAX_FRAME_MS);
        lastFrameNanos = now;
        return delta;
    }
}
