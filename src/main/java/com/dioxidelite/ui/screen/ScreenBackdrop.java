package com.dioxidelite.ui.screen;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.ui.UiTheme;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;

/** Full-screen animated architectural backdrop shared by standalone client screens. */
final class ScreenBackdrop {

    private static final Paint GRADIENT_PAINT = new Paint().setAntiAlias(true).setDither(true);
    private static final Paint LINE_PAINT = new Paint().setAntiAlias(true).setStrokeWidth(1.0F);
    private static final Paint IMAGE_PAINT = new Paint().setAntiAlias(true).setDither(true);
    private static final Paint BLEND_PAINT = new Paint().setAntiAlias(true).setDither(true);
    private static final Image MAIN_MENU_BACKGROUND = loadImage(
            "/assets/dioxide-lite/textures/mainmenu/background.png");
    private static final long MAX_IMPORT_BYTES = 32L * 1024L * 1024L;
    private static final long MAX_IMPORT_PIXELS = 40_000_000L;
    /** 背景模式切换时的交叉淡化时长（秒），避免生硬跳变。 */
    private static final float SWITCH_FADE_SECONDS = 0.42F;
    /** 烘焙出的帧宽与JPEG 质量：1080p 源在 960 宽时约 48KB/帧，整段约 51MB。 */
    private static final int FRAME_WIDTH = 960;
    private static final float FRAME_QUALITY = 0.82F;

    private static Image customMainMenuBackground;
    private static boolean backgroundStateLoaded;
    private static volatile Mode mode = Mode.BUILTIN_IMAGE;
    private static volatile VideoBackgroundPlayer videoPlayer;

    /**
     * 后台线程（视频烘焙）请求切换到的模式。
     *
     * <p>烘焙结束是在工作线程上，直接改 {@link #mode} / 开视频播放器会与渲染线程抢 Skija 状态，
     * 所以后台只登记请求，真正的切换由渲染线程在 {@link #drawMainMenu} 里消费。
     */
    private static volatile Mode pendingMode;

    /** 上一帧正在显示的背景，用于切换时的交叉淡化。 */
    private static Image previousImage;
    private static VideoBackgroundPlayer previousPlayer;
    private static float previousBlend;
    private static float switchStartedAt;

    private ScreenBackdrop() {
    }

    /** 菜单背景来源。 */
    enum Mode {
        BUILTIN_IMAGE("Default image"),
        BUILTIN_VIDEO("Default video"),
        CUSTOM_IMAGE("Custom image"),
        CUSTOM_VIDEO("Custom video"),
        GRID("Animated grid");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        /** 界面上显示的名字（走语言文件，缺键时回退英文）。 */
        Component displayName() {
            return Component.translatableWithFallback(
                    "DioxideLite.options_screen.mode." + name().toLowerCase(Locale.ROOT), label);
        }

        static Mode byName(String name, Mode fallback) {
            for (Mode candidate : values()) {
                if (candidate.name().equalsIgnoreCase(name)) return candidate;
            }
            return fallback;
        }
    }

    static void draw(Canvas canvas, float width, float height, int shadeAlpha) {
        draw(canvas, width, height, seconds(), width * 0.5F, height * 0.5F, shadeAlpha);
    }

    static void drawMainMenu(Canvas canvas, float width, float height, int shadeAlpha) {
        drawMainMenu(canvas, width, height, seconds(), width * 0.5F, height * 0.5F,
                shadeAlpha);
    }

