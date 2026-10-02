package com.dioxidelite.runtime;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（来源开源版）com/dioxidelite/runtime/FeatureRuntime.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.DioxideLite;
import com.dioxidelite.command.CommandManager;
import com.dioxidelite.config.ConfigManager;
import com.dioxidelite.manager.AltManager;
import com.dioxidelite.manager.HealthManager;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.manager.target.TargetManager;
import com.dioxidelite.module.ModuleManager;
import com.dioxidelite.notification.NotificationManager;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.script.LuaScriptManager;
import com.viaversion.dioxidelitevia.protocoltranslator.ProtocolTranslator;
import tritium.ncm.music.CloudMusic;

import java.util.concurrent.CompletableFuture;

/** Starts and stops the client feature runtime without an account gate. */
public final class FeatureRuntime {

    public static final FeatureRuntime INSTANCE = new FeatureRuntime();

    private boolean active;
    private boolean musicStarted;
    private int musicGeneration;

    private FeatureRuntime() {
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void activate() {
        if (active) return;
        try {
            active = true;
            AltManager.INSTANCE.load();
            RotationManager.INSTANCE.getClass();
            HealthManager.INSTANCE.getClass();
            TargetManager.INSTANCE.getClass();
            ModuleManager.INSTANCE.init();
            com.dioxidelite.module.modules.render.advanced.RenderModuleRegistry.registerAll();
            LuaScriptManager.INSTANCE.loadAll();
            ConfigManager.INSTANCE.load();
            // 主题状态先落地；启动动画附加项开启时立刻预热演出（加载画面阶段就能接管）
            com.dioxidelite.ui.theme.ThemeRuntime.init();
            com.dioxidelite.ui.dr.DrIntroPlayer.preloadAsync();
            CommandManager.INSTANCE.init();
            NotificationManager.INSTANCE.start();
            musicStarted = true;
            int generation = ++musicGeneration;
            CompletableFuture.runAsync(() -> {
                CloudMusic.initNCM();
                synchronized (FeatureRuntime.this) {
                    if (generation != musicGeneration || !active) {
                        CloudMusic.onStop();
                    }
                }
            });
            DioxideLite.LOGGER.info("Loaded {} modules.", ModuleManager.INSTANCE.modules().size());
        } catch (Throwable error) {
            if (ModuleManager.INSTANCE.isInitialized()) {
                LuaScriptManager.INSTANCE.unloadAll();
            }
            active = false;
            DioxideLite.LOGGER.error("Feature runtime initialization failed", error);
        }
    }

    public synchronized void deactivate() {
        if (ModuleManager.INSTANCE.isInitialized()) {
            ModuleManager.INSTANCE.disableAll();
            LuaScriptManager.INSTANCE.unloadAll();
        }
        NotificationManager.INSTANCE.clear();
        if (musicStarted) {
            musicGeneration++;
            CloudMusic.onStop();
            musicStarted = false;
        }
        active = false;
        ProtocolTranslator.setTargetVersion(ProtocolTranslator.NATIVE_VERSION);
    }

    public synchronized void shutdown() {
        ConfigManager.INSTANCE.save();
        if (ModuleManager.INSTANCE.isInitialized()) {
            LuaScriptManager.INSTANCE.unloadAll();
        }
        if (musicStarted) {
            musicGeneration++;
            CloudMusic.onStop();
            musicStarted = false;
        }
        active = false;
        com.dioxidelite.audio.AudioManager.close();
        SkijaRenderer.close();
    }
}
