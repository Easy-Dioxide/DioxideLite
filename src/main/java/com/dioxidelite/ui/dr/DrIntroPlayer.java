package com.dioxidelite.ui.dr;

import com.dioxidelite.DioxideLite;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

/**
 * 开机演出播放器：帧由 {@link IntroScenes} 按章合成，渲染桥走 Skija。
 *
 * 进入动画需等游戏初始加载结束（isGameLoadFinished），之前保持纯黑；
 * 播放期间每帧把 RGBA 画布包成 Skija Image 等比裁切铺满。
 * 跳过键 Z / 回车 / 空格沿用原版 button1_p 的轮询方式。
 */
public final class DrIntroPlayer {

    private static DrIntroPlayer instance;
    private static long startedAtMs = -1L;
    /** 预热最长等待上限（仅兜底）：游戏初始加载超过此时间也强制开播。 */
    private static final long WARMUP_MAX_MS = 60000L;

    private static volatile boolean done;

    private IntroFrameSource intro;
    private volatile boolean running;
    private volatile boolean finished;
    private boolean playbackStartedForDr;
    private long warmupStartMs = -1L;
    private int textureW = -1;
    private int textureH = -1;

    private DrIntroPlayer() {
    }

    /** 客户端启动时调用；启动动画附加项关闭时不创建播放器。 */
    public static void preload() {
        if (!DrThemeState.introEnabled) {
            done = true;
            return;
        }
        if (instance == null) {
            instance = new DrIntroPlayer();
            startedAtMs = System.currentTimeMillis();
            instance.warmupStartMs = startedAtMs;
            done = false;
        }
        instance.startInternal();
    }

    /** 游戏初始化完成前先异步把音源备好，不占用主线程。 */
    public static void preloadAsync() {
        CompletableFuture.runAsync(DrIntroPlayer::preload);
    }

    public static boolean isActive() {
        return instance != null;
    }

    /** 动画是否仍处于接管期（预热黑场或播放中）。 */
    public static boolean isPlaying() {
        return instance != null && instance.running && !instance.finished;
    }

    /** 动画是否已彻底结束（播完/失败/已关闭）。 */
    public static boolean isFinished() {
        return done || (instance != null && instance.finished);
    }

    public static void close() {
        done = true;
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
        startedAtMs = -1L;
    }

    /** 重播当前章节的演出（主题设置里的重播按钮）。 */
    public static void replay() {
        if (!DrThemeState.introEnabled) {
            return;
        }
        close();
        IntroSfx.stopAll();
        done = false;
        instance = new DrIntroPlayer();
        startedAtMs = System.currentTimeMillis();
        instance.warmupStartMs = startedAtMs;
        instance.startInternal();
    }

    /** 当前选中的演出章节（启动动画附加项 1~5）。 */
    private static int chapter() {
        return Math.max(1, Math.min(DrTheme.CHAPTERS.length, DrThemeState.introChapter));
    }

    private synchronized void startInternal() {
        if (running) {
            return;
        }
        running = true;
        // 先落盘再建场景：场景构造里就会 snd_play，顺序反了第一声读不到文件
        IntroSfx.prepare();
        intro = IntroScenes.create(chapter());
    }

    /**
     * 画一帧。返回 true 表示已接管画面（含预热黑场），false 表示演出已结束/未创建。
     */
    public static boolean render(Canvas canvas, float screenW, float screenH) {
        if (instance == null) {
            return false;
        }
        return instance.renderInternal(canvas, screenW, screenH);
    }

    private boolean renderInternal(Canvas canvas, float screenW, float screenH) {
        if (!running || intro == null) {
            return false;
        }
        boolean warmupBlack = false;
        long now = System.currentTimeMillis();

        if (!playbackStartedForDr) {
            if (warmupStartMs < 0L) {
                warmupStartMs = now;
            }
            boolean gameReady = Minecraft.getInstance().isGameLoadFinished();
            boolean forceStart = now - warmupStartMs >= WARMUP_MAX_MS;
            if (gameReady || forceStart) {
                playbackStartedForDr = true;
            } else {
                warmupBlack = true;
            }
        }
        if (!warmupBlack) {
            intro.advance(now);
            if (intro.isFinished()) {
                finished = true;
                // 演出结束回收所有演出音效（原版 exit_screen 里 snd_free）
                IntroSfx.stopAll();
                return false;
            }
            if (pollSkipKey() && intro.requestSkip()) {
                // ch1 的底噪在 Ch1ProcessLogo.onSkipRequested 里单轨淡出，
                // ch2~5 才是所有演出音效一起淡出
                if (!IntroScenes.usesIntronoise(chapter())) {
                    IntroSfx.fadeOutAll(20 * IntroScenes.frameMs(chapter()));
                }
            }
        }

        if (warmupBlack) {
            canvas.drawColor(0xFF000000);
            return true;
        }

        int frameW = intro.width();
        int frameH = intro.height();
        if (frameW <= 0 || frameH <= 0) {
            canvas.drawColor(0xFF000000);
            return true;
        }
        textureW = frameW;
        textureH = frameH;

        Image frame = null;
        try {
            frame = Image.makeRaster(new ImageInfo(frameW, frameH,
                    ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), intro.frame(), frameW * 4L);
            drawCover(canvas, frame, screenW, screenH);
        } catch (Throwable t) {
            DioxideLite.LOGGER.warn("[DioxideLite][DR] intro frame upload failed", t);
            canvas.drawColor(0xFF000000);
        } finally {
            if (frame != null) {
                frame.close();
            }
        }
        return true;
    }

    /** 跳过键轮询（GLFW 直读；加载画面阶段没有 Screen 按键事件可用）。 */
    private static boolean pollSkipKey() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ENTER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
    }

    private static final Paint COVER_PAINT = new Paint().setAntiAlias(true);

    private void drawCover(Canvas canvas, Image frame, float screenW, float screenH) {
        float screenAspect = screenW / Math.max(1f, screenH);
        float textureAspect = textureW / (float) textureH;
        float sourceW = textureW;
        float sourceH = textureH;
        float sourceX = 0f;
        float sourceY = 0f;
        if (textureAspect > screenAspect) {
            sourceW = Math.max(1f, textureH * screenAspect);
            sourceX = Math.max(0f, (textureW - sourceW) * 0.5f);
        } else if (textureAspect < screenAspect) {
            sourceH = Math.max(1f, textureW / screenAspect);
            sourceY = Math.max(0f, (textureH - sourceH) * 0.5f);
        }
        canvas.drawImageRect(frame,
                Rect.makeXYWH(sourceX, sourceY, sourceW, sourceH),
                Rect.makeXYWH(0f, 0f, screenW, screenH),
                SamplingMode.DEFAULT, COVER_PAINT, true);
    }

    private void shutdown() {
        running = false;
        finished = true;
        intro = null;
        IntroSfx.stopAll();
    }
}
