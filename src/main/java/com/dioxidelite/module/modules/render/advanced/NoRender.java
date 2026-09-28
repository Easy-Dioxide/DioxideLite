package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.util.client.ViewBobbingSuppressor;

/**
 * 移植自 来源客户端 <code>features/render/NoRender</code>
 * （"Hides selected vanilla visual effects"）。
 *
 * <p>来源的六项开关与原版默认值：
 * Fire=true、Hurt camera=true、Hand bob=false、Boss bar=false、Bad effects=true、Scoreboard=false。
 * 原版还提供 <code>L(BooleanSetting)</code> —— "模块开启且该项打开" 的统一判定，
 * 由各渲染点的 hook 调用；本端口对应 {@link #isHidden(BooleanSetting)}。</p>
 *
 * <p>Dioxide 侧的可做部分：Hand bob 走项目已有的
 * {@link ViewBobbingSuppressor}（GameRendererMixin#extractOptions 会读它来关掉
 * <code>optionsRenderState.bobView</code>），不需要新 hook；其余五项要动到原版渲染点，
 * 只能由 hook 读取本模块的开关。</p>
 */
// PORT-NOTE: 需要 ScreenEffectRenderer#renderFire、GameRenderer#bobHurt、Gui 的 boss bar / scoreboard / 效果 HUD 提取点等 hook；本端口只实现了设置面、isHidden(BooleanSetting) 判定与 Hand bob（ViewBobbingSuppressor）。
public final class NoRender extends Module {

    public static final NoRender INSTANCE = new NoRender();

    /** ViewBobbingSuppressor 的持有者标识，只在本模块启用期间占用。 */
    private static final String BOB_SUPPRESSOR_OWNER = "custom_norender";

    /** 来源: Fire，默认开。 */
    public final BooleanSetting fire = add(new BooleanSetting("Fire", true));

    /** 来源: Hurt camera，默认开。 */
    public final BooleanSetting hurtCamera = add(new BooleanSetting("Hurt camera", true));

    /** 来源: Hand bob，默认关。 */
    public final BooleanSetting handBob = add(new BooleanSetting("Hand bob", false)
            .onChange(ignored -> syncHandBob()));

    /** 来源: Boss bar，默认关。 */
    public final BooleanSetting bossBar = add(new BooleanSetting("Boss bar", false));

    /** 来源: Bad effects，默认开（失明/黑暗一类负面效果的视效与雾效）。 */
    public final BooleanSetting badEffects = add(new BooleanSetting("Bad effects", true));

    /** 来源: Scoreboard，默认关。 */
    public final BooleanSetting scoreboard = add(new BooleanSetting("Scoreboard", false));

    private NoRender() {
        super("NoRender", Category.RENDER);
    }

    /**
     * 对应 来源客户端 <code>NoRender#L(BooleanSetting)</code>：
     * 只有模块本身启用、且传入的开关为打开时才隐藏对应效果。
     */
    public boolean isHidden(BooleanSetting setting) {
        return isEnabled() && setting.get();
    }

    @Override
    protected void onEnable() {
        syncHandBob();
    }

    @Override
    protected void onDisable() {
        ViewBobbingSuppressor.release(BOB_SUPPRESSOR_OWNER);
    }

    private void syncHandBob() {
        if (isEnabled() && handBob.get()) {
            ViewBobbingSuppressor.acquire(BOB_SUPPRESSOR_OWNER);
        } else {
            ViewBobbingSuppressor.release(BOB_SUPPRESSOR_OWNER);
        }
    }
}
