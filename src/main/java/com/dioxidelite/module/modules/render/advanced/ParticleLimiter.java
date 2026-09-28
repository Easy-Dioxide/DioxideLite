package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.DoubleSetting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 稀疏化原版粒子（移植自 来源客户端 {@code features/render/ParticleLimiter}，
 * 原描述 "Thins vanilla particles in the Hypixel Bed Wars lobby"，原设置只有 Keep 一项）。
 *
 * <p>来源 里这是一个纯判定模块：{@code I()} 在「模块启用 + 当前处于 Hypixel Bed Wars 大厅」
 * 时按 Keep 百分比随机返回 true，粒子生成点的 hook 拿到 true 就丢弃这次生成，
 * 也就是平均保留 Keep% 的粒子。</p>
 *
 * <p>本端口保留 {@link #keep}（默认 5%，范围 5~100%，步进 5%）与
 * {@link #shouldLimit()}，并复刻 来源 {@code HypixelUtils.L()} 的门控：
 * 服务器地址含 {@code hypixel.net} 且侧边栏计分板标题含 "BED WARS"。
 * 真正把粒子拦下来还需要在粒子生成点调用 {@link #shouldLimit()}。</p>
 */
// PORT-NOTE: 需要粒子生成 hook（ParticleEngine#createParticle / addParticle / createTrackingEmitter 等生成点，可由项目已有的 ParticleEngineMixin 在其取消判定里追加本模块的 shouldLimit()）；本端口只实现了设置面、Hypixel Bed Wars 大厅门控与 shouldLimit() 随机拦截判定。
public final class ParticleLimiter extends Module {

    public static final ParticleLimiter INSTANCE = new ParticleLimiter();

    /** 来源 门控用的服务器地址关键字（HypixelUtils.d()）。 */
    private static final String HYPIXEL_HOST = "hypixel.net";

    /** 来源 门控用的侧边栏标题关键字（HypixelUtils.L() 里的 "BED WARS"）。 */
    private static final String BED_WARS_SIDEBAR = "BED WARS";

    /** 来源: Keep，默认 5%，范围 5~100%，步进 5%（单位 %）。 */
    public final DoubleSetting keep = add(new DoubleSetting("Keep", 5.0, 5.0, 100.0, 5.0));

    private ParticleLimiter() {
        super("Particle Limiter", Category.RENDER);
    }

    /**
     * 对应 来源 {@code I()}：返回 true 表示这次粒子生成应当被拦掉
     * （概率为 100 - Keep%，即平均保留 Keep% 的粒子）。
     */
    public boolean shouldLimit() {
        if (!isEnabled() || !isBedWarsLobby()) {
            return false;
        }
        return ThreadLocalRandom.current().nextInt(100) >= keep.get();
    }

    /**
     * 对应 来源 {@code HypixelUtils.L()}：当前服务器是 hypixel.net，
     * 且侧边栏计分板标题含 "BED WARS"（Bed Wars 大厅）。
     */
    private boolean isBedWarsLobby() {
        ServerData server = mc.getCurrentServer();
        if (server == null || server.ip == null
                || !server.ip.toLowerCase(Locale.ROOT).contains(HYPIXEL_HOST)) {
            return false;
        }
        if (mc.level == null) {
            return false;
        }
        Objective sidebar = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return sidebar != null
                && sidebar.getDisplayName().getString().toUpperCase(Locale.ROOT).contains(BED_WARS_SIDEBAR);
    }
}
