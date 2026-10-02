package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.ModuleManager;
import com.dioxidelite.module.modules.render.HudEditorModule;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers the additional render modules shipped in this package and retires the built-in modules
 * they supersede.
 *
 * <p>Registration goes through {@link ModuleManager#registerDynamic(Module)}, so the client's own
 * module table is not touched: the modules simply join the {@code RENDER} category and show up in
 * both ClickGUI styles, because both screens read the module list from the manager.</p>
 */
public final class RenderModuleRegistry {

    /** Built-in modules that are replaced by the implementations in this package. */
    private static final String[] SUPERSEDED = {"esp", "chams", "no_render", "fullbright"};

    private static boolean registered;

    private RenderModuleRegistry() {
    }

    /** Called once from the client bootstrap, right after {@code ModuleManager.init()}. */
    public static synchronized void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        // 先下线客户端自带的全部 RENDER 模块（HUD 编辑器工具除外），再注册本包的实现，
        // 这样它们的名字（ESP / Chams / NoRender / ...）可以原样使用，界面上只剩这一套。
        retireBuiltInRenderModules();
        Module[] modules = {
                Ambience.INSTANCE, Animations.INSTANCE, Arrows.INSTANCE, AttackEffects.INSTANCE,
                BedESP.INSTANCE, BlockOverlay.INSTANCE, Boxes.INSTANCE, BreakProgress.INSTANCE,
                Camera.INSTANCE, CapeChanger.INSTANCE,
                Chams.INSTANCE, ContainerESP.INSTANCE, Crosshair.INSTANCE,
                ESP.INSTANCE, FogBlur.INSTANCE, FogRemove.INSTANCE, Freelook.INSTANCE,
                Fullbright.INSTANCE, GlowESP.INSTANCE, Hand.INSTANCE, Hurtcam.INSTANCE,
                ItemPhysics.INSTANCE, NoFOV.INSTANCE, NoHurtCamera.INSTANCE, NoRender.INSTANCE,
                ParticleLimiter.INSTANCE, Particles.INSTANCE, PostProcessing.INSTANCE,
                SeeInvisibles.INSTANCE,
                SkeletonESP.INSTANCE, SkinChanger.INSTANCE, Skybox.INSTANCE, StreamerMode.INSTANCE,
                TNTTimer.INSTANCE, TargetESP.INSTANCE, TitleChanger.INSTANCE, Trajectories.INSTANCE,
                Zoom.INSTANCE,
        };
        // 已移除：ChinaHat（彩虹锥帽+光晕拖尾，过于猎奇）、Wings（翅膀装饰，过于猎奇）
        //         JumpCircles（跳跃光圈装饰）、Trails（实体拖尾装饰）—— 纯装饰，不符合 Opal 简洁风格
        // 如需启用，将对应 INSTANCE 加回上方数组即可。
        int count = 0;
        for (Module module : modules) {
            try {
                ModuleManager.INSTANCE.registerDynamic(module);
                count++;
            } catch (Throwable error) {
                DioxideLite.LOGGER.warn("Could not register render module {}", module.id(), error);
            }
        }
        DioxideLite.LOGGER.info("Registered {} advanced render modules.", count);
    }

    /**
     * Removes every module the client shipped in the RENDER category so this package is the only
     * render feature set. The HUD editor tool stays registered, otherwise there would be no way to
     * arrange the HUD layer any more.
     */
    private static void retireBuiltInRenderModules() {
        List<Module> builtIn = new ArrayList<>(ModuleManager.INSTANCE.modulesIn(Category.RENDER));
        int removed = 0;
        for (Module module : builtIn) {
            if (module instanceof HudEditorModule) {
                continue;
            }
            try {
                ModuleManager.INSTANCE.unregisterDynamic(module);
                removed++;
            } catch (Throwable error) {
                DioxideLite.LOGGER.warn("Could not remove built-in render module {}", module.id(), error);
            }
        }
        if (removed > 0) {
            DioxideLite.LOGGER.info("Removed {} built-in render modules.", removed);
        }
    }

    /** The built-in render module ids that this package replaces. */
    public static List<String> replacedBuiltInIds() {
        return List.of(SUPERSEDED);
    }
}
