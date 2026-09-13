package com.dioxidelite.module;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.KeyInputEvent;
import com.dioxidelite.event.events.MouseButtonEvent;
import com.dioxidelite.module.modules.ClickGui;
import com.dioxidelite.module.modules.FontModule;
import com.dioxidelite.module.modules.render.FullBright;
import com.dioxidelite.module.modules.render.BlockHighlight;
import com.dioxidelite.module.modules.render.CameraClip;
import com.dioxidelite.module.modules.render.Compass;
import com.dioxidelite.module.modules.render.ItemTag;
import com.dioxidelite.module.modules.render.DioxideIslandModule;
import com.dioxidelite.module.modules.render.GlobalBlurModule;
import com.dioxidelite.module.modules.render.KillEffect;
import com.dioxidelite.module.modules.render.DeltaForceStyle;
import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.module.modules.render.NoRender;
import com.dioxidelite.ui.hud.BPSHUD;
import com.dioxidelite.ui.hud.CoordinatesHUD;
import com.dioxidelite.ui.hud.FPSHUD;
import com.dioxidelite.ui.hud.HUD;
import com.dioxidelite.ui.hud.HudFusionManager;
import com.dioxidelite.ui.hud.InventoryHUD;
import com.dioxidelite.ui.hud.KeybindOverlayHUD;
import com.dioxidelite.ui.hud.ModuleListHUD;
import com.dioxidelite.ui.hud.Notifications;
import com.dioxidelite.ui.hud.PotionHUD;
import com.dioxidelite.ui.hud.RadarHUD;
import com.dioxidelite.ui.hud.ScoreboardHUD;
import com.dioxidelite.ui.hud.SessionInfoHUD;
import com.dioxidelite.ui.hud.PerformanceHUD;
import com.dioxidelite.ui.hud.WatermarkHUD;
import com.dioxidelite.util.client.InputBind;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every {@link Module} instance: registers them, binds their i18n keys, and
 * dispatches key-bind presses. Registered once from {@link com.dioxidelite.DioxideLiteClient}.
 */
