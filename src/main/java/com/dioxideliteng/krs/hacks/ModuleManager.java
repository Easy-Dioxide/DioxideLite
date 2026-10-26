package com.dioxideliteng.krs.hacks;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.EventManager;
import com.dioxideliteng.krs.events.EventListener;
import com.dioxideliteng.krs.events.features.*;
import com.dioxideliteng.krs.hacks.features.combat.*;
import com.dioxideliteng.krs.hacks.features.dev.*;
import com.dioxideliteng.krs.hacks.features.exploit.*;
import com.dioxideliteng.krs.hacks.features.exploit.disabler.DisablerModule;
import com.dioxideliteng.krs.hacks.features.movement.*;
import com.dioxideliteng.krs.hacks.features.movement.fly.FlyModule;
import com.dioxideliteng.krs.hacks.features.movement.speed.SpeedModule;
import com.dioxideliteng.krs.hacks.features.nulling.PluginsDetector;
import com.dioxideliteng.krs.hacks.features.player.*;
import com.dioxideliteng.krs.hacks.features.render.*;
import com.dioxideliteng.krs.hacks.features.level.*;
import com.dioxideliteng.krs.hacks.features.level.Timer;
import com.dioxideliteng.krs.utils.ChatUtil;
import com.dioxideliteng.krs.utils.GuiInputBlocker;
import com.dioxideliteng.krs.utils.entity.PlayerUtil;
import com.dioxideliteng.krs.utils.math.BehaviorUtils;
import com.dioxideliteng.krs.utils.math.TimerUtil;
import com.dioxideliteng.krs.utils.packet.BlinkUtil;
import com.dioxideliteng.krs.utils.render.RenderUtil;
import com.dioxideliteng.krs.render.Initializer;
import com.dioxideliteng.krs.utils.math.Tuple;
import com.dioxideliteng.krs.screen.NanoVGClickGuiScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.imguimc.customwindow.ModuleRenderable;
import com.dioxideliteng.krs.imguimc.screen.EmptyScreen;

import java.lang.reflect.Field;
import java.util.*;

public class ModuleManager implements EventListener {
    private static final ModuleManager INSTANCE = new ModuleManager();

    public static final List<Module> modules = new ArrayList<>();
    public static final List<Module> devModules = new ArrayList<>();
    public static final List<Module> allModules = new ArrayList<>();
    public static boolean isDebugRendering = false;

    public static int rotTick = 0;

    public static int transactionCounter = 0;
    public static boolean gettingTransactions = false;

    public static boolean plChecked = false;
    private static boolean needPlReCheck = false;

    private static final Map<Class<?>, List<Field>> settingsCache = new HashMap<>();
    private static final Map<Class<?>, Module> moduleByClass = new HashMap<>();
    private static final Map<String, Module> moduleByName = new HashMap<>();
    private static EventManager registeredEventManager;

