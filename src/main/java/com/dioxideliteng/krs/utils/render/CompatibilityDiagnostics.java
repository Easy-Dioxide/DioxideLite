package com.dioxideliteng.krs.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuDevice;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.sdl.SDLError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 兼容渲染层的诊断日志。
 *
 * <p>KRS 的 NanoVG / ImGui 走的是「隐藏 OpenGL 上下文 + 跨 API 纹理回传」这条非常规路径，
 * 在真实 GPU 驱动上出问题的概率远高于软件渲染。出问题时需要的信息（后端名、SDL 版本、
 * 具体失败的 OpenGL 调用）在普通 latest.log 里往往被淹没，所以单独写一份
 * {@code DioxideLiteNG-krs-render.log} 到游戏目录，方便直接回传定位。</p>
 */
public final class CompatibilityDiagnostics {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FILE_NAME = "DioxideLiteNG-krs-render.log";
    /** 单文件上限，避免长期运行把磁盘写满。 */
    private static final long MAX_BYTES = 512L * 1024L;

    private CompatibilityDiagnostics() {
    }

    /** 记录一次兼容层相关事件（含完整堆栈）到诊断文件。写入失败时静默忽略。 */
    public static void report(String stage, Throwable failure) {
        StringBuilder builder = new StringBuilder(1024);
        builder.append("===== ").append(LocalDateTime.now().format(TIME_FORMAT))
                .append(" [").append(stage).append("] =====\n");
        appendEnvironment(builder);
        if (failure != null) {
            StringWriter writer = new StringWriter();
            failure.printStackTrace(new PrintWriter(writer));
            builder.append(writer);
        }
        builder.append('\n');
        write(builder.toString());
    }

    /** 记录一行环境摘要（用于「用户说崩了但没日志」时的一手信息）。 */
    public static void reportEnvironment() {
        StringBuilder builder = new StringBuilder(512);
        builder.append("===== ").append(LocalDateTime.now().format(TIME_FORMAT))
                .append(" [environment] =====\n");
        appendEnvironment(builder);
        builder.append('\n');
        write(builder.toString());
    }

    private static void appendEnvironment(StringBuilder builder) {
        builder.append("os.name         = ").append(System.getProperty("os.name"))
                .append(' ').append(System.getProperty("os.version"))
                .append(" (").append(System.getProperty("os.arch")).append(")\n");
        builder.append("java.version    = ").append(System.getProperty("java.version")).append('\n');

        try {
            GpuDevice device = RenderSystem.getDevice();
            builder.append("gpu.backend     = ")
                    .append(device == null ? "unavailable" : device.getDeviceInfo().backendName()).append('\n');
        } catch (Throwable failure) {
            builder.append("gpu.backend     = <error: ").append(failure).append(">\n");
        }

        try {
            // SDL 的错误串是排查隐藏 GL 上下文创建失败的第一手材料。
            builder.append("sdl.last_error  = ").append(SDLError.SDL_GetError()).append('\n');
        } catch (Throwable failure) {
            builder.append("sdl.last_error  = <error: ").append(failure).append(">\n");
        }

        try {
            // 用离屏上下文自己的能力表判断，避免受「当前线程没有绑定 GL 上下文」影响。
            GLCapabilities capabilities = GraphicsApiCompatibility.getOffscreenCapabilities();
            if (capabilities == null) {
                builder.append("gl.capabilities = <offscreen context not created>\n");
            } else {
                builder.append("gl.version      = ")
                        .append(capabilities.OpenGL45 ? "4.5+" : capabilities.OpenGL43 ? "4.3+"
                                : capabilities.OpenGL33 ? "3.3+" : "below 3.3").append('\n');
                builder.append("gl.compute      = ").append(capabilities.GL_ARB_compute_shader).append('\n');
            }
        } catch (Throwable failure) {
            builder.append("gl.capabilities = <error: ").append(failure).append(">\n");
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getWindow() != null) {
                builder.append("window.size     = ")
                        .append(minecraft.getWindow().getWidth()).append('x')
                        .append(minecraft.getWindow().getHeight()).append('\n');
            }
        } catch (Throwable ignored) {
            // 窗口信息只是辅助，取不到就算了。
        }

        builder.append("compat.enabled  = ").append(GraphicsApiCompatibility.usesCompatibilityRenderer()).append('\n');
        String reason = GraphicsApiCompatibility.getDisabledReason();
        if (reason != null) {
            builder.append("compat.disabled = ").append(reason).append('\n');
        }
    }

    private static void write(String text) {
        try {
            Path path = resolvePath();
            if (path == null)
                return;
            if (Files.exists(path) && Files.size(path) > MAX_BYTES) {
                Files.delete(path);
            }
            Files.writeString(path, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
            // 诊断日志本身绝不能再引发问题。
        }
    }

    private static Path resolvePath() {
        try {
            return Path.of(FILE_NAME).toAbsolutePath();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