public final class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private final Minecraft mc = DioxideLite.mc();
    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, SmartKeyboardState> smartKeyboardStates = new IdentityHashMap<>();
    private final Map<Module, SmartMouseState> smartMouseStates = new IdentityHashMap<>();
    private boolean initialized;

    private static final long SMART_MOUSE_HOLD_THRESHOLD_NANOS = 200_000_000L;

    private enum SmartKeyboardState {
        PENDING_ENABLED,
        PENDING_DISABLED,
        HOLDING
    }

    private record SmartMouseState(boolean previouslyEnabled, long pressTimeNanos) {
    }

    private ModuleManager() {
    }

    /** Instantiates and registers all built-in modules, then listens for keybinds. */
    public void init() {
        if (initialized) {
            return;
        }
        register(ClickGui.INSTANCE);
        register(FontModule.INSTANCE);
        register(FullBright.INSTANCE);
        register(BlockHighlight.INSTANCE);
        register(CameraClip.INSTANCE);
        register(Compass.INSTANCE);
        register(DeltaForceStyle.INSTANCE);
        register(ItemTag.INSTANCE);
        register(KillEffect.INSTANCE);
        register(LegendWatch.INSTANCE);
        register(NoRender.INSTANCE);
        register(DioxideIslandModule.INSTANCE);
        register(GlobalBlurModule.INSTANCE);
        register(WatermarkHUD.INSTANCE);
        register(PerformanceHUD.INSTANCE);
        register(ModuleListHUD.INSTANCE);
        register(FPSHUD.INSTANCE);
        register(BPSHUD.INSTANCE);
        register(CoordinatesHUD.INSTANCE);
        register(InventoryHUD.INSTANCE);
        register(KeybindOverlayHUD.INSTANCE);
        register(PotionHUD.INSTANCE);
        register(RadarHUD.INSTANCE);
        register(ScoreboardHUD.INSTANCE);
        register(SessionInfoHUD.INSTANCE);
        register(Notifications.INSTANCE);
        register(HUD.INSTANCE);
        EventBus.INSTANCE.subscribe(HudFusionManager.INSTANCE);
        EventBus.INSTANCE.subscribe(this);
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Disables all active behavior when the authentication lease closes. */
    public void disableAll() {
        smartKeyboardStates.clear();
        smartMouseStates.clear();
        for (Module module : modules) {
            module.setEnabled(false);
        }
    }

    private void register(Module module) {
        if (modules.stream().anyMatch(existing -> existing.id().equals(module.id()))) {
            throw new IllegalStateException("Duplicate module id: " + module.id());
        }
        modules.add(module);
        module.bindI18n();
        module.captureConfigDefaults();
    }

    /** Registers a runtime-owned module such as one declared by a Lua script. */
    public synchronized void registerDynamic(Module module) {
        if (!initialized) {
            throw new IllegalStateException("Module manager is not initialized");
        }
        register(module);
    }

    /** Removes one runtime-owned module after stopping all of its active behavior. */
    public synchronized void unregisterDynamic(Module module) {
        if (!modules.contains(module)) {
            return;
        }
        module.setEnabled(false);
        smartKeyboardStates.remove(module);
        smartMouseStates.remove(module);
        modules.remove(module);
    }

    public List<Module> modules() {
        return modules;
    }

    /** All modules in a category, in registration order. */
    public List<Module> modulesIn(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.category() == category) {
                result.add(module);
            }
        }
        return result;
    }

    @Listen
    private void onKey(KeyInputEvent event) {
        if (event.key() == GLFW.GLFW_KEY_UNKNOWN) {
            return;
        }

        switch (event.action()) {
            case GLFW.GLFW_PRESS -> {
                if (mc.screen != null || mc.options.keyDebugModifier.isDown()) {
                    return;
                }
                for (Module module : modules) {
                    InputBind bind = module.bind();
                    if (!bind.matchesKey(event.key()) || !bind.matchesModifiers(event.modifiers())) {
                        continue;
                    }
                    handleKeyboardPress(module, bind);
                }
            }
            case GLFW.GLFW_REPEAT -> {
                for (Module module : modules) {
                    InputBind bind = module.bind();
                    if (bind.action() == InputBind.BindAction.SMART
                            && bind.matchesKey(event.key())
                            && smartKeyboardStates.containsKey(module)) {
                        smartKeyboardStates.put(module, SmartKeyboardState.HOLDING);
                    }
                }
            }
            case GLFW.GLFW_RELEASE -> {
                for (Module module : modules) {
                    InputBind bind = module.bind();
                    if (!module.isToggleable() || !bind.isAffectedByKeyRelease(event.key())) {
                        continue;
                    }
                    switch (bind.action()) {
                        case HOLD -> module.setEnabled(false);
                        case SMART -> {
                            SmartKeyboardState state = smartKeyboardStates.remove(module);
                            if (state != null) {
                                module.setEnabled(state == SmartKeyboardState.PENDING_DISABLED);
                            }
                        }
                        case TOGGLE -> {
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    private void handleKeyboardPress(Module module, InputBind bind) {
        if (!module.isToggleable()) {
            module.trigger();
            return;
        }
        switch (bind.action()) {
            case TOGGLE -> module.toggle();
            case HOLD -> module.setEnabled(true);
            case SMART -> {
                smartKeyboardStates.put(module, module.isEnabled()
                        ? SmartKeyboardState.PENDING_ENABLED
                        : SmartKeyboardState.PENDING_DISABLED);
                module.setEnabled(true);
            }
        }
    }

    @Listen
    private void onMouse(MouseButtonEvent event) {
        switch (event.action()) {
            case GLFW.GLFW_PRESS -> {
                if (mc.screen != null) {
                    return;
                }
                for (Module module : modules) {
                    InputBind bind = module.bind();
                    if (!bind.matchesMouse(event.button()) || !bind.matchesModifiers(event.modifiers())) {
                        continue;
                    }
                    if (!module.isToggleable()) {
                        module.trigger();
                        continue;
                    }
                    switch (bind.action()) {
                        case TOGGLE -> module.toggle();
                        case HOLD -> module.setEnabled(true);
                        case SMART -> {
                            smartMouseStates.put(module,
                                    new SmartMouseState(module.isEnabled(), System.nanoTime()));
                            module.setEnabled(true);
                        }
                    }
                }
            }
            case GLFW.GLFW_RELEASE -> {
                for (Module module : modules) {
                    InputBind bind = module.bind();
                    if (!module.isToggleable() || !bind.matchesMouse(event.button())) {
                        continue;
                    }
                    switch (bind.action()) {
                        case HOLD -> module.setEnabled(false);
                        case SMART -> {
                            SmartMouseState state = smartMouseStates.remove(module);
                            if (state == null) {
                                continue;
                            }
                            boolean held = System.nanoTime() - state.pressTimeNanos()
                                    >= SMART_MOUSE_HOLD_THRESHOLD_NANOS;
                            module.setEnabled(held ? false : !state.previouslyEnabled());
                        }
                        case TOGGLE -> {
                        }
                    }
                }
            }
            default -> {
            }
        }
    }
}
