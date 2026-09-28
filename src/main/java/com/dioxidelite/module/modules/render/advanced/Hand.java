package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.ColorSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.Locale;

/**
 * 第一人称手臂定制（移植自 来源客户端 {@code features/render/Hand}：
 * "Adjust the first-person hand and replace the sword swing"）。
 *
 * <p>来源原版把三组设置（{@code FeatureSupport_274 "Main hand"} = 主手位移/旋转/缩放、
 * {@code FeatureSupport_322} = 替换挥动曲线与幅度、{@code FeatureSupport_266} = 剑的颜色/呈现）
 * 直接喂给 {@code RenderSupport_102}（把手臂画进离屏 framebuffer 再摆回屏幕）与
 * {@code RenderSupport_097}（custom_hand_mask shader 的手臂特效管线），本端口的做法是：
 * 设置面 1:1 保留，把与 hook 无关的数学部分（挥动进度、缓动曲线、材质配色表）原样实现，
 * 并暴露 {@link #applyMainHandTransform(PoseStack)} / {@link #swingOffset()} /
 * {@link #swordColor(ItemStack)} 给以后的 mixin 直接调用。</p>
 *
 * <p>默认值说明：反编译产物把设置组的字段初值丢了，这里的默认值取自 Hand 类里保留下来的静态常量
 * （{@code 0.56 / -0.52 / -0.72} 位移、{@code 45.0} 旋转、{@code 0.4 / 0.5} 缩放与挥动相关），
 * 数值区间与来源 一致。</p>
 */
// PORT-NOTE: 需要 ItemInHandRenderer（renderArmWithItem / renderHandsWithItems）的第一人称手臂 hook，
// 以及 来源的离屏手臂特效管线（RenderSupport_097/102/139/150 + custom_hand_mask shader）；
// 本端口只实现了 1:1 设置面、挥动进度与缓动曲线、材质配色表，以及供 hook 调用的变换/取色接口。
public final class Hand extends Module {

    public static final Hand INSTANCE = new Hand();

    /** 来源 {@code FeatureMode_315}：替换挥动的缓动曲线（调用处 ordinal 0..4，常量为 0 / 3t² / t / sin / √t）。 */
    public enum SwingMode {
        OFF,
        ACCELERATE,
        LINEAR,
        SINE,
        SQRT
    }

    /** 来源 {@code FeatureMode_324}：剑的呈现管线 —— 颜色着色（{@code FeatureSupport_273}）或自定义模型（{@code FeatureSupport_311}）。 */
    public enum SwordStyle {
        COLOR,
        MODEL
    }

    /** 来源 常量 {@code i = 260000000L}：挥动进度循环周期 260ms。 */
    private static final long SWING_PERIOD_NANOS = 260_000_000L;

    // --- 来源 FeatureSupport_274 "Main hand"：主手位移 / 旋转 / 缩放 -------------------

    /** 主手位移 X（格），来源 常量 {@code L = 0.56}。 */
    public final DoubleSetting mainX = add(new DoubleSetting("Main hand X", 0.56, -2.0, 2.0, 0.01));

    /** 主手位移 Y（格），来源 常量 {@code c = -0.52}。 */
    public final DoubleSetting mainY = add(new DoubleSetting("Main hand Y", -0.52, -2.0, 2.0, 0.01));

    /** 主手位移 Z（格），来源 常量 {@code M = -0.72}。 */
    public final DoubleSetting mainZ = add(new DoubleSetting("Main hand Z", -0.72, -2.0, 2.0, 0.01));

    /** 主手 Yaw（度），来源 常量 {@code D = 45.0}。 */
    public final DoubleSetting mainYaw = add(new DoubleSetting("Main hand yaw", 45.0, -360.0, 360.0, 1.0));

    /** 主手 Pitch（度）。 */
    public final DoubleSetting mainPitch = add(new DoubleSetting("Main hand pitch", 0.0, -360.0, 360.0, 1.0));