    static void drawMainMenu(Canvas canvas, float width, float height, float time,
                             float pointerX, float pointerY, int shadeAlpha) {
        ensureBackgroundStateLoaded();
        consumePendingMode();
        if (mode == Mode.GRID) {
            draw(canvas, width, height, time, pointerX, pointerY, 18);
            return;
        }

        float fade = switchProgress(time);
        VideoBackgroundPlayer player = videoPlayer;
        VideoBackgroundPlayer.Frame frame = player != null && player.isUsable()
                ? player.current(time) : VideoBackgroundPlayer.Frame.EMPTY;

        if (frame.isEmpty()) {
            // 视频还没就绪：继续用上一帧画面顶着，不闪黑
            Image still = currentStill();
            if (still == null) {
                draw(canvas, width, height, shadeAlpha);
                return;
            }
            drawImageCover(canvas, still, null, 0.0F, width, height);
            applyShade(canvas, width, height, shadeAlpha);
            return;
        }

        // 过渡期间：旧背景在下，新背景（视频当前帧）按进度淡入
        drawImageCover(canvas, previousImage, null, 0.0F, width, height);
        drawImageCover(canvas, frame.image(), frame.hasBlend() ? frame.blendImage() : null,
                frame.hasBlend() ? frame.blendWeight() : 1.0F - fade, width, height);
        applyShade(canvas, width, height, shadeAlpha);

        if (fade >= 1.0F) {
            clearPrevious();
        }
    }

    /** 当前应当显示的静态图（不含视频帧）。 */
    private static Image currentStill() {
        return switch (mode) {
            case CUSTOM_IMAGE -> customMainMenuBackground != null
                    ? customMainMenuBackground : MAIN_MENU_BACKGROUND;
            default -> MAIN_MENU_BACKGROUND;
        };
    }

    /** 切换淡化的进度：0=完全还是旧背景，1=过渡完成。 */
    private static float switchProgress(float time) {
        if (previousImage == null || switchStartedAt <= 0.0F) return 1.0F;
        float elapsed = time - switchStartedAt;
        if (elapsed >= SWITCH_FADE_SECONDS) return 1.0F;
        if (elapsed <= 0.0F) return 0.0F;
        return elapsed / SWITCH_FADE_SECONDS;
    }

    private static void clearPrevious() {
        previousImage = null;
        previousPlayer = null;
        previousBlend = 0.0F;
        switchStartedAt = 0.0F;
    }

    /**
     * 把图片按 cover 方式铺满视口。
     *
     * @param blend  第二张图（非空时叠加在第一张之上）
     * @param weight 第二张图的透明度
     */
    private static void drawImageCover(Canvas canvas, Image image, Image blend, float weight,
                                       float width, float height) {
        if (width <= 0.0F || height <= 0.0F) return;
        if (image != null) drawCovered(canvas, image, width, height, IMAGE_PAINT, 1.0F);
        if (blend != null) {
            float alpha = Math.max(0.0F, Math.min(1.0F, weight));
            if (alpha > 0.0F) drawCovered(canvas, blend, width, height, BLEND_PAINT, alpha);
        }
    }

