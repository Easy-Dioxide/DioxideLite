package com.dioxideliteng.krs;

import com.dioxideliteng.krs.events.EventManager;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.utils.NotificationManager;
import com.dioxideliteng.krs.utils.nanovg.NanoVGManager;
import com.dioxideliteng.krs.utils.network.FileUtil;
import com.dioxideliteng.krs.utils.rotation.RotationManager;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * KRS 移植体系的运行时入口。
 *
 * <p>原版 {@code com.instrumentalist.krs.Client} 持有 KRS 全部单例并驱动其生命周期。
 * 移植到 DioxideLite NG 后只保留<b>模块运行必需</b>的部分：事件总线、旋转系统、
 * 通知系统与模块注册表。NanoVG / ImGui / 配置界面相关的单例不再搬入，
 * 模块开关与参数由 NG 自己的 ClickGui 呈现。</p>
 */
public final class KrsClient implements IMinecraft {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 标识这批模块来自 KRS 移植，供 NG 侧展示与区分。 */
    public static final String BUILD_TAG = "Krs-Port";
    /** 兼容原 KRS 里读取版本号字符串的位置。 */
    public static final String clientVersion = "1.3.1-Dev";

    public static volatile EventManager eventManager = null;
    public static RotationManager rotationManager = null;
    public static NanoVGManager nanoVgManager = null;
    public static NotificationManager notificationManager = null;
    /** 模块配置目录（相对于游戏目录）。 */
    public static String configLocation = "DioxideLiteNG-krs";
    public static volatile boolean loaded = false;

    private static final AtomicBoolean initialized = new AtomicBoolean();
    private static final AtomicBoolean shutdown = new AtomicBoolean();

    private KrsClient() {
    }

    /**
     * 幂等初始化，由 NG 客户端入口在 Minecraft 构造完成后调用。
     *
     * <p>不主动加载 KRS 配置文件：模块开关由 NG 的 {@code FeatureManager} 统一管理，
     * 避免两套配置互相覆盖。</p>
     */
    public static void init() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        eventManager = new EventManager();
        // RotationManager 与 Minecraft 实例强相关（构造期读取 mc.player），
        // 在没有游戏实例的场合（单元验证、无头环境）跳过，避免初始化整体失败。
        rotationManager = mc == null ? null : createRotationManager();
        nanoVgManager = new NanoVGManager();
        notificationManager = new NotificationManager();

        // 必须在事件总线就绪之后：Module.setState 会向 eventManager 注册监听。
        ModuleManager.onInitialize();

        loaded = true;
        LOGGER.info("[KRS-Port] bridge ready, {} modules registered", ModuleManager.modules.size());
    }

    private static RotationManager createRotationManager() {
        try {
            return new RotationManager();
        } catch (Throwable throwable) {
            LOGGER.warn("[KRS-Port] rotation manager unavailable, rotation-based modules will be inactive", throwable);
            return null;
        }
    }

    public static void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }
        loaded = false;
        runQuietly("modules", ModuleManager::shutdown);
        runQuietly("network executor", FileUtil.INSTANCE::shutdown);
        runQuietly("notifications", () -> {
            if (notificationManager != null) {
                notificationManager.clear();
            }
        });
        runQuietly("NanoVG queue", () -> {
            if (nanoVgManager != null) {
                nanoVgManager.shutdown();
            }
        });
    }

    private static void runQuietly(String resource, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            LOGGER.warn("[KRS-Port] failed to release {}", resource, throwable);
        }
    }

    public static void copyToClipboard(String string) {
        if (string == null) {
            return;
        }
        mc.keyboardHandler.setClipboard(string);
    }
}
