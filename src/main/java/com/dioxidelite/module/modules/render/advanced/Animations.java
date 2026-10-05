package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.EnumSetting;

/**
 * 第一人称挥砍 / 格挡物品动画（移植自 来源客户端 {@code features/render/Animations}，
 * "Changes the swinging and blocking item animations"）。
 *
 * <p>来源 端同样只是一个设置容器：两个 {@code EnumSetting} 分别控制格挡姿态
 * （"Block"："Raises the sword higher and lets it sweep along with the swing"）
 * 与挥动驱动源（"Swing"："Drives the item bob off the equip progress instead of the
 * swing progress"），真正的姿态改写由 {@code ItemInHandRenderer} 侧的钩子读取
 * {@code Animations.h()} / {@code Animations.I()} 完成。</p>
 *
 * <p>本端口保留两个枚举设置与同语义的静态查询方法
 * （{@link #blockOverride()} / {@link #equipDrivenSwing()}），供接入钩子时直接调用。</p>
 *
 * <p>还原度说明：来源的 {@code FeatureMode_263} / {@code FeatureMode_318} 枚举常量名不在
 * 反编译产物里，只能从调用处推断：{@code h()} 判定 {@code H.is(FeatureMode_263.f)}、
 * {@code I()} 判定 {@code e.is(FeatureMode_318.H)}，而这两个常量的下标恰好是各自的默认值
 * （模块默认就应"改变动画"），因此这里定义 {@code VANILLA}/{@code DIOXIDE} 两个值并让
 * 默认落在 {@code DIOXIDE} 上。</p>
 */
// [v2.2.5 补全] 已在 ItemInHandRendererMixin 接入：
//   - blockOverride() -> 强制剑使用 BLOCK 格挡姿态，并在 applyItemArmTransform 后叠加"举高 + 随挥动扫过"变换；
//   - equipDrivenSwing() -> ModifyArg 注入 swingArm，把物品晃动驱动从挥动进度改为装备进度。
public final class Animations extends Module {

    public static final Animations INSTANCE = new Animations();

    /** 来源 {@code FeatureMode_263}：格挡姿态模式（常量名不可考，按默认分支语义还原）。 */
    public enum BlockMode {
        /** 原版格挡姿态。 */
        VANILLA,
        /** 来源 姿态：剑举得更高，并随挥舞扫过。 */
        DIOXIDE
    }

    /** 来源 {@code FeatureMode_318}：挥舞驱动模式（常量名不可考，按默认分支语义还原）。 */
    public enum SwingMode {
        /** 原版：摆动进度驱动。 */
        VANILLA,
        /** 来源：改由装备进度（equip progress）驱动物品晃动。 */
        DIOXIDE
    }

    /** 来源: EnumSetting("Block", FeatureMode_263.f)，默认即生效分支。 */
    public final EnumSetting<BlockMode> block = add(new EnumSetting<>("Block", BlockMode.DIOXIDE));

    /** 来源: EnumSetting("Swing", FeatureMode_318.H)，默认即生效分支。 */
    public final EnumSetting<SwingMode> swing = add(new EnumSetting<>("Swing", SwingMode.DIOXIDE));

    private Animations() {
        super("Animations", Category.RENDER);
    }

    /** 来源 {@code Animations.h()}：Block 模式是否命中 来源 分支（模块启用时才生效）。 */
    public static boolean blockOverride() {
        return INSTANCE.isEnabled() && INSTANCE.block.is(BlockMode.DIOXIDE);
    }

    /** 来源 {@code Animations.I()}：Swing 是否改为装备进度驱动（模块启用时才生效）。 */
    public static boolean equipDrivenSwing() {
        return INSTANCE.isEnabled() && INSTANCE.swing.is(SwingMode.DIOXIDE);
    }
}