    /** 主手 Roll（度）。 */
    public final DoubleSetting mainRoll = add(new DoubleSetting("Main hand roll", 0.0, -360.0, 360.0, 1.0));

    /** 主手缩放 X（%），来源 常量 {@code k = 0.4} 对应的缩放轴，100 = 原版。 */
    public final DoubleSetting mainScaleX = add(new DoubleSetting("Main hand scale X", 100.0, 10.0, 300.0, 5.0));

    /** 主手缩放 Y（%），来源 常量 {@code g = 0.5f} 对应的缩放轴，100 = 原版。 */
    public final DoubleSetting mainScaleY = add(new DoubleSetting("Main hand scale Y", 100.0, 10.0, 300.0, 5.0));

    /** 主手缩放 Z（%），100 = 原版。 */
    public final DoubleSetting mainScaleZ = add(new DoubleSetting("Main hand scale Z", 100.0, 10.0, 300.0, 5.0));

    // --- 来源 FeatureSupport_322 / FeatureMode_315：替换挥动 ---------------------------

    /** 替换挥动的缓动曲线（来源 {@code a.g}，默认取 SINE，即 来源 里的 sin 分支）。 */
    public final EnumSetting<SwingMode> swing = add(new EnumSetting<>("Swing", SwingMode.SINE));

    /** 挥动幅度（%，来源 {@code a.h}）：来源 把它除以 100 当作正弦振幅。 */
    public final DoubleSetting swingAmount = add(new DoubleSetting("Swing amount", 100.0, 0.0, 400.0, 5.0));

    // --- 来源 FeatureSupport_266 / FeatureSupport_273 / FeatureMode_324：剑 --------------

    /** 是否接管剑的呈现（来源 {@code FeatureSupport_266.D()}）。 */
    public final BooleanSetting sword = add(new BooleanSetting("Sword", true));

    /** 剑的呈现管线（来源 {@code FeatureMode_324}）；形状/着色分别走 DIOXIDE 的两条渲染管线。 */
    public final EnumSetting<SwordStyle> swordStyle = add(new EnumSetting<>("Sword style", SwordStyle.COLOR));

    /** 颜色跟随手持物品材质（来源 {@code FeatureSupport_273.e}，此时用 {@link #materialColor(String)} 的调色板）。 */
    public final BooleanSetting itemColors = add(new BooleanSetting("Item colors", true));

    /** 自定义剑色（来源 {@code FeatureSupport_273.h}）；默认 {@code 0xFFE6E6E6} = 来源 调色板的兜底色。 */
    public final ColorSetting color = add(new ColorSetting("Color", new Color(0xFFE6E6E6, true))
            .visibleWhen(() -> !itemColors.get() && swordStyle.is(SwordStyle.COLOR)));

    /** 来源 {@code a.d}：按下攻击键的时刻（{@code -1} = 未按下），用于计算 260ms 循环的挥动进度。 */
    private long swingStartNanos = -1L;

    private Hand() {
        super("Hand", Category.RENDER);
    }

    // --- 与 hook 无关的数学部分（来源 D() / L(FeatureMode_315, float) / d()） -------------

    /**
     * 来源 {@code D()}：按住攻击键时的手部挥动进度 0..1（自按下起按 260ms 循环取模），
     * 没按攻击键时归零并复位计时；第一人称 hook 可以直接用它驱动替换挥动。
     */
    public float swingProgress() {
        if (mc.options == null || !mc.options.keyAttack.isDown()) {
            swingStartNanos = -1L;
            return 0.0F;
        }
        if (swingStartNanos < 0L) {
            swingStartNanos = System.nanoTime();
        }
        return (float) ((System.nanoTime() - swingStartNanos) % SWING_PERIOD_NANOS) / (float) SWING_PERIOD_NANOS;
    }