    private static void drawCovered(Canvas canvas, Image image, float width, float height,
                                    Paint paint, float alpha) {
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        if (imageWidth <= 0.0F || imageHeight <= 0.0F) return;
        float viewportAspect = width / height;
        float imageAspect = imageWidth / imageHeight;
        float sourceWidth = imageWidth;
        float sourceHeight = imageHeight;
        if (viewportAspect > imageAspect) {
            sourceHeight = imageWidth / viewportAspect;
        } else {
            sourceWidth = imageHeight * viewportAspect;
        }
        float sourceX = (imageWidth - sourceWidth) * 0.5F;
        float sourceY = (imageHeight - sourceHeight) * 0.5F;
        paint.setAlpha(Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, alpha))));
        canvas.drawImageRect(image,
                Rect.makeXYWH(sourceX, sourceY, sourceWidth, sourceHeight),
                Rect.makeXYWH(0.0F, 0.0F, width, height),
                SamplingMode.LINEAR, paint, true);
    }

    static boolean canResetMainMenuBackground() {
        ensureBackgroundStateLoaded();
        return mode != Mode.BUILTIN_IMAGE;
    }

    static Mode mode() {
        ensureBackgroundStateLoaded();
        // 后台刚请求但还没被渲染线程消费时，按「将要生效的模式」回答，
        // 否则连点循环按钮会算出错误的下一档。
        Mode requested = pendingMode;
        return requested != null ? requested : mode;
    }

    /**
     * 请求切换背景模式，任意线程可调。
     *
     * <p>真正的切换发生在此后的某个渲染帧里（见 {@link #consumePendingMode()}）。
     */
    static void requestMode(Mode next) {
        if (next == null) return;
        pendingMode = next;
    }

    /** 在渲染线程消费挂起的切换请求。 */
    private static void consumePendingMode() {
        Mode next = pendingMode;
        if (next == null) return;
        pendingMode = null;
        applyMode(next);
    }

    /**
     * 这一档现在切过去能不能看出变化。
     *
     * <p>「自定义图片 / 自定义视频」在没有导入素材时会回退成内置图，
     * 界面上一模一样 —— 循环切换要跳过它们，否则用户点一下会觉得「没反应」。
     */
    static boolean isModeAvailable(Mode candidate) {
        ensureBackgroundStateLoaded();
        return switch (candidate) {
            case CUSTOM_IMAGE -> customMainMenuBackground != null;
            case CUSTOM_VIDEO -> hasCachedFrames(customVideoFrameDirectory());
            // 内置视频首次需要烘焙，但切过去会自己触发，所以算可用。
            default -> true;
        };
    }

    /** 内置视频资源（打包在 jar 里，首次使用时烘焙成帧序列）。 */
    static final String BUILTIN_VIDEO_RESOURCE =
            "/assets/dioxide-lite/textures/mainmenu/dc2989d7904191e44e8408fe2e34e5be.mp4";

    /** 切换背景模式；视频类模式在帧序列未就绪时会回退到静态图。 */
    static void applyMode(Mode next) {
        ensureBackgroundStateLoaded();
        if (next == null) return;
        // 先落盘：即使这一档和当前相同（比如从旧版 grid 标记迁移过来）也要记住选择。
        saveMode(next);
        if (next == mode) return;
        Mode previous = mode;
        beginSwitch();
        mode = next;
        if (next != Mode.CUSTOM_VIDEO) {
            closeCustomVideo();
        }
        //注意：customMainMenuBackground 不在这里清理，来回切换时不必重新解码 PNG，
        //currentStill() 会按当前 mode 决定用它还是内置图。
        if (previous == Mode.GRID && next != Mode.GRID) {
            previousImage = MAIN_MENU_BACKGROUND;
        }
        if (next == Mode.BUILTIN_VIDEO || next == Mode.CUSTOM_VIDEO) {
            startVideoIfPossible();
        }
        backgroundStateLoaded = true;
    }

    /** 导入自定义图片背景。 */
    static void importMainMenuBackground(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("The selected image does not exist.");
        }
        long byteCount = Files.size(source);
        if (byteCount <= 0L || byteCount > MAX_IMPORT_BYTES) {
            throw new IOException("The image must be smaller than 32 MB.");
        }

        BufferedImage decoded = decodeImport(source);
        Path target = customBackgroundPath();
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            if (!ImageIO.write(decoded, "png", temporary.toFile())) {
                throw new IOException("The image could not be converted to PNG.");
            }
            Image replacement = loadImage(temporary);
            if (replacement == null) {
                throw new IOException("The converted image could not be loaded.");
            }
            try {
                moveReplacing(temporary, target);
            } catch (IOException exception) {
                replacement.close();
                throw exception;
            }
            replaceCustomBackground(replacement);
            applyMode(Mode.CUSTOM_IMAGE);
        } finally {
            Files.deleteIfExists(temporary);
            decoded.flush();
        }
    }

    /** 导入自定义视频背景：复制到配置目录并烘焙成帧序列。 */
    static VideoBackgroundBaker.Manifest importMainMenuVideo(Path source,
                                                             VideoBackgroundBaker.Progress progress)
            throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("The selected video does not exist.");
        }
        long byteCount = Files.size(source);
        if (byteCount <= 0L) {
            throw new IOException("The selected video is empty.");
        }

        Path directory = backgroundDirectory();
        Files.createDirectories(directory);
        Path video = directory.resolve("menu-background.mp4");
        Path temporary = video.resolveSibling(video.getFileName() + ".tmp");
        Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
        moveReplacing(temporary, video);

        Path frames = customVideoFrameDirectory();
        VideoBackgroundBaker.discard(frames);
        VideoBackgroundBaker.Manifest manifest =
                VideoBackgroundBaker.bake(video, frames, FRAME_WIDTH, FRAME_QUALITY, progress);
        // 这里跑在烘焙线程上：只登记请求，由渲染线程真正切过去。
        requestMode(Mode.CUSTOM_VIDEO);
        return manifest;
    }

    /** 恢复默认背景（内置图片）。自定义素材保留在磁盘上，方便再切回来。 */
    static void resetMainMenuBackground() {
        // 清掉旧版本用来记 grid 的标记文件，否则下次启动会把它又当成当前模式。
        try {
            Files.deleteIfExists(gridBackgroundMarker());
        } catch (IOException exception) {
            DioxideLite.LOGGER.warn("Unable to delete the legacy grid background marker", exception);
        }
        requestMode(Mode.BUILTIN_IMAGE);
    }

    /**
 * 把打包在 jar 里的内置视频烘焙成帧序列（首次使用时触发）。
 * 资源本身读不出 JCodec 需要的随机访问通道，所以先落到磁盘。
 */
