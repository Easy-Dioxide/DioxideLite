package com.dioxidelite.ui.screen;

import com.dioxidelite.DioxideLite;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Path;

/** Native picker and persistence bridge for the shared menu backdrop. */
public final class MenuBackgroundSettings {

    private MenuBackgroundSettings() {
    }

    /** 导入自定义图片背景。 */
    public static boolean importImage() {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(
                "Import DioxideLite background",
                Minecraft.getInstance().gameDirectory.getAbsolutePath(),
                null,
                "PNG, JPG, BMP, or GIF image",
                false);
        if (selected == null || selected.isBlank()) {
            return false;
        }
        try {
            ScreenBackdrop.importMainMenuBackground(Path.of(selected));
            return true;
        } catch (IOException | RuntimeException exception) {
            DioxideLite.LOGGER.warn("Unable to import menu background {}", selected, exception);
            showError(exception.getMessage());
            return false;
        }
    }

    /**
     * 导入自定义视频背景。需要先把mp4 烘焙成帧序列，
     * 这一步耗时（1080p 约 18 秒），所以放到后台线程做。
     *
     * @return 后台任务句柄；{@code null} 表示已经在烘焙中
     */
    public static VideoBakeTask importVideo() {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(
                "Import DioxideLite background video",
                Minecraft.getInstance().gameDirectory.getAbsolutePath(),
                null,
                "MP4, MOV, or MKV video",
                false);
        if (selected == null || selected.isBlank()) {
            return null;
        }
        Path source = Path.of(selected);
        return VideoBakeTask.start(
                progress -> ScreenBackdrop.importMainMenuVideo(source, progress));
    }

    /** 启用打包在jar 里的内置视频背景（首次需要烘焙）。 */
    public static VideoBakeTask useBuiltinVideo() {
        return VideoBakeTask.start(ScreenBackdrop::bakeBuiltinVideo);
    }

    /** 切到默认图片背景。 */
    public static boolean reset() {
        ScreenBackdrop.resetMainMenuBackground();
        return true;
    }

    /** 切到动态网格背景。 */
    public static boolean useGrid() {
        ScreenBackdrop.requestMode(ScreenBackdrop.Mode.GRID);
        return true;
    }

    public static boolean canReset() {
        return ScreenBackdrop.canResetMainMenuBackground();
    }

    /** 当前背景模式的名字，用于在界面上显示切换结果。 */
    public static Component currentModeLabel() {
        return ScreenBackdrop.mode().displayName();
    }

    /**
     * 循环切到下一个可用的背景模式。
     *
     * <p>背景模式枚举属于内部实现，外部（如混入的选项界面）不应直接引用，
     * 统一从这里入口操作。没有素材的模式（自定义图片 / 自定义视频）会被跳过，
     * 保证每次点击都能看到背景真的变了。
     *
     * @return 切到内置视频时返回后台烘焙任务；其它模式返回 {@code null}
     */
    public static VideoBakeTask cycleMode() {
        ScreenBackdrop.Mode[] modes = ScreenBackdrop.Mode.values();
        ScreenBackdrop.Mode current = ScreenBackdrop.mode();
        for (int step = 1; step <= modes.length; step++) {
            ScreenBackdrop.Mode next = modes[(current.ordinal() + step) % modes.length];
            if (next == current) {
                break;
            }
            if (!ScreenBackdrop.isModeAvailable(next)) {
                continue;
            }
            if (next == ScreenBackdrop.Mode.BUILTIN_VIDEO) {
                return useBuiltinVideo();
            }
            ScreenBackdrop.requestMode(next);
            return null;
        }
        return null;
    }

    private static void showError(String detail) {
        String message = detail == null || detail.isBlank()
                ? "The selected file could not be imported."
                : detail;
        TinyFileDialogs.tinyfd_messageBox(
                "DioxideLite background",
                message,
                "ok",
                "error",
                1);
    }

    /**
     * 视频烘焙任务：后台线程解码，进度可在界面上轮询。
     */
    public static final class VideoBakeTask {

        private volatile int done;
        private volatile int total;
        private volatile boolean finished;
        private volatile boolean succeeded;
        private volatile String failure;

        static VideoBakeTask start(BakeAction action) {
            VideoBakeTask holder = new VideoBakeTask();
            Thread worker = new Thread(() -> {
                try {
                    action.bake((done, total) -> {
                        holder.done = done;
                        holder.total = total;
                    });
                    holder.succeeded = true;
                } catch (Throwable exception) {
                    holder.failure = exception.getMessage();
                    DioxideLite.LOGGER.warn("Unable to bake menu background video", exception);
                } finally {
                    holder.finished = true;
                }
            }, "DioxideLite-VideoBake");
            worker.setDaemon(true);
            worker.start();
            return holder;
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean isSucceeded() {
            return succeeded;
        }

        public String failure() {
            return failure;
        }

        /** 0..1 的进度；总量未知时返回 -1。 */
        public float progress() {
            int current = done;
            int all = total;
            if (all <= 0) return -1.0F;
            return Math.max(0.0F, Math.min(1.0F, current / (float) all));
        }
    }

    /** 烘焙动作签名，屏蔽掉源是「本地文件」还是「jar 内资源」的差异。 */
    private interface BakeAction {
        VideoBackgroundBaker.Manifest bake(VideoBackgroundBaker.Progress progress) throws IOException;
    }
}