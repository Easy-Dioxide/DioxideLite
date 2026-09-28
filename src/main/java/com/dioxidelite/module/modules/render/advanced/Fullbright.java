package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.DoubleSetting;

/**
 * 移植自 来源客户端 <code>features/render/Fullbright</code>
 * （"Makes the world brighter"）。
 *
 * <p>来源的行为：开启时先记下 <code>gameSettings.gammaSetting</code>，把 gamma 写到
 * 设定值（"Gamma"，1..100，默认 100，原版上限为 1），之后每个 tick 持续维持；
 * 关闭时把之前记下的值写回去。</p>
 *
 * <p>移植说明：MC 26.1.2 的 <code>Options.gamma()</code> 是 <code>OptionInstance&lt;Double&gt;</code>，
 * 其 ValueSet 为 <code>UnitDouble</code>，只接受 [0,1]，越界的 <code>set()</code> 会被静默忽略
 * （不像 来源 那样能直接把 raw 字段写到 100）。所以本端口把 来源的 1..100 落在原版合法范围内：
 * 在任何 来源 合法取值下都取原版最大亮度 1.0 —— 即 Vanilla 的 "Bright"。</p>
 */
// PORT-NOTE: 需要 Lightmap hook（读取/覆盖 LightmapRenderState.brightness，或像 LightmapMixin 那样直接清空 lightmap 纹理）；本端口只实现了原版 gamma 选项可写入范围内的最大亮度（mc.options.gamma() = 1.0，即 Vanilla "Bright"）。
public final class Fullbright extends Module {

    public static final Fullbright INSTANCE = new Fullbright();

    /** 来源: Gamma 100.0 (1.0..100.0, 1.0)；来源的提示语为 "Gamma to apply (vanilla max brightness is 1)"。 */
    public final DoubleSetting gamma = add(new DoubleSetting("Gamma", 100.0, 1.0, 100.0, 1.0));

    /** 开启前的原版 gamma，关闭时原样恢复（对应 来源的成员字段 e）。 */
    private double previousGamma = 0.5;

    private boolean gammaSaved;

    private Fullbright() {
        super("Fullbright", Category.RENDER);
    }

    /**
     * Dioxide 已经存在同名模块 <code>Fullbright</code>（模块 id {@code fullbright}），
     * ModuleManager#register 会对重复 id 抛异常；这里沿用 来源的显示名，只把持久化 id 独立出来。
     */
    @Override
    public String id() {
        return "custom_fullbright";
    }

    @Override
    protected void onEnable() {
        if (mc.options == null) {
            return;
        }
        previousGamma = mc.options.gamma().get();
        gammaSaved = true;
        applyGamma();
    }

    @Override
    protected void onDisable() {
        if (!gammaSaved || mc.options == null) {
            return;
        }
        mc.options.gamma().set(previousGamma);
        gammaSaved = false;
    }

    /** 来源 在 tick 事件里持续把 gamma 拉回设定值（防止被选项界面改掉）。 */
    @Listen
    private void onTick(TickEvent.Pre event) {
        applyGamma();
    }

    private void applyGamma() {
        if (mc.options == null) {
            return;
        }
        // gamma 的 UnitDouble 只允许 [0,1]：来源的取值区间 (>=1) 全部对应"原版最大亮度"，
        // 因此这里饱和到 1.0，与来源 在合法取值下的观感一致（超出原版上限的部分见 PORT-NOTE）。
        mc.options.gamma().set(Math.min(1.0D, gamma.get()));
    }
}