    public static synchronized void onInitialize() {
        unregisterExistingListeners();

        clearModuleRegistry();

        modules.addAll(List.of(
                new SpeedModule(), new FlyModule(), new ElytraFly(), new EntityControl(), new NoBreakCooldown(),
                new Interface(), new KillAura(), new Backtrack(),
                new SpinBot(), new BrandSpoofer(), new AntiBot(), new AutoPot(), new Criticals(),
                new Teams(), new Velocity(), new Phase(), new ServerCrasher(), new Spammer(),
                new PerfectHorseJump(), new InventoryMove(), new Jesus(), new NoSlow(), new LongJump(),
                new ChestStealer(), new FastLadder(), new InvManager(), new NoFall(), new AirPlace(),
                new Sprint(), new ThunderDetector(), new KillEffect(), new CameraNoClip(),
                new AntiBlind(), new Breaker(), new CivBreak(), new Timer(), new Nuker(), new NoteBot(),
                new Xray(), new CaveFinder(), new Scaffold(), new Blink(), new TransactionConfirmBlinker(), new ExploitPatcher(),
                new PortalScreen(), new FullBright(), new TargetStrafe(), new DisablerModule(),
                new OldHitting(), new Freecam(), new Capes(), new MurdererDetector(),
                new NoHurtCam(), new FastBow(), new FastEat(), new Zoom(), new PathFinder(),
                new FastBreak(), new ViewModel(), new EntityDesync(), new Step(), new AntiVoid(),
                new AutoFish(), new NoJumpCooldown(), new AlwaysRiptide(),
                new WorldTime(), new ChatCommands(), new Rotations(),
                new PluginsDetector(), new NameTags(), new ESP(), new PlayerIndicator(), new MovementFix(), new LongThrow(),
                new MaceExploit(), new NoPush(), new LookTP(),
                new AntiCheatDetector(), new HackerDetector(), new PackSpoofer(),
                new EntityFly(), new WidelyPutin(), new Imposter(), new CustomModel(), new Stalker(),
                new PacketDuper(), new SneakSpam(), new ImGui(), new ClickGui(), new ConsoleSpammer(),
                new Reach(), new NoCombatDelay(), new AutoBypass(), new FakePinger(), new QuickMacro(),
                new FPSBobbing(), new CrossbowExploit(), new WTap(), new HatenaPiano(),
                new SafeWalk(), new Parkour(), new AutoWalk(), new AutoRespawn(),
                new AutoLeave(), new AutoSneak(), new MiddleClick(), new AntiAFK(),
                new NoWeb(), new AntiLevitation(), new FastFall(),
                new Spider(), new AutoRocket(), new AutoTotem(), new ItemDropChanger(),
                new XCarry(), new PortalGodMode(), new Stasis(), new ChestTP(), new SurroundTP(),
                new NoBreakReset(), new TrueSight(), new AutoTool(), new DurabilityAlert(), new HUDHider()
        ));

        devModules.addAll(List.of(
                new BlockBreakSimulator2(), new Debugga(), new NoMoreAutism(), new ChatExcepChecker(),
                new FukumaiPlayerTracker(), new MovementUtilTest(), new TuckMod(), new Sex()
        ));

        allModules.addAll(modules);
        allModules.addAll(devModules);

        sortModules(modules);
        sortModules(devModules);
        sortModules(allModules);

        for (Module module : allModules) {
            moduleByClass.put(module.getClass(), module);
            moduleByName.put(module.moduleName, module);
        }

        registeredEventManager = KrsClient.eventManager;
        registeredEventManager.register(INSTANCE);

        activateInitialModuleStates();
    }

    public static synchronized void shutdown() {
        unregisterExistingListeners();
    }

    private static void clearModuleRegistry() {
        modules.clear();
        devModules.clear();
        allModules.clear();
        moduleByClass.clear();
        moduleByName.clear();
        settingsCache.clear();
    }

    private static void sortModules(List<Module> moduleList) {
        moduleList.sort(Comparator.comparing(module -> module.moduleName));
    }

    private static void unregisterExistingListeners() {
        if (registeredEventManager != null) {
            registeredEventManager.unregister(INSTANCE);
            for (Module module : allModules)
                registeredEventManager.unregister(module);
        }
        for (Module module : allModules) {
            try {
                module.dispose(registeredEventManager);
            } catch (RuntimeException | Error failure) {
                System.err.println("Failed to dispose module: " + module.moduleName);
                failure.printStackTrace(System.err);
            }
        }
        registeredEventManager = null;
    }

    /**
     * 移植版策略：所有 KRS 模块一律以关闭状态启动。
     *
     * <p>KRS 原版会把构造时标记 {@code tempEnabled} 的模块（如 Interface）自动开启，
     * 但 DioxideLite NG 要求移植进来的模块默认全关，由用户在 ClickGui 里自行开启，
     * 避免一进服就开始发包。因此这里只清掉初始启用标记，不激活任何模块。</p>
     */
    private static void activateInitialModuleStates() {
        for (Module module : allModules) {
            if (module.tempEnabled) {
                module.tempEnabled = false;
            }
        }

        Interface.reloadSortedModules();
    }

    public static List<Field> getSettings(Object module) {
        if (module == null) return Collections.emptyList();
        return settingsCache.computeIfAbsent(module.getClass(), ModuleManager::collectSettings);
    }

    private static List<Field> collectSettings(Class<?> moduleClass) {
        List<Field> settings = new ArrayList<>();
        for (Field field : moduleClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Module.Setting.class)) continue;

