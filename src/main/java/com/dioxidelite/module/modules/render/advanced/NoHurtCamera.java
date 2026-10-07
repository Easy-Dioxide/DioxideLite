package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.mixin.LivingEntityAccessor;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * 禁用受伤时的镜头倾斜与玩家模型受伤红光。
 *
 * <p>镜头倾斜（{@code GameRenderer#bobHurt}）由 {@link com.dioxidelite.mixin.GameRendererMixin}
 * 在模块启用时直接取消，无需本模块额外处理。</p>
 *
 * <p>{@code hideModelDamage} 控制玩家模型本身是否显示受伤闪白；通过每 tick 将本地玩家的
 * {@code hurtTime} 清零实现——{@code hurtTime} 是渲染层读取的受伤动画计时器，清零即无闪白。</p>
 */
public final class NoHurtCamera extends Module {

    public static final NoHurtCamera INSTANCE = new NoHurtCamera();

    /**
     * 是否同时隐藏玩家模型受伤闪白。
     * 镜头晃动已由 GameRendererMixin 统一处理；此开关仅针对模型层面。
     */
    public final BooleanSetting hideModelDamage = add(new BooleanSetting("No Player Model Hurt", false));

    private NoHurtCamera() {
        super("No Hurt Camera", Category.RENDER);
    }

    /**
     * 每 tick 前置阶段运行：若模块启用且 hideModelDamage 打开，
     * 将本地玩家的 hurtTime 清零，防止受伤闪白出现在模型渲染中。
     */
    @Listen
    private void onPreTick(TickEvent.Pre event) {
        if (!isEnabled() || !hideModelDamage.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            ((LivingEntityAccessor) (LivingEntity) mc.player).dioxidelite$setHurtTime(0);
        } catch (Exception ignored) {
        }
    }
}
