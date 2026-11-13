package com.dioxideliteng.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVideo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 独立的渲染诊断日志。
 *
 * <p>原生崩溃（LWJGL 的 {@code FATAL ERROR in native method: No context is current}、
 * Windows 上的 access violation）不会写 latest.log，也不会被 Java 的 try/catch 拦住，
 * 所以此前「崩了但完全没有日志」。本类把环境摘要和每次降级/异常单独写到游戏目录的
 * {@code DioxideLiteNG-render.log}，用户只需回传这一个文件即可定位。</p>
 *
 * <p>本类中的所有外部调用都必须包 try/catch：诊断代码本身绝不能成为新的崩溃源。</p>
 */
public final class RenderDiagnostics {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FILE_NAME = "DioxideLiteNG-render.log";
    private static final long MAX_BYTES = 512L * 1024L;
    /** 同一阶段最多写 3 次，避免每帧复现的问题把日志刷满并反复删档。 */
    private static final int MAX_PER_STAGE = 3;
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> COUNTS
            = new java.util.concurrent.ConcurrentHashMap<>();

    private RenderDiagnostics() {
    }

    private static boolean allowWrite(String stage) {
        try {
            return COUNTS.computeIfAbsent(stage, key -> new java.util.concurrent.atomic.AtomicInteger())
                    .incrementAndGet() <= MAX_PER_STAGE;
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** 记录一次带完整堆栈的失败。 */
    public static void report(String stage, Throwable failure) {
        if (!allowWrite(stage)) {
            return;
        }
        StringBuilder builder = header(stage);
        appendEnvironment(builder);
        if (failure != null) {
            StringWriter writer = new StringWriter();
            failure.printStackTrace(new PrintWriter(writer));
            builder.append(writer);
        }
        builder.append('\n');
        write(builder);
    }

    /** 记录一条不带堆栈的说明（例如「跳过某条危险路径」）。 */
    public static void report(String stage, String message) {
        if (!allowWrite(stage)) {
            return;
        }
        StringBuilder builder = header(stage);
        builder.append("message        = ").append(message).append('\n');
        appendEnvironment(builder);
        builder.append('\n');
        write(builder);
    }

    /** 启动时记录一次环境摘要，用于「崩了但没日志」的场景。 */
    public static void reportEnvironment() {
        if (!allowWrite("environment")) {
            return;
        }
        StringBuilder builder = header("environment");
        appendEnvironment(builder);
        builder.append('\n');
        write(builder);
    }

    private static StringBuilder header(String stage) {
        return new StringBuilder(1024)
                .append("===== ").append(LocalDateTime.now().format(TIME_FORMAT))
                .append(" [").append(stage).append("] =====\n");
    }

    private static void appendEnvironment(StringBuilder builder) {
        builder.append("os              = ").append(System.getProperty("os.name", "?"))
                .append(' ').append(System.getProperty("os.version", "?"))
                .append(" (").append(System.getProperty("os.arch", "?")).append(")\n");
        builder.append("java            = ").append(System.getProperty("java.version", "?"))
                .append(" / ").append(System.getProperty("java.vm.name", "?")).append('\n');

        try {
            var device = RenderSystem.tryGetDevice();
            builder.append("gpu.backend     = ")
                    .append(device == null ? "<device unavailable>" : device.getDeviceInfo().backendName())
                    .append('\n');
        } catch (Throwable failure) {
            builder.append("gpu.backend     = <error: ").append(failure).append(">\n");
        }

        // 关键：GL.getCapabilities() 只是线程局部缓存，即使上下文已经被
        // SDL_GL_MakeCurrent(nullptr) 解绑也可能非 null。只有 SDL 这两个值
        // 才能证明「当前线程真的有一个可用的 OpenGL 上下文」。
        try {
            builder.append("gl.current_ctx  = ").append(SDLVideo.SDL_GL_GetCurrentContext()).append('\n');
            builder.append("gl.current_win  = ").append(SDLVideo.SDL_GL_GetCurrentWindow()).append('\n');
        } catch (Throwable failure) {
            builder.append("gl.current_ctx  = <error: ").append(failure).append(">\n");
        }

        try {
            GLCapabilities caps = GL.getCapabilities();
            if (caps == null) {
                builder.append("gl.capabilities = <none on this thread>\n");
            } else {
                builder.append("gl.capabilities = ")
                        .append(caps.OpenGL45 ? "4.5+" : caps.OpenGL43 ? "4.3+"
                                : caps.OpenGL33 ? "3.3+" : "below 3.3")
                        .append(" (stencilFbo=").append(caps.GL_ARB_framebuffer_object)
                        .append(")\n");
            }
        } catch (Throwable failure) {
            builder.append("gl.capabilities = <error: ").append(failure).append(">\n");
        }

        try {
            builder.append("sdl.last_error  = ").append(SDLError.SDL_GetError()).append('\n');
        } catch (Throwable failure) {
            builder.append("sdl.last_error  = <error: ").append(failure).append(">\n");
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getWindow() != null) {
                builder.append("window.size     = ")
                        .append(minecraft.getWindow().getWidth()).append('x')
                        .append(minecraft.getWindow().getHeight()).append('\n');
            }
        } catch (Throwable ignored) {
            // 窗口信息只是辅助信息
        }

        // 只读取纯字段 getter：这里绝不能回调 usesGpuBackend() 等会再写诊断的方法，
        // 否则异常路径上会出现「写日志 -> 探测 -> 再写日志」的无限递归。
        builder.append("nvg.failed      = ").append(NVGRenderer.isFailed()).append('\n');
        String reason = NVGRenderer.getDisabledReason();
        if (reason != null) {
            builder.append("nvg.disabled    = ").append(reason).append('\n');
        }
    }

    private static void write(StringBuilder builder) {
        try {
            Path path = resolvePath();
            if (path == null) {
                return;
            }
            if (Files.exists(path) && Files.size(path) > MAX_BYTES) {
                Files.delete(path);
            }
            Files.writeString(path, builder.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
            // 诊断日志本身绝不能再引发问题
        }
    }

    private static Path resolvePath() {
        try {
            return FabricLoader.getInstance().getGameDir().resolve(FILE_NAME);
        } catch (Throwable ignored) {
            try {
                return Path.of(FILE_NAME).toAbsolutePath();
            } catch (Throwable again) {
                return null;
            }
        }
    }
}
