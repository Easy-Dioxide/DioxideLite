package com.dioxidelite.module;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.event.EventBus;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.KeyInputEvent;
import com.dioxidelite.event.events.MouseButtonEvent;
import com.dioxidelite.module.modules.ClickGui;
import com.dioxidelite.module.modules.FontModule;
import com.dioxidelite.module.modules.player.IrcModule;
import com.dioxidelite.module.modules.render.FullBright;
import com.dioxidelite.module.modules.render.HudEditorModule;
// [DioxideLite 移植] SetsunaClient 模块导入
import com.dioxidelite.module.modules.AltManagerModule;
import com.dioxidelite.module.modules.combat.AntiBot;
import com.dioxidelite.module.modules.player.AntiResourcePack;
import com.dioxidelite.module.modules.player.AntiWeb;
import com.dioxidelite.module.modules.combat.AutoHitCrystal;
import com.dioxidelite.module.modules.player.AutoMLG;
import com.dioxidelite.module.modules.player.AutoTool;
import com.dioxidelite.module.modules.combat.AutoTotem;
import com.dioxidelite.module.modules.combat.Backtrack;
import com.dioxidelite.module.modules.player.BedAura;
import com.dioxidelite.module.modules.combat.Burrow;
import com.dioxidelite.module.modules.player.ChestStealer;
import com.dioxidelite.module.modules.combat.Criticals;
import com.dioxidelite.module.modules.combat.FakeLag;
import com.dioxidelite.module.modules.player.FakePlayer;
import com.dioxidelite.module.modules.player.FastBreak;
import com.dioxidelite.module.modules.player.FastCraftModule;
import com.dioxidelite.module.modules.movement.FlatElytraFly;
import com.dioxidelite.module.modules.player.GhostHand;
import com.dioxidelite.module.modules.render.HoleESP;
import com.dioxidelite.module.modules.player.InvManager;
import com.dioxidelite.module.modules.movement.InvMove;
import com.dioxidelite.module.modules.movement.KeepSprint;
import com.dioxidelite.module.modules.combat.KillAura;
import com.dioxidelite.module.modules.combat.KillAuraPlus;
import com.dioxidelite.module.modules.combat.MaceAura;
import com.dioxidelite.module.modules.misc.MiddleClickFriend;
import com.dioxidelite.module.modules.movement.MovementFix;
import com.dioxidelite.module.modules.movement.NoFall;
import com.dioxidelite.module.modules.movement.NoJumpDelay;
import com.dioxidelite.module.modules.movement.NoSlow;
import com.dioxidelite.module.modules.render.OreTracers;
import com.dioxidelite.module.modules.player.PacketEat;
import com.dioxidelite.module.modules.movement.Scaffold;
import com.dioxidelite.module.modules.render.SpawnerFinder;
import com.dioxidelite.module.modules.combat.SpearKill;
import com.dioxidelite.module.modules.movement.Speed;
import com.dioxidelite.module.modules.combat.Surround;
import com.dioxidelite.module.modules.render.Tracers;
import com.dioxidelite.module.modules.render.UHCDetector;
import com.dioxidelite.module.modules.movement.Velocity;
import com.dioxidelite.module.modules.render.Xray;
import com.dioxidelite.module.modules.combat.ZealotCrystalPlus;
import com.dioxidelite.module.modules.render.BlockHighlight;
import com.dioxidelite.module.modules.render.CameraClip;
import com.dioxidelite.module.modules.render.Compass;
import com.dioxidelite.module.modules.render.ItemTag;
import com.dioxidelite.module.modules.render.DioxideIslandModule;
import com.dioxidelite.module.modules.render.GlobalBlurModule;
import com.dioxidelite.module.modules.render.KillEffect;
import com.dioxidelite.module.modules.render.DeltaForceStyle;
import com.dioxidelite.module.modules.render.LegendWatch;
import com.dioxidelite.module.modules.movement.Sprint;
import com.dioxidelite.module.modules.render.NoRender;
import com.dioxidelite.module.modules.render.ESP;
import com.dioxidelite.module.modules.render.Chams;
import com.dioxidelite.module.modules.render.NameTags;
import com.dioxidelite.module.modules.render.TeamViewer;
import com.dioxidelite.module.modules.render.AttackRing;
import com.dioxidelite.module.modules.render.CombatVisuals;
import com.dioxidelite.module.modules.player.NetEaseMusicModule;
import com.dioxidelite.ui.hud.TargetHud;
import com.dioxidelite.ui.hud.ScaffoldBlockHUD;
import com.dioxidelite.ui.hud.MusicLyricsHUD;
import com.dioxidelite.render.NameTagLogoRenderer;
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
import net.minecraft.client.gui.screens.ChatScreen;
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
        register(HudEditorModule.INSTANCE);
        // [DioxideLite 移植] 以下模块移植自 SetsunaClient（上游开源版）。
        // AltManagerModule 此前从未被注册，导致该模块永远无法从 GUI 或按键打开，
        // 本次一并补上。
        // --- COMBAT ---
        register(AntiBot.INSTANCE);
        register(AutoHitCrystal.INSTANCE);
        register(AutoTotem.INSTANCE);
        register(Backtrack.INSTANCE);
        register(Burrow.INSTANCE);
        register(Criticals.INSTANCE);
        register(FakeLag.INSTANCE);
        register(KillAura.INSTANCE);
        register(KillAuraPlus.INSTANCE);
        register(MaceAura.INSTANCE);
        register(SpearKill.INSTANCE);
        register(Surround.INSTANCE);
        register(ZealotCrystalPlus.INSTANCE);
        // --- MISC ---
        register(MiddleClickFriend.INSTANCE);
        // --- MOVEMENT ---
        register(FlatElytraFly.INSTANCE);
        register(InvMove.INSTANCE);
        register(KeepSprint.INSTANCE);
        register(MovementFix.INSTANCE);
        register(NoFall.INSTANCE);
        register(NoJumpDelay.INSTANCE);
        register(NoSlow.INSTANCE);
        register(Scaffold.INSTANCE);
        register(Speed.INSTANCE);
        register(Velocity.INSTANCE);
        // --- PLAYER ---
        register(AntiResourcePack.INSTANCE);
        register(AntiWeb.INSTANCE);
        register(AutoMLG.INSTANCE);
        register(AutoTool.INSTANCE);
        register(BedAura.INSTANCE);
        register(ChestStealer.INSTANCE);
        register(FakePlayer.INSTANCE);
        register(FastBreak.INSTANCE);
        register(FastCraftModule.INSTANCE);
        register(GhostHand.INSTANCE);
        register(InvManager.INSTANCE);
        register(PacketEat.INSTANCE);
        // --- RENDER ---
        register(HoleESP.INSTANCE);
        register(OreTracers.INSTANCE);
        register(SpawnerFinder.INSTANCE);
        register(Tracers.INSTANCE);
        register(UHCDetector.INSTANCE);
        register(Xray.INSTANCE);
        // --- CLIENT ---
        register(AltManagerModule.INSTANCE);
        register(Sprint.INSTANCE);
        register(IrcModule.INSTANCE);
        register(FullBright.INSTANCE);
        register(BlockHighlight.INSTANCE);
        register(CameraClip.INSTANCE);
        register(Compass.INSTANCE);
        register(DeltaForceStyle.INSTANCE);
        register(ItemTag.INSTANCE);
        register(KillEffect.INSTANCE);
        register(LegendWatch.INSTANCE);
        register(NoRender.INSTANCE);
        register(ESP.INSTANCE);
        register(Chams.INSTANCE);
        register(NameTags.INSTANCE);
        register(TeamViewer.INSTANCE);
        register(AttackRing.INSTANCE);
        register(CombatVisuals.INSTANCE);
        register(NetEaseMusicModule.INSTANCE);
        register(TargetHud.INSTANCE);
        register(ScaffoldBlockHUD.INSTANCE);
        register(MusicLyricsHUD.INSTANCE);
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
        EventBus.INSTANCE.subscribe(NameTagLogoRenderer.INSTANCE);
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
                if ((mc.screen != null && !(mc.screen instanceof ChatScreen))
                        || mc.options.keyDebugModifier.isDown()) {
                    return;
                }
                for (Module module : modules) {
                    if (mc.screen instanceof ChatScreen && module != HudEditorModule.INSTANCE) continue;
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