            field.setAccessible(true);
            settings.add(field);
        }
        return List.copyOf(settings);
    }

    public static boolean getModuleState(Class<? extends Module> moduleClass) {
        Module m = moduleByClass.get(moduleClass);
        return m != null && m.tempEnabled;
    }

    public static <T extends Module> T getModule(Class<T> moduleClass) {
        Module m = moduleByClass.get(moduleClass);
        return m == null ? null : moduleClass.cast(m);
    }

    public static Module getModuleByName(String moduleName) {
        return moduleName == null ? null : moduleByName.get(moduleName);
    }

    public static void pullDebugScreen() {
        Initializer.pullEveryRenderable();
        if (mc.gui.screen() instanceof EmptyScreen) {
            GuiInputBlocker.setClosingDebugScreen(true);
            try {
                mc.gui.setScreen(null);
            } finally {
                GuiInputBlocker.setClosingDebugScreen(false);
            }
        }
        isDebugRendering = false;
        transactionCounter = 0;
        gettingTransactions = false;
    }

    @Override
    public void onWorld(WorldEvent event) {
        for (Module module : modules) {
            if (module.tempEnabled && module instanceof PluginsDetector)
                module.toggle();
            else if (module.tempEnabled && (module instanceof InvManager || module instanceof ChestStealer || module instanceof Scaffold || module instanceof KillAura || module instanceof Stasis)) {
                module.toggle();
                KrsClient.notificationManager.addNotification("World Change", "Automatically disabled " + module.moduleName);
            }
        }

        if (!ModuleManager.getModuleState(Breaker.class) && Breaker.resetAutoWhitelist(true))
            KrsClient.notificationManager.addNotification("Auto Whitelist", "Reset bed whitelist");

        // KRS 原版在此把模块配置落盘。移植后模块开关由 NG 的 FeatureManager 统一持久化，
        // 这里不再写 KRS 自己的配置文件，避免两套配置互相覆盖。

        BehaviorUtils.noKillAura = false;

        PlayerUtil.INSTANCE.fullResetSpoofState();

        if (KrsClient.nanoVgManager != null)
            KrsClient.nanoVgManager.discardQueuedRenderers();

        plChecked = false;
        PluginsDetector.plugins = null;
        PluginsDetector.detectedAcs = null;
        needPlReCheck = true;
    }

    @Override
    public void onSendPacket(SendPacketEvent event) {
        if (mc.player == null || mc.level == null) return;

        Packet<?> packet = event.packet;

        if (gettingTransactions && packet instanceof ServerboundPongPacket) {
            transactionCounter += 1;
            ChatUtil.printChat(((ServerboundPongPacket) packet).getId() + " (x" + transactionCounter + ")");
            ModuleRenderable.addCommandLog(((ServerboundPongPacket) packet).getId() + " (x" + transactionCounter + ")");
            if (transactionCounter >= 10) {
                ChatUtil.printChat("Logged 10x transactions!");
                ModuleRenderable.addCommandLog("Logged 10x transactions!");
                transactionCounter = 0;
                gettingTransactions = false;
            }
        }

        if (BlinkUtil.INSTANCE.getBlinking() && Blink.shouldBlinkOutgoing() && !BlinkUtil.INSTANCE.getLimiter() && !getModuleState(Freecam.class)) {
            if (KillAura.isSendingSpreadTeleportPacket()
                    || ChestTP.isSendingSpreadTeleportPacket()
                    || SurroundTP.isSendingSpreadTeleportPacket())
                return;

            if (packet instanceof ServerboundMovePlayerPacket
                    && (KillAura.shouldSuppressMovementDuringSpreadTeleport()
                    || ChestTP.shouldSuppressMovement()
                    || SurroundTP.shouldSuppressMovement())) {
                event.cancel();
                return;
            }

            if (packet instanceof ServerboundMovePlayerPacket || ModuleManager.getModuleState(KillAura.class) && KillAura.closestEntity != null && KillAura.shouldCancelUseItemOnWhileBlinking() && packet instanceof ServerboundUseItemOnPacket)
                event.cancel();

            if (packet instanceof ServerboundMovePlayerPacket.Pos || packet instanceof ServerboundMovePlayerPacket.Rot || packet instanceof ServerboundMovePlayerPacket.PosRot || packet instanceof ServerboundMovePlayerPacket.StatusOnly || packet instanceof ServerboundPlayerCommandPacket || packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundPlayerActionPacket || packet instanceof ServerboundUseItemPacket || ModuleManager.getModuleState(TransactionConfirmBlinker.class) && packet instanceof ServerboundPongPacket) {
                event.cancel();
                BlinkUtil.INSTANCE.addPacket(packet);
            }
        }
    }

    @Override
    public void onReceivedPacket(ReceivedPacketEvent event) {
        if (mc.player == null || mc.level == null) return;

        if (BlinkUtil.INSTANCE.getBlinking() && Blink.shouldQueueIncoming(event.packet) && !getModuleState(Freecam.class)) {
            event.cancel();
            BlinkUtil.INSTANCE.addIncomingPacket(event.packet);
        }
    }

    @Override
    public void onKey(KeyboardEvent event) {
        boolean shouldOpenEmptyScreen = false;

        if (GuiInputBlocker.shouldBlockMinecraftKeyEvent(event.key) && (event.key != ImGui.getOpenGuiKey() || event.key != ClickGui.getOpenGuiKey()))
            return;

        // KRS 原本在这里用右键 Shift 打开它自己的 NanoVG ClickGui。
        // 移植版不搬 KRS 界面，界面入口统一交给 NG 的 ClickGui，
        // 否则两个 ClickGui 会抢同一个按键（NG 的 ClickGui 默认也是右 Shift）。

        if ((event.key == ImGui.getOpenGuiKey() || event.key == Keys.KEY_ESCAPE) && event.action == Keys.PRESS) {
            if (!isDebugRendering) {
                if (event.key != ImGui.getOpenGuiKey()) return;
                Initializer.pushRenderable(new ModuleRenderable());
                isDebugRendering = true;
                GuiInputBlocker.releaseMovementKeys();
                mc.mouseHandler.releaseMouse();
                shouldOpenEmptyScreen = mc.gui.screen() == null;
            } else if (event.key == Keys.KEY_ESCAPE && !(mc.gui.screen() instanceof EmptyScreen) || event.key == ImGui.getOpenGuiKey())
                pullDebugScreen();
        }

        Interface interfaceModule = getModule(Interface.class);
        if (interfaceModule != null && interfaceModule.isHandlingTabGuiKey(event.key, event.action))
            return;

        if (mc.gui.screen() == null) {
            for (Module m : allModules) {
                if (event.key == m.key && event.action == Keys.PRESS)
                    m.toggle();
            }
        }

        if (shouldOpenEmptyScreen && mc.gui.screen() == null)
            mc.gui.setScreen(new EmptyScreen());
    }

    @Override
    public void onTick(TickEvent event) {
        // KRS 的 Timer / Spider / LongJump / TuckMod 通过直接写 TimerUtil.timerSpeed 改倍速，
        // 移植版不再重复注入 DeltaTracker（NG 的 MixinTimer 已 @Overwrite 该方法），
        // 而是每刻把该字段桥接到 NG 的 TimerController，让 KRS 模块行为保持一致。
        TimerUtil.applyToGameTimer();

        if (mc.level == null) return;

        Queue<Tuple<Integer, Integer>> pending = RenderUtil.getPending();
        if (!pending.isEmpty()) {
            int perTick = 4;

            int count = Math.min(perTick, pending.size());
            for (int i = 0; i < count; i++) {
                Tuple<Integer, Integer> pos = pending.poll();
                if (pos == null) break;

                int cx = pos.getFirst();
                int cz = pos.getSecond();
                mc.level.setSectionRangeDirty(cx >> 4, -64 >> 4, cz >> 4, (cx + 15) >> 4, mc.level.getHeight() >> 4, (cz + 15) >> 4);
            }
        }
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        KrsClient.rotationManager.update();

        if (needPlReCheck && mc.player.tickCount >= 50) {
            for (Module module : modules) {
                if (!module.tempEnabled && module instanceof PluginsDetector) {
                    module.toggle();
                    break;
                }
            }

            needPlReCheck = false;
            if (getModuleState(AntiCheatDetector.class))
                AntiCheatDetector.shouldNotify = true;
        }
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;

        if (isDebugRendering && mc.gui.screen() == null)
            mc.gui.setScreen(new EmptyScreen());

        if (KrsClient.rotationManager.isRotating()) {
            event.yaw = KrsClient.rotationManager.getRotationYaw();
            event.pitch = KrsClient.rotationManager.getRotationPitch();
            rotTick++;
        } else if (rotTick != 0) rotTick = 0;
    }
}
