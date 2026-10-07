package com.dioxidelite.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露原版右键节流字段 {@code Minecraft.rightClickDelay}。
 *
 * <p><b>为什么需要这个 accessor：</b>原版右键加速的真正瓶颈是
 * {@code Minecraft.rightClickDelay} —— 每次 {@code startUseItem()} 都会把它写成 4，
 * 然后每 tick 递减 1，`handleKeybinds()` 只有在它等于 0 时才会执行右键动作。
 * 这就是原版 4 tick（200 ms）放置间隔的唯一来源。
 *
 * <p>把它清零即可让原版自身的放置流程（挥动、客户端预测、发包、序列号）保持完整，
 * 同时把间隔降到 1 tick。相比直接调用 {@code MultiPlayerGameMode.useItemOn}，
 * 这种做法的副作用最小，也不会绕过原版的服务端校验链。
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    /** 设置原版右键节流的剩余 tick 数（0 = 本 tick 允许右键动作）。 */
    @Accessor("rightClickDelay")
    void dioxidelite$setRightClickDelay(int ticks);

    /** 读取原版右键节流的剩余 tick 数。 */
    @Accessor("rightClickDelay")
    int dioxidelite$getRightClickDelay();
}