    /** 来源 {@code FeatureMode_315} 的缓动表：OFF=0、ACCELERATE=3t²、LINEAR=t、SINE=sin(πt)、SQRT=√t。 */
    public float swingCurve(float progress) {
        float t = Math.clamp(progress, 0.0F, 1.0F);
        return switch (swing.get()) {
            case OFF -> 0.0F;
            case ACCELERATE -> t * t * 3.0F;
            case LINEAR -> t;
            case SINE -> (float) Math.sin(t * Math.PI);
            case SQRT -> (float) Math.sqrt(t);
        };
    }

    /** 来源 {@code d(float)} 里的最终挥动量：缓动曲线 × 幅度/100。 */
    public float swingOffset() {
        return swingCurve(swingProgress()) * (swingAmount.get().floatValue() / 100.0F);
    }

    /** 手持物是否为剑（来源 用 {@code ItemSword} 判定，MC 26.1.2 用 {@code ItemTags.SWORDS}）。 */
    public boolean holdingSword() {
        if (noPlayer()) {
            return false;
        }
        ItemStack held = mc.player.getMainHandItem();
        return !held.isEmpty() && held.is(ItemTags.SWORDS);
    }

    /** 来源 {@code l()}：是否应该接管剑的呈现（启用 + 有玩家 + Sword 开关 + 手持剑）。 */
    public boolean shouldOverrideSword() {
        return isEnabled() && sword.get() && holdingSword();
    }

    /**
     * 来源 {@code L()}/{@code d()}：剑的着色值 —— Item colors 打开时按手持物材质取色，
     * 否则用 {@link #color} 设置值。
     */
    public int swordColor(ItemStack stack) {
        if (!itemColors.get()) {
            return color.argb();
        }
        if (stack == null || stack.isEmpty()) {
            return DEFAULT_SWORD_COLOR;
        }
        return materialColor(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT));
    }

    /** 来源 兜底剑色 {@code -1644826} = {@code 0xFFE6E6E6}。 */
    public static final int DEFAULT_SWORD_COLOR = 0xFFE6E6E6;

    /**
     * 来源 {@code d()} 的材质调色板 1:1 映射（按物品名子串匹配，命中顺序与来源 相同：
     * diamond / gold / iron / emerald / redstone / lapis|dyepowder / 兜底）。
     * 1.8 的 {@code dyepowder} 在 26.1.2 对应 {@code *_dye}。
     */
    public static int materialColor(String itemName) {
        if (itemName.contains("diamond")) {
            return 0xFF55DDE0;
        }
        if (itemName.contains("gold")) {
            return 0xFFFFD45A;
        }
        if (itemName.contains("iron")) {
            return 0xFFD8DEE8;
        }
        if (itemName.contains("emerald")) {
            return 0xFF35D06F;
        }
        if (itemName.contains("redstone")) {
            return 0xFFE23B3B;
        }
        if (itemName.contains("lapis") || itemName.contains("dye")) {
            return 0xFF3156D4;
        }
        return DEFAULT_SWORD_COLOR;
    }

    // --- 供第一人称 hook 调用的变换 ---------------------------------------------------

    /**
     * 应用 "Main hand" 组的位置 -> 旋转(X/Y/Z) -> 缩放变换（来源 在
     * {@code RenderSupport_102.D()} 之后、画手臂之前做同样的三步）。
     */
    public void applyMainHandTransform(PoseStack stack) {
        stack.translate(mainX.get().floatValue(), mainY.get().floatValue(), mainZ.get().floatValue());
        stack.mulPose(Axis.YP.rotationDegrees(mainYaw.get().floatValue()));
        stack.mulPose(Axis.XP.rotationDegrees(mainPitch.get().floatValue()));
        stack.mulPose(Axis.ZP.rotationDegrees(mainRoll.get().floatValue()));
        stack.scale(mainScaleX.get().floatValue() / 100.0F,
                mainScaleY.get().floatValue() / 100.0F,
                mainScaleZ.get().floatValue() / 100.0F);
    }
}
