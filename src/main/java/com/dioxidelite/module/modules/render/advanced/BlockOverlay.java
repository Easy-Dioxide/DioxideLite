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
import com.dioxidelite.util.render.Render3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * 方块覆盖层（移植自 来源客户端 BlockOverlay）：给准星所指方块套一层带描边/填充/流光的框，
 * 并随挖掘进度填充。全部在世界空间 3D 层（Render3DEvent）绘制。
 */
public final class BlockOverlay extends Module {

    public static final BlockOverlay INSTANCE = new BlockOverlay();

    /** 来源客户端 FeatureMode_278 的三个主题：描边 / 半透明填充 / 流光渐变。 */
    public enum Theme {
        OUTLINE,
        FILL,
        GLINT
    }

    /** 每帧向目标值靠拢的插值系数（对应 来源客户端 的 4.0f/70.0f 动画速度）。 */
    private static final double SMOOTHING = 0.35;

    public final EnumSetting<Theme> theme = add(new EnumSetting<>("Theme", Theme.FILL));
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0xFF64AFFF, true)));
    public final ColorSetting secondColor = add(new ColorSetting("Second Color", new Color(0xFF9456FF, true))
            .visibleWhen(() -> theme.is(Theme.GLINT)));
    public final DoubleSetting lineWidth = add(new DoubleSetting("Line Width", 1.5, 0.5, 5.0, 0.5));
    public final DoubleSetting outlineOpacity = add(new DoubleSetting("Outline Opacity", 90.0, 5.0, 100.0, 5.0));
    public final DoubleSetting fillOpacity = add(new DoubleSetting("Fill Opacity", 22.0, 1.0, 80.0, 1.0)
            .visibleWhen(() -> !theme.is(Theme.OUTLINE)));
    public final DoubleSetting padding = add(new DoubleSetting("Padding", 0.6, 0.0, 5.0, 0.1));
    public final BooleanSetting progress = add(new BooleanSetting("Progress", true));
    public final BooleanSetting onlyOnBedBreak = add(new BooleanSetting("Only On Bed Break", false));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", true));
    public final BooleanSetting hideVanillaOutline = add(new BooleanSetting("Hide Vanilla Outline", true));

    private boolean visible;
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;
    private double targetMinX;
    private double targetMinY;
    private double targetMinZ;
    private double targetMaxX;
    private double targetMaxY;
    private double targetMaxZ;
    private float open;
    private float progressValue;

    private BlockOverlay() {
        super("BlockOverlay", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        reset();
    }

    @Listen
    private void onRender3D(Render3DEvent event) {
        if (noPlayer()) {
            reset();
            return;
        }

        BlockPos pos = resolveTarget();
        if (pos == null && !visible) {
            return;
        }

        if (pos != null) {
            double blockX = pos.getX();
            double blockY = pos.getY();
            double blockZ = pos.getZ();
            if (!visible) {
                // 第一次出现时直接吸附，避免从上一个方块的位置飞过来。
                minX = blockX;
                minY = blockY;
                minZ = blockZ;
                maxX = blockX + 1.0;
                maxY = blockY + 1.0;
                maxZ = blockZ + 1.0;
                visible = true;
            }
            targetMinX = blockX;
            targetMinY = blockY;
            targetMinZ = blockZ;
            targetMaxX = blockX + 1.0;
            targetMaxY = blockY + 1.0;
            targetMaxZ = blockZ + 1.0;
        }

        open = approach(open, pos != null ? 1.0F : 0.0F);
        if (pos == null && open <= 0.01F) {
            reset();
            return;
        }
        if (!visible) {
            return;
        }

        minX = approach(minX, targetMinX);
        minY = approach(minY, targetMinY);
        minZ = approach(minZ, targetMinZ);
        maxX = approach(maxX, targetMaxX);
        maxY = approach(maxY, targetMaxY);
        maxZ = approach(maxZ, targetMaxZ);
        if (open <= 0.01F) {
            return;
        }

        double inflate = padding.get() / 100.0;
        AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(inflate);

        progressValue = approach(progressValue, breakProgress());
        drawOverlay(event.getPoseStack(), box, open, progressValue);
    }

    private void drawOverlay(PoseStack stack, AABB box, float alphaScale, float progressRatio) {
        Color base = color.get();
        int outlineColor = argb(base, outlineOpacity.get() * alphaScale);
        int fillColor = argb(base, fillOpacity.get() * alphaScale);

        switch (theme.get()) {
            case OUTLINE -> Render3DUtils.drawOutlineBox(stack, box, outlineColor, lineWidth.get().floatValue());
            case FILL -> {
                Render3DUtils.drawFilledBox(box, fillColor);
                Render3DUtils.drawOutlineBox(stack, box, outlineColor, lineWidth.get().floatValue());
            }
            case GLINT -> {
                float phase = (System.currentTimeMillis() % 2000L) / 2000.0F;
                float wave = 1.0F - Math.abs(phase * 2.0F - 1.0F);
                Color first = ColorUtils.interpolate(base, secondColor.get(), wave);
                Color second = ColorUtils.interpolate(secondColor.get(), base, wave);
                Render3DUtils.drawFilledFadeBox(box,
                        argb(first, fillOpacity.get() * alphaScale),
                        argb(second, fillOpacity.get() * alphaScale));
                Render3DUtils.drawOutlineBox(stack, box, outlineColor, lineWidth.get().floatValue());
            }
        }

        if (progressRatio > 0.001F) {
            double height = (box.maxY - box.minY) * progressRatio;
            AABB progressBox = new AABB(box.minX, box.minY, box.minZ, box.maxX, box.minY + height, box.maxZ);
            Render3DUtils.drawFilledBox(progressBox, argb(base, outlineOpacity.get() * alphaScale));
        }
    }

    private BlockPos resolveTarget() {
        if (onlyOnBedBreak.get()) {
            // PORT-NOTE: 需要 BedBreaker 的床目标；本端口只实现了"仅当正在挖掘方块时显示"的退化条件。
            if (mc.gameMode == null || !mc.gameMode.isDestroying()) {
                return null;
            }
        }
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK
                || !(mc.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (mc.level.getBlockState(pos).isAir()) {
            return null;
        }
        if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 25.0) {
            return null;
        }
        return pos.immutable();
    }

    /** 当前挖掘进度 0..1（原版 destroyStage 0..9）。 */
    private float breakProgress() {
        if (!progress.get() || mc.gameMode == null || !mc.gameMode.isDestroying()) {
            return 0.0F;
        }
        int stage = mc.gameMode.getDestroyStage();
        return stage < 0 ? 0.0F : Mth.clamp((stage + 1) / 10.0F, 0.0F, 1.0F);
    }

    private static double approach(double current, double target) {
        return current + (target - current) * SMOOTHING;
    }

    private static float approach(float current, float target) {
        return current + (target - current) * (float) SMOOTHING;
    }

    private static int argb(Color color, double percent) {
        int alpha = (int) Math.round(color.getAlpha() * Mth.clamp(percent / 100.0, 0.0, 1.0));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Mth.clamp(alpha, 0, 255)).getRGB();
    }

    private void reset() {
        visible = false;
        open = 0.0F;
        progressValue = 0.0F;
    }

    // PORT-NOTE: 需要 LevelRenderer 的方块描边 hook 才能关闭原版黑框（Hide Vanilla Outline）；本端口只实现了自绘覆盖层。
    // PORT-NOTE: Render3DUtils 恒为透视绘制，Through Walls 开关当前无法切换深度测试。
}
