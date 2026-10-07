package com.dioxidelite.module.modules.player;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.mixin.MinecraftAccessor;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

/**
 * 快速放置（Fast Place）。
 *
 * <p><b>原理（必读）：</b>原版"右键只能每 200 ms 放一次"并不是物品冷却，
 * 而是 {@code Minecraft.rightClickDelay} 这道节流闸：
 * {@code startUseItem()} 把它置为 4，之后每 tick 减 1，
 * {@code handleKeybinds()} 仅在它等于 0 时才允许执行右键动作。
 *
 * <p>因此正确的做法是在每个客户端 tick 的最前面（{@link TickEvent.Pre}，即
 * {@code Minecraft.tick()} 的 HEAD）按需要把该字段清零，让原版自己的放置流程
 * 继续跑（挥动 / 预测 / 发包 / 序列号全部保留），只是不再被节流。
 *
 * <p>注意：{@code ItemCooldowns}（末影珍珠、紫颂果那类）与方块放置无关，
 * 拦截它并不能加速放置——这正是之前修不好的原因之一。
 *
 * <p>设置项：
 * <ul>
 *   <li>{@code Place Delay} — 方块放置最小间隔（ms），0 = 每 tick 一次（最快，约 20/s）</li>
 *   <li>{@code Interact Delay} — 实体交互最小间隔（ms），-1 = 复用 Place Delay</li>
 *   <li>{@code Place} — 是否加速方块放置</li>
 *   <li>{@code Interact} — 是否加速实体交互</li>
 *   <li>{@code Jump} — 右键按住时自动跳跃</li>
 * </ul>
 */
public final class FastPlace extends Module {

    public static final FastPlace INSTANCE = new FastPlace();

    /** 原版节流的单位：1 tick = 50 ms。 */
    private static final long TICK_MILLIS = 50L;

    /** 放置冷却（ms），0 = 无冷却限制（最快）。 */
    public final IntSetting placeDelay = add(new IntSetting("Place Delay", 0, 0, 500, 10));

    /** 实体交互冷却（ms），0 = 无冷却限制；-1 = 使用与放置相同的冷却。 */
    public final IntSetting interactDelay = add(new IntSetting("Interact Delay", -1, -1, 500, 10));

    /** 是否允许方块放置。 */
    public final BooleanSetting place = add(new BooleanSetting("Place", true));

    /** 是否允许实体交互。 */
    public final BooleanSetting interact = add(new BooleanSetting("Interact", true));

    /** 是否在右键按住时自动跳跃。 */
    public final BooleanSetting jump = add(new BooleanSetting("Jump", false));

    /** 上次放行的纳秒时间戳。 */
    private long lastPlaceNanos = 0L;

    private FastPlace() {
        super("Fast Place", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        // 关掉模块后把节流恢复成"已就绪"，让原版自己在下次使用时写回 4，
        // 避免残留的 0 让玩家在关闭模块后仍被加速一瞬。
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        ((MinecraftAccessor) mc).dioxidelite$setRightClickDelay(0);
    }

    // -------------------------------------------------------------------------
    // 冷却检测
    // -------------------------------------------------------------------------

    /** 返回距离上次放行经过的时间（毫秒）。 */
    public long elapsedMillis() {
        return (System.nanoTime() - lastPlaceNanos) / 1_000_000L;
    }

    /** 当前放置冷却是否已就绪。 */
    public boolean canPlace() {
        int delay = placeDelay.get();
        return delay <= 0 || elapsedMillis() >= delay;
    }

    /** 当前实体交互冷却是否已就绪（-1 时复用 placeDelay）。 */
    public boolean canInteract() {
        int delay = interactDelay.get();
        if (delay < 0) {
            delay = placeDelay.get();
        }
        return delay <= 0 || elapsedMillis() >= delay;
    }

    /** 记录一次放行，更新时间戳。 */
    public void recordPlace() {
        lastPlaceNanos = System.nanoTime();
    }

    // -------------------------------------------------------------------------
    // 设置查询
    // -------------------------------------------------------------------------

    public boolean isPlaceEnabled() {
        return place.get();
    }

    public boolean isInteractEnabled() {
        return interact.get();
    }

    // -------------------------------------------------------------------------
    // 事件监听
    // -------------------------------------------------------------------------

    /**
     * 每个客户端 tick 的最前面（{@code Minecraft.tick()} HEAD）驱动右键节流。
     *
     * <p>只在按住右键时介入，避免影响其它物品的正常使用节奏。
     */
    @Listen
    private void onPreTick(TickEvent.Pre event) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!mc.options.keyUse.isDown()) {
            return;
        }

        HitResult hit = mc.hitResult;
        boolean onEntity = hit != null && hit.getType() == HitResult.Type.ENTITY;

        if (onEntity) {
            if (!isInteractEnabled()) {
                return;
            }
            int delay = interactDelay.get();
            if (delay < 0) {
                delay = placeDelay.get();
            }
            applyThrottle(mc, delay);
        } else {
            if (!isPlaceEnabled()) {
                return;
            }
            applyThrottle(mc, placeDelay.get());
        }
    }

    /**
     * 按毫秒间隔决定本 tick 是否清零原版右键节流。
     *
     * @param delayMillis 允许下一次动作所需的最小间隔；&le; 0 表示每 tick 都放行
     */
    private void applyThrottle(Minecraft mc, int delayMillis) {
        MinecraftAccessor throttle = (MinecraftAccessor) mc;

        if (delayMillis <= 0) {
            throttle.dioxidelite$setRightClickDelay(0);
            recordPlace();
            return;
        }

        long elapsed = elapsedMillis();
        if (elapsed >= delayMillis) {
            throttle.dioxidelite$setRightClickDelay(0);
            recordPlace();
            return;
        }

        // 冷却未就绪：把剩余毫秒换算成 tick 数写回去，让原版自己压住这次右键。
        long remaining = delayMillis - elapsed;
        int ticks = (int) Math.max(1L, (remaining + TICK_MILLIS - 1L) / TICK_MILLIS);
        throttle.dioxidelite$setRightClickDelay(ticks);
    }

    /** 右键按住时自动跳跃。 */
    @Listen
    private void onPreTickJump(TickEvent.Pre event) {
        if (!isEnabled() || !jump.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!mc.options.keyUse.isDown()) {
            return;
        }
        if (!isPlaceEnabled() || !canPlace()) {
            return;
        }
        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
        }
    }
}