static VideoBackgroundBaker.Manifest bakeBuiltinVideo(VideoBackgroundBaker.Progress progress)
            throws IOException {
        Path directory = backgroundDirectory();
        Files.createDirectories(directory);
        Path video = directory.resolve("builtin-video.mp4");
        if (!Files.isRegularFile(video) || Files.size(video) <= 0L) {
            try (InputStream stream = ScreenBackdrop.class.getResourceAsStream(
                    BUILTIN_VIDEO_RESOURCE)) {
                if (stream == null) {
                    throw new IOException("The bundled background video is missing from the jar.");
                }
                Path temporary = video.resolveSibling(video.getFileName() + ".tmp");
                Files.write(temporary, stream.readAllBytes());
                moveReplacing(temporary, video);
            }
        }
        Path frames = builtinVideoFrameDirectory();
        VideoBackgroundBaker.Manifest manifest =
                VideoBackgroundBaker.loadCached(frames, 0);
        if (manifest == null) {
            manifest = VideoBackgroundBaker.bake(video, frames, FRAME_WIDTH, FRAME_QUALITY,
                    progress);
        }
        // 烘焙线程上只登记请求 —— 之前这里什么都不做，导致「切到内置视频」这一档永远不生效。
        requestMode(Mode.BUILTIN_VIDEO);
        return manifest;
    }

