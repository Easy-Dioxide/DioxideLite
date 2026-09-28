package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.Render3DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.render.Render3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * 掉落物躺倒 + 自转（移植自 来源客户端 {@code features/render/ItemPhysics}，
 * "Makes dropped items lie and rotate on the ground"）。
 *
 * <p>来源 端只有一个设置 {@code NumberSetting("Rotation speed", 0.0, 0.0, 3.0, 0.1)}（后缀 "x"）：
 * 模型本体的"躺平"与绕 Y 轴自转由渲染钩子完成，贴地角度沿用原版
 * {@link ItemEntity#getSpin(float, float)}（{@code age / 20.0 + bobOffs}，单位为弧度）
 * 再乘以 Rotation speed。</p>
 *
 * <p>本端口实现没有钩子也能做的那一层：在 Render3DEvent（世界空间 3D）里，为视野内的掉落物
 * 画一块贴地的旋转方形投影 —— {@code Rotation speed = 0.0} 时静止躺平（与来源 默认值一致），
 * {@code 1.0} 为原版转速，{@code 3.0} 为三倍速；同一角度通过
 * {@link #groundSpin(ItemEntity, float)} 暴露给未来的渲染钩子复用。</p>
 *
 * <p>Dioxide 端附加设置：Range / Color（来源 直接改模型，不需要绘制范围与颜色）。</p>
 */
// PORT-NOTE: 需要 ItemEntityRenderer 渲染钩子（ItemEntityRenderState.bobOffset / submit 里的模型贴地与自转变换）才能让掉落物模型本体躺倒并旋转；本端口只实现了世界空间 3D 层（Render3DEvent）的贴地旋转投影标记与 groundSpin() 角度计算。
public final class ItemPhysics extends Module {

    public static final ItemPhysics INSTANCE = new ItemPhysics();

    /** 贴地方块的四个角（单位坐标，绘制时按半宽缩放并绕 Y 轴旋转）。 */
    private static final double[][] SQUARE_CORNERS = {
            {-1.0D, -1.0D}, {1.0D, -1.0D}, {1.0D, 1.0D}, {-1.0D, 1.0D}
    };

    /** 贴地投影线的粗细。 */
    private static final float LINE_THICKNESS = 1.5F;

    /** 来源: NumberSetting("Rotation speed", 0.0, 0.0, 3.0, 0.1)，后缀 "x"；0 = 静止躺平，1 = 原版转速。 */
    public final DoubleSetting rotationSpeed = add(new DoubleSetting("Rotation speed", 0.0, 0.0, 3.0, 0.1)
            .displayAs("Rotation speed (x)"));

    /** Dioxide 端附加：投影的可见距离（方块）。 */
    public final DoubleSetting range = add(new DoubleSetting("Range", 32.0, 4.0, 128.0, 1.0));

    /** Dioxide 端附加：地面投影的颜色 / 透明度。 */
    public final ColorSetting color = add(new ColorSetting("Color", new Color(255, 255, 255, 170)));

    private ItemPhysics() {
        super("ItemPhysics", Category.RENDER);
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        PoseStack stack = event.getPoseStack();
        double maxDistSq = range.get() * range.get();

        Color base = color.get();
        int lineArgb = color.argb();
        Color fillArgb = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                Math.max(20, base.getAlpha() / 3));

        for (Entity candidate : mc.level.entitiesForRendering()) {
            if (!(candidate instanceof ItemEntity item) || !item.isAlive()) {
                continue;
            }
            if (mc.player.distanceToSqr(item) > maxDistSq) {
                continue;
            }

            double x = Mth.lerp(partialTick, item.xOld, item.getX());
            double y = Mth.lerp(partialTick, item.yOld, item.getY());
            double z = Mth.lerp(partialTick, item.zOld, item.getZ());

            AABB box = item.getBoundingBox();
            double groundY = box.minY + (y - item.getY()) + 0.01D;
            double half = Math.max(box.getXsize(), box.getZsize()) * 0.5D + 0.06D;

            // 躺平的地面投影（贴地薄片）——对应 来源"lies on the ground"的观感。
            Render3DUtils.drawFilledBox(
                    new AABB(x - half, groundY, z - half, x + half, groundY + 0.02D, z + half),
                    fillArgb);

            // 绕 Y 轴旋转的方形轮廓——角度与未来钩子共用 groundSpin()。
            float angle = groundSpin(item, partialTick);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            for (int i = 0; i < SQUARE_CORNERS.length; i++) {
                double[] from = SQUARE_CORNERS[i];
                double[] to = SQUARE_CORNERS[(i + 1) % SQUARE_CORNERS.length];
                Render3DUtils.drawLine(stack,
                        new Vec3(x + rotateX(from, half, cos, sin), groundY, z + rotateZ(from, half, cos, sin)),
                        new Vec3(x + rotateX(to, half, cos, sin), groundY, z + rotateZ(to, half, cos, sin)),
                        lineArgb, LINE_THICKNESS);
            }
        }
    }

    /**
     * 掉落物躺倒后的绕 Y 轴角度（弧度）：原版 {@link ItemEntity#getSpin(float, float)}
     * 乘上 "Rotation speed"。0.0 时为 0（静止躺平）。
     * 供未来的 ItemEntityRenderer 钩子直接复用。
     */
    public float groundSpin(ItemEntity entity, float partialTick) {
        float vanilla = ItemEntity.getSpin(entity.getAge() + partialTick, entity.bobOffs);
        return (float) (vanilla * rotationSpeed.get());
    }

    private static double rotateX(double[] corner, double half, float cos, float sin) {
        return corner[0] * half * cos - corner[1] * half * sin;
    }

    private static double rotateZ(double[] corner, double half, float cos, float sin) {
        return corner[0] * half * sin + corner[1] * half * cos;
    }
}
