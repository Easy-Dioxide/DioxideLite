package com.dioxideliteng.krs;

import com.dioxideliteng.krs.hacks.KrsFeature;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.combat.*;
import com.dioxideliteng.krs.hacks.features.exploit.Blink;
import com.dioxideliteng.krs.hacks.features.exploit.ExploitPatcher;
import com.dioxideliteng.krs.hacks.features.exploit.PathFinder;
import com.dioxideliteng.krs.hacks.features.exploit.PortalScreen;
import com.dioxideliteng.krs.hacks.features.exploit.TransactionConfirmBlinker;
import com.dioxideliteng.krs.hacks.features.exploit.XCarry;
import com.dioxideliteng.krs.hacks.features.level.*;
import com.dioxideliteng.krs.hacks.features.level.Timer;
import com.dioxideliteng.krs.hacks.features.movement.*;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyModule;
import com.dioxideliteng.krs.hacks.features.movement.speed.SpeedModule;
import com.dioxideliteng.krs.hacks.features.player.*;
import com.dioxideliteng.krs.hacks.features.render.*;
import com.dioxideliteng.module.Feature;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Creates and registers every ported KRS module with DioxideLiteNG's {@link com.dioxideliteng.module.FeatureManager}.
 *
 * <p>Naming: KRS names ("Kill Aura") are folded to DioxideLiteNG's CamelCase convention ("KillAura"). If a
 * DioxideLiteNG module already uses that name, the ported module gets a {@code 2} suffix ("KillAura2").
 *
 * <p>Deliberately not ported: KRS's own GUI/HUD (Interface, ClickGui, ImGui - DioxideLiteNG has its own), server-abuse
 * tools (ServerCrasher, ConsoleSpammer, PacketDuper, EntityDesync), anticheat disablers and detectors, client-identity
 * spoofers, joke/debug modules, and the packet-spread teleport/throw exploits (ChestTP, SurroundTP, LongThrow...).
 */
public final class KrsModules {
    private static final Logger LOGGER = LogUtils.getLogger();

    private KrsModules() {
    }

    private static List<Supplier<Module>> factories() {
        return List.of(
            // combat
            KillAura::new, Backtrack::new, AntiBot::new, AutoPot::new, Criticals::new, Teams::new, Velocity::new,
            TargetStrafe::new, FastBow::new, FastEat::new, MaceExploit::new, Reach::new, NoCombatDelay::new,
            CrossbowExploit::new, WTap::new, AutoTotem::new,
            // movement
            SpeedModule::new, FlyModule::new, ElytraFly::new, EntityControl::new, InventoryMove::new, Jesus::new,
            NoSlow::new, LongJump::new, Sprint::new, PerfectHorseJump::new, Step::new, AntiVoid::new, NoJumpCooldown::new,
            MovementFix::new, NoPush::new, EntityFly::new, SafeWalk::new, Parkour::new, AutoWalk::new, AutoSneak::new,
            NoWeb::new, AntiLevitation::new, FastFall::new, Spider::new, AutoRocket::new, Stasis::new,
            // player
            SpinBot::new, Phase::new, ChestStealer::new, FastLadder::new, InvManager::new, NoFall::new, AirPlace::new,
            Scaffold::new, Freecam::new, MurdererDetector::new, FastBreak::new, AutoFish::new,
            LookTP::new, HackerDetector::new, Stalker::new, SneakSpam::new, AutoRespawn::new, AutoLeave::new,
            MiddleClick::new, AntiAFK::new, AutoTool::new, DurabilityAlert::new,
            // level (world interaction)
            NoBreakCooldown::new, NoPlaceCooldown::new, Spammer::new, Breaker::new, CivBreak::new, Timer::new,
            Nuker::new, NoteBot::new, Xray::new, CaveFinder::new, AlwaysRiptide::new, QuickMacro::new,
            NoBlockEffects::new, ItemDropChanger::new, VisualSpoofer::new,
            // exploit (game-side only)
            Blink::new, TransactionConfirmBlinker::new, ExploitPatcher::new, PortalScreen::new, PathFinder::new, XCarry::new,
            // render
            ThunderDetector::new, KillEffect::new, NoRender::new, Camera::new, CameraNoClip::new,
            AntiBlind::new, FullBright::new, Capes::new, NoHurtCam::new, Zoom::new, ViewModel::new,
            WorldTime::new, Rotations::new, NameTags::new, ESP::new, Trails::new, OldHitting::new, WidelyPutin::new, Imposter::new, 
            FPSBobbing::new, TrueSight::new, HUDHider::new
        );
    }

    /** Appends the ported modules to {@code target}, which already holds DioxideLiteNG's own modules. */
    public static void registerAll(List<Feature> target) {
        KrsBridge.init();

        Set<String> taken = new HashSet<>();
        for (Feature feature : target) {
            taken.add(normalizedKey(feature.getName()));
        }

        int registered = 0;
        for (Supplier<Module> factory : factories()) {
            Module module;
            try {
                module = factory.get();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("Failed to construct a ported KRS module; skipping it", failure);
                continue;
            }

            String name = displayName(module.moduleName, taken);
            taken.add(normalizedKey(name));

            // KRS shipped several passive/visual modules switched on (ViewModel, Zoom, Rotations...). Surprising a
            // DioxideLiteNG user with changed visuals on first launch is worse, so only the protective ExploitPatcher
            // starts enabled. A saved config restores the user's own choices either way.
            boolean enabledByDefault = module instanceof ExploitPatcher;
            module.tempEnabled = false;
            KrsFeature feature;
            try {
                feature = new KrsFeature(module, name);
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("Failed to register ported KRS module {}", module.moduleName, failure);
                continue;
            }

            ModuleManager.register(module);
            KrsBridge.add(feature);
            target.add(feature);
            if (enabledByDefault) {
                feature.restoreEnabled(true);
            }
            registered++;
        }
        LOGGER.info("Registered {} ported KRS modules", registered);
    }

    /** "Kill Aura" -> "KillAura"; appends "2" while the name is already used by another module. */
    static String displayName(String krsName, Set<String> taken) {
        String base = krsName.replace(" ", "");
        String name = base;
        while (taken.contains(normalizedKey(name))) {
            name = name + "2";
        }
        return name;
    }

    private static String normalizedKey(String name) {
        return name.replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