/** 记录切换开始时刻，并把当前画面留作淡出底图。 */
    private static void beginSwitch() {
        previousImage = currentStill();
        previousBlend = 0.0F;
        switchStartedAt = seconds();
    }

    private static void closeCustomVideo() {
        if (videoPlayer != null) {
            videoPlayer.close();
            videoPlayer = null;
        }
    }

    /** 打开当前模式对应的视频（缓存已烘焙时立即可用）。 */
    private static void startVideoIfPossible() {
        Path frames = mode == Mode.CUSTOM_VIDEO
                ? customVideoFrameDirectory() : builtinVideoFrameDirectory();
        VideoBackgroundBaker.Manifest manifest =
                VideoBackgroundBaker.loadCached(frames, 0);
        if (manifest == null) return;
        closeCustomVideo();
        videoPlayer = VideoBackgroundPlayer.open(frames, manifest);
    }

    static void draw(Canvas canvas, float width, float height, float time,
                     float pointerX, float pointerY, int shadeAlpha) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        drawGradient(canvas, Rect.makeXYWH(0.0F, 0.0F, width, height),
                0.0F, 0.0F, width, height,
                new int[]{0xFF06070A, 0xFF10141A, 0xFF090A0E},
                new float[]{0.0F, 0.58F, 1.0F});

        float parallaxX = clamp((pointerX / width - 0.5F) * 10.0F, -5.0F, 5.0F);
        float parallaxY = clamp((pointerY / height - 0.5F) * 8.0F, -4.0F, 4.0F);
        drawPerspectivePlane(canvas, width, height, time, parallaxX, parallaxY);
        drawGrid(canvas, width, height, time, parallaxX, parallaxY);
        drawScan(canvas, width, height, time);

        if (shadeAlpha > 0) {
            canvas.drawColor(UiTheme.argb(Math.min(255, shadeAlpha), 4, 6, 9));
        }
        drawEdgeShade(canvas, width, height);
    }

    private static void drawPerspectivePlane(Canvas canvas, float width, float height, float time,
                                             float parallaxX, float parallaxY) {
        int accent = UiTheme.accent();
        float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.55F);

        canvas.save();
        canvas.translate(width * 0.73F + parallaxX, height * 0.46F + parallaxY);
        canvas.rotate(-13.0F);
        float planeWidth = Math.max(120.0F, width * 0.22F);
        float planeHeight = height * 2.1F;
        drawGradient(canvas, Rect.makeXYWH(-planeWidth * 0.5F, -planeHeight * 0.5F,
                        planeWidth, planeHeight),
                -planeWidth * 0.5F, 0.0F, planeWidth * 0.5F, 0.0F,
                new int[]{UiTheme.withAlpha(accent, 0), UiTheme.withAlpha(accent, 12 + Math.round(pulse * 9.0F)),
                        UiTheme.withAlpha(0xFFF1A45D, 8), UiTheme.withAlpha(accent, 0)},
                new float[]{0.0F, 0.28F, 0.72F, 1.0F});
        canvas.restore();

        TraceLine trace = traceLine(width, height, parallaxX);
        LINE_PAINT.setColor(UiTheme.withAlpha(0xFFF1A45D, 38)).setStrokeWidth(1.0F);
        canvas.drawLine(trace.startX(), trace.startY(), trace.endX(), trace.endY(), LINE_PAINT);
    }

    static TraceLine traceLine(float width, float height, float parallaxX) {
        float startX = width * 0.82F + parallaxX * 0.7F;
        return new TraceLine(startX, height * 0.12F,
                startX - height * 0.22F, height * 0.88F);
    }

    private static void drawGrid(Canvas canvas, float width, float height, float time,
                                 float parallaxX, float parallaxY) {
        float spacing = Math.max(34.0F, Math.min(58.0F, width / 12.0F));
        float offsetX = positiveModulo(time * 3.5F + parallaxX, spacing);
        float offsetY = positiveModulo(time * 2.0F + parallaxY, spacing);
        LINE_PAINT.setColor(0x0EFFFFFF).setStrokeWidth(1.0F);
        for (float x = -spacing + offsetX; x < width + spacing; x += spacing) {
            canvas.drawLine(x, 0.0F, x, height, LINE_PAINT);
        }
        for (float y = -spacing + offsetY; y < height + spacing; y += spacing) {
            canvas.drawLine(0.0F, y, width, y, LINE_PAINT);
        }

        LINE_PAINT.setColor(0x18FFFFFF).setStrokeWidth(1.0F);
        canvas.drawLine(width * 0.08F, 0.0F, width * 0.08F, height, LINE_PAINT);
        canvas.drawLine(width * 0.92F, 0.0F, width * 0.92F, height, LINE_PAINT);
    }

    private static void drawScan(Canvas canvas, float width, float height, float time) {
        float travel = height + 120.0F;
        float y = positiveModulo(time * 23.0F, travel) - 60.0F;
        int accent = UiTheme.accent();
        drawGradient(canvas, Rect.makeXYWH(0.0F, y - 38.0F, width, 76.0F),
                0.0F, y - 38.0F, 0.0F, y + 38.0F,
                new int[]{UiTheme.withAlpha(accent, 0), UiTheme.withAlpha(accent, 11),
                        UiTheme.withAlpha(accent, 0)},
                new float[]{0.0F, 0.5F, 1.0F});
        LINE_PAINT.setColor(UiTheme.withAlpha(accent, 24)).setStrokeWidth(1.0F);
        canvas.drawLine(0.0F, y, width, y, LINE_PAINT);
    }

    /** 叠加整屏暗色遮罩，数值越大越暗。 */
    private static void applyShade(Canvas canvas, float width, float height, int shadeAlpha) {
        if (shadeAlpha > 0) {
            canvas.drawColor(UiTheme.argb(Math.min(255, shadeAlpha), 4, 6, 9));
        }
    }

    private static void drawEdgeShade(Canvas canvas, float width, float height) {
        float edge = Math.min(150.0F, width * 0.24F);
        drawGradient(canvas, Rect.makeXYWH(0.0F, 0.0F, edge, height),
                0.0F, 0.0F, edge, 0.0F,
                new int[]{0x8C020306, 0x00020306});
        drawGradient(canvas, Rect.makeXYWH(width - edge, 0.0F, edge, height),
                width, 0.0F, width - edge, 0.0F,
                new int[]{0x76020306, 0x00020306});
        float vertical = Math.min(100.0F, height * 0.25F);
        drawGradient(canvas, Rect.makeXYWH(0.0F, height - vertical, width, vertical),
                0.0F, height, 0.0F, height - vertical,
                new int[]{0x76020306, 0x00020306});
    }

    private static void drawGradient(Canvas canvas, Rect bounds, float x0, float y0, float x1, float y1,
                                     int[] colors) {
        drawGradient(canvas, bounds, x0, y0, x1, y1, colors, null);
    }

    private static void drawGradient(Canvas canvas, Rect bounds, float x0, float y0, float x1, float y1,
                                     int[] colors, float[] positions) {
        try (Shader shader = positions == null
                ? Shader.makeLinearGradient(x0, y0, x1, y1, colors)
                : Shader.makeLinearGradient(x0, y0, x1, y1, colors, positions)) {
            GRADIENT_PAINT.setShader(shader);
            canvas.drawRect(bounds, GRADIENT_PAINT);
            GRADIENT_PAINT.setShader(null);
        }
    }

    private static float seconds() {
        return (System.nanoTime() & 0x1FFFFFFFFFFFFFL) / 1_000_000_000.0F;
    }

    private static Image loadImage(String resource) {
        try (InputStream stream = ScreenBackdrop.class.getResourceAsStream(resource)) {
            return stream == null ? null : Image.makeFromEncoded(stream.readAllBytes());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Image loadImage(Path file) {
        try {
            return Files.isRegularFile(file) ? Image.makeFromEncoded(Files.readAllBytes(file)) : null;
        } catch (Exception exception) {
            DioxideLite.LOGGER.warn("Unable to load custom menu background {}", file, exception);
            return null;
        }
    }

    private static BufferedImage decodeImport(Path source) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(source.toFile())) {
            if (stream == null) {
                throw new IOException("The selected file is not a supported image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IOException("Use a PNG, JPG, BMP, or GIF image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int imageWidth = reader.getWidth(0);
                int imageHeight = reader.getHeight(0);
                long pixels = (long) imageWidth * imageHeight;
                if (imageWidth <= 0 || imageHeight <= 0 || pixels > MAX_IMPORT_PIXELS) {
                    throw new IOException("The image resolution is too large.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException("The selected image could not be decoded.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private static Image mainMenuBackground() {
        ensureBackgroundStateLoaded();
        if (mode == Mode.CUSTOM_IMAGE && customMainMenuBackground != null) {
            return customMainMenuBackground;
        }
        return MAIN_MENU_BACKGROUND;
    }

    private static void ensureBackgroundStateLoaded() {
        if (backgroundStateLoaded) {
            return;
        }
        Mode requested = loadSavedMode();
        if (requested == null) {
            // 兼容旧版本：以前「reset」会写一个 grid 标记文件，没有存档记录时以它为准。
            requested = Files.isRegularFile(gridBackgroundMarker())
                    ? Mode.GRID : Mode.BUILTIN_IMAGE;
        }
        mode = requested;
        customMainMenuBackground = loadImage(customBackgroundPath());
        // 之前选的模式可能已经没有素材了（图删了 / 帧缓存没了），回退到默认图片，
        // 否则开屏会是一片空白，看着就像「切了没反应」。
        if (mode == Mode.CUSTOM_IMAGE && customMainMenuBackground == null) {
            mode = Mode.BUILTIN_IMAGE;
        }
        if (mode == Mode.CUSTOM_VIDEO && !hasCachedFrames(customVideoFrameDirectory())) {
            mode = customMainMenuBackground != null ? Mode.CUSTOM_IMAGE : Mode.BUILTIN_IMAGE;
        }
        if (mode == Mode.BUILTIN_VIDEO && !hasCachedFrames(builtinVideoFrameDirectory())) {
            mode = Mode.BUILTIN_IMAGE;
        }
        if (mode != requested) {
            DioxideLite.LOGGER.info("Menu background mode {} has no assets left, using {}",
                    requested, mode);
        }
        DioxideLite.LOGGER.info("Menu background mode: {}", mode);
        backgroundStateLoaded = true;
        if (mode == Mode.BUILTIN_VIDEO || mode == Mode.CUSTOM_VIDEO) {
            startVideoIfPossible();
        }
    }

    private static boolean hasCachedFrames(Path directory) {
        return VideoBackgroundBaker.loadCached(directory, 0) != null;
    }

    /** 读取上次选择的模式；没有记录（首次使用 / 文件损坏）时返回 {@code null}。 */
    private static Mode loadSavedMode() {
        Path file = modeFilePath();
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Mode.byName(Files.readString(file).trim(), null);
        } catch (IOException exception) {
            DioxideLite.LOGGER.warn("Unable to read the saved menu background mode", exception);
            return null;
        }
    }

    private static void saveMode(Mode selected) {
        try {
            Path file = modeFilePath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, selected.name());
        } catch (IOException | RuntimeException exception) {
            DioxideLite.LOGGER.warn("Unable to save the menu background mode", exception);
        }
    }

    private static void replaceCustomBackground(Image replacement) {
        Image previous = customMainMenuBackground;
        customMainMenuBackground = replacement;
        if (previous != null && previous != replacement) {
            previous.close();
        }
    }

    private static Path customBackgroundPath() {
        return backgroundDirectory()
                .resolve("menu-background.png");
    }

    private static Path gridBackgroundMarker() {
        return backgroundDirectory()
                .resolve("use-grid-background");
    }

    /** 上次选择的背景模式（明文枚举名）。 */
    private static Path modeFilePath() {
        return backgroundDirectory()
                .resolve("menu-background-mode.txt");
    }

    private static Path customVideoFrameDirectory() {
        return backgroundDirectory().resolve("menu-video-frames");
    }

    private static Path builtinVideoFrameDirectory() {
        return backgroundDirectory().resolve("builtin-video-frames");
    }

    private static Path backgroundDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve(".DioxideLite")
                .resolve("ui");
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0.0F ? result + modulus : result;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    record TraceLine(float startX, float startY, float endX, float endY) {
    }
}
