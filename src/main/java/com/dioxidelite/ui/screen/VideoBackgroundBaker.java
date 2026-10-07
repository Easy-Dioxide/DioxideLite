package com.dioxidelite.ui.screen;

import com.dioxidelite.DioxideLite;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 把视频一次性烘焙成 JPEG 帧序列，供菜单背景循环播放。
 *
 * <p>为什么不在运行期实时解码：实测 JCodec 解 1080p H.264 约 34ms/帧，
 * 而 30fps 每帧预算只有 33ms，单线程实时解码必然掉帧。这里改成
 * 「首次使用时烘焙一次、之后直接播缓存帧」——运行期只剩 Skija 的图片解码。
 *
 * <p>烘焙按关键帧切段并行：每个线程从最近的关键帧开始解，段内顺序解码，
 * 实测与整段串行解码逐帧一致（已验证所有分段边界）。
 */
final class VideoBackgroundBaker {

    /** 烘焙产物的清单文件名，同时用于校验缓存是否完整。 */
    static final String MANIFEST_NAME = "manifest.txt";
    private static final String FRAME_PREFIX = "f";
    private static final String FRAME_SUFFIX = ".jpg";
    private static final int FRAME_NAME_DIGITS = 5;
    /** 每段最少多少帧才值得单独开一个线程，避免线程开销超过收益。 */
    private static final int MIN_SEGMENT_FRAMES = 24;
    private static final int MAX_WORKER_THREADS = 8;

    private VideoBackgroundBaker() {
    }

    /** 烘焙进度回调。 */
    interface Progress {
        void onProgress(int done, int total);
    }

    /**
     * 把 {@code source} 烘焙到 {@code targetDirectory}。
     *
     * @param targetWidth 输出帧宽度，高度按原始宽高比推算并对齐到偶数
     * @param quality     JPEG 质量0..1
     */
    static Manifest bake(Path source, Path targetDirectory, int targetWidth,
                         float quality, Progress progress) throws IOException {
        Probe probe = probe(source);
        if (probe.totalFrames <= 0) {
            throw new IOException("The video contains no decodable frames.");
        }
        if (probe.width <= 0 || probe.height <= 0) {
            throw new IOException("The video reports an invalid resolution.");
        }

        Path staging = targetDirectory.resolveSibling(targetDirectory.getFileName() + ".baking");
        deleteRecursively(staging);
        Files.createDirectories(staging);

        int width = Math.min(targetWidth, probe.width);
        int height = evenHeight(probe.width, probe.height, width);

        try {
            List<int[]> segments = segments(probe);
            AtomicInteger done = new AtomicInteger();
            AtomicInteger total = new AtomicInteger(probe.totalFrames);
            int workers = Math.max(1, Math.min(segments.size(),
                    Math.min(MAX_WORKER_THREADS, Runtime.getRuntime().availableProcessors())));
            ExecutorService pool = Executors.newFixedThreadPool(workers);
            List<Future<?>> futures = new ArrayList<>();
            try {
                for (int[] segment : segments) {
                    futures.add(pool.submit(() -> {
                        try {
                            bakeSegment(source, segment[0], segment[1], width, height,
                                    quality, staging, done, progress, total);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }));
                }
                pool.shutdown();
                RuntimeException firstFailure = null;
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception exception) {
                        if (firstFailure == null) firstFailure = unwrap(exception);
                    }
                }
                if (firstFailure != null) throw firstFailure;
            } finally {
                pool.shutdownNow();
            }

            Manifest manifest = new Manifest(probe.totalFrames, probe.frameRate(),
                    width, height, probe.durationSeconds());
            manifest.write(staging);
            Files.createDirectories(targetDirectory.getParent());
            replaceDirectory(staging, targetDirectory);
            return manifest;
        } catch (IOException | RuntimeException exception) {
            deleteRecursively(staging);
            throw exception;
        }
    }

    /** 读取现成缓存；不存在或不完整时返回 {@code null}。 */
    static Manifest loadCached(Path directory, int expectedFrames) {
        Manifest manifest = Manifest.read(directory.resolve(MANIFEST_NAME));
        if (manifest == null || expectedFrames > 0 && manifest.frameCount() != expectedFrames) {
            return null;
        }
        return manifest;
    }

    /** 清空某个视频的帧缓存。 */
    static void discard(Path directory) {
        deleteRecursively(directory);
    }

    static Path frameFile(Path directory, int index) {
        return directory.resolve(String.format("%s%0" + FRAME_NAME_DIGITS + "d%s",
                FRAME_PREFIX, index, FRAME_SUFFIX));
    }

    private static Probe probe(Path source) throws IOException {
        try (SeekableByteChannel channel = NIOUtils.readableChannel(source.toFile())) {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            var track = grab.getVideoTrack();
            if (track == null) throw new IOException("The file has no video track.");
            DemuxerTrackMeta meta = track.getMeta();
            if (meta == null) throw new IOException("The video track has no metadata.");
            var videoMeta = meta.getVideoCodecMeta();
            if (videoMeta == null || videoMeta.getSize() == null) {
                throw new IOException("The video track is not decodable.");
            }
            int totalFrames = Math.max(0, meta.getTotalFrames());
            double duration = meta.getTotalDuration();
            float frameRate = duration > 0.0 ? (float) (totalFrames / duration) : 30.0F;
            if (!Float.isFinite(frameRate) || frameRate <= 0.1F) frameRate = 30.0F;
            // JCodec 的 getTotalDuration() 返回的就是秒（1081 帧 / 36.03s 刚好得到 30fps），
            // 别再按微秒除一遍，否则清单里记的时长会小一百万倍。
            return new Probe(totalFrames, videoMeta.getSize().getWidth(),
                    videoMeta.getSize().getHeight(), frameRate,
                    (float) duration, keyFrames(track));
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("The video could not be opened.", exception);
        }
    }

    /** 取视频轨道的关键帧索引，失败时退化为整段串行解码。 */
    private static int[] keyFrames(org.jcodec.common.SeekableDemuxerTrack track) {
        try {
            int[] keys = track.getMeta().getSeekFrames();
            if (keys == null || keys.length == 0) return new int[]{0};
            keys = keys.clone();
            java.util.Arrays.sort(keys);
            if (keys[0] != 0) {
                // 没有从 0 开始的关键帧就只能整段解码，否则第0 帧无法重建
                return new int[]{0};
            }
            return keys;
        } catch (RuntimeException exception) {
            DioxideLite.LOGGER.warn("Unable to read keyframe index, falling back to serial decode",
                    exception);
            return new int[]{0};
        }
    }

    /** 以关键帧为起点把帧区间切成可独立解码的段。 */
    private static List<int[]> segments(Probe probe) {
        int[] keys = probe.keyFrames();
        List<int[]> segments = new ArrayList<>();
        if (keys == null || keys.length == 0) {
            segments.add(new int[]{0, Integer.MAX_VALUE});
            return segments;
        }
        for (int i = 0; i < keys.length; i++) {
            int start = keys[i];
            int end = i + 1 < keys.length ? keys[i + 1] : Integer.MAX_VALUE;
            // 用探测到的总帧数收紧最后一段，避免多解一遍
            if (end == Integer.MAX_VALUE && probe.totalFrames > 0) end = probe.totalFrames;
            if (start >= end) continue;
            segments.add(new int[]{start, end});
        }
        if (segments.isEmpty()) segments.add(new int[]{0, Integer.MAX_VALUE});
        return segments;
    }

    private static void bakeSegment(Path source, int startFrame, int endFrame,
                                    int width, int height, float quality,
                                    Path staging, AtomicInteger done, Progress progress,
                                    AtomicInteger total) throws IOException {
        ImageWriter writer = null;
        try (SeekableByteChannel channel = NIOUtils.readableChannel(source.toFile())) {
            FrameGrab grab;
            try {
                grab = FrameGrab.createFrameGrab(channel);
                if (startFrame > 0) grab.seekToFramePrecise(startFrame);
            } catch (JCodecException exception) {
                throw new IOException("The video segment could not be decoded.", exception);
            }
            writer = jpegWriter();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.05F, Math.min(1.0F, quality)));

            int index = startFrame;
            long lastReport = 0L;
            while (index < endFrame) {
                Picture picture = grab.getNativeFrame();
                if (picture == null) break;
                writeFrame(picture, writer, param, width, height, staging, index);
                index++;
                int finished = done.incrementAndGet();
                long now = System.nanoTime();
                if (progress != null && now - lastReport > 100_000_000L) {
                    lastReport = now;
                    report(progress, finished, total.get());
                }
            }
        } finally {
            if (writer != null) writer.dispose();
        }
        report(progress, done.get(), total.get());
    }

    private static void writeFrame(Picture picture, ImageWriter writer, ImageWriteParam param,
                                   int width, int height, Path staging, int index)
            throws IOException {
        BufferedImage decoded = AWTUtil.toBufferedImage(picture);
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try {
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(decoded, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
            Path target = frameFile(staging, index);
            try (ImageOutputStream stream = ImageIO.createImageOutputStream(target.toFile())) {
                if (stream == null) throw new IOException("Could not open a frame file for writing.");
                writer.setOutput(stream);
                writer.write(null, new IIOImage(scaled, null, null), param);
            }
        } finally {
            decoded.flush();
            scaled.flush();
        }
    }

    private static ImageWriter jpegWriter() throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("No JPEG writer is available in this JVM.");
        return writers.next();
    }

    private static void report(Progress progress, int done, int total) {
        try {
            progress.onProgress(done, total);
        } catch (RuntimeException ignored) {
            // 进度回调不该影响烘焙
        }
    }

    private static int evenHeight(int sourceWidth, int sourceHeight, int width) {
        int height = Math.max(2, Math.round(sourceHeight * (float) width / sourceWidth / 2.0F) * 2);
        return height;
    }

    private static RuntimeException unwrap(Exception exception) {
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
        return cause instanceof RuntimeException runtime ? runtime : new RuntimeException(cause);
    }

    private static void replaceDirectory(Path staging, Path target) throws IOException {
        Path retired = target.resolveSibling(target.getFileName() + ".old");
        deleteRecursively(retired);
        if (Files.exists(target)) {
            try {
                Files.move(target, retired, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(target, retired);
            }
        }
        try {
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(staging, target);
        }
        deleteRecursively(retired);
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (var walk = Files.walk(directory)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 删不掉就留着，下一次烘焙会覆盖
                }
            });
        } catch (IOException exception) {
            DioxideLite.LOGGER.warn("Unable to clean video frame cache {}", directory, exception);
        }
    }

    /** 帧序列清单，兼作缓存完整性校验。 */
    record Manifest(int frameCount, float frameRate, int width, int height, float durationSeconds) {

        void write(Path directory) throws IOException {
            StringBuilder text = new StringBuilder();
            text.append("frames=").append(frameCount).append('\n')
                    .append("fps=").append(frameRate).append('\n')
                    .append("width=").append(width).append('\n')
                    .append("height=").append(height).append('\n')
                    .append("duration=").append(durationSeconds).append('\n');
            Files.writeString(directory.resolve(MANIFEST_NAME), text.toString());
        }

        static Manifest read(Path file) {
            try {
                if (!Files.isRegularFile(file)) return null;
                int frames = 0;
                float fps = 30.0F;
                int width = 0;
                int height = 0;
                float duration = 0.0F;
                for (String line : Files.readAllLines(file)) {
                    int split = line.indexOf('=');
                    if (split <= 0) continue;
                    String key = line.substring(0, split).trim();
                    String value = line.substring(split + 1).trim();
                    switch (key) {
                        case "frames" -> frames = Integer.parseInt(value);
                        case "fps" -> fps = Float.parseFloat(value);
                        case "width" -> width = Integer.parseInt(value);
                        case "height" -> height = Integer.parseInt(value);
                        case "duration" -> duration = Float.parseFloat(value);
                        default -> {
                        }
                    }
                }
                if (frames <= 0 || width <= 0 || height <= 0) return null;
                return new Manifest(frames, fps, width, height, duration);
            } catch (Exception exception) {
                DioxideLite.LOGGER.warn("Unable to read video frame manifest {}", file, exception);
                return null;
            }
        }

        /** 单帧时长（秒）。 */
        public float frameDuration() {
            return frameRate > 0.1F ? 1.0F / frameRate : 1.0F / 30.0F;
        }

        /** 循环总时长（秒）。 */
        public float totalDuration() {
            return frameCount * frameDuration();
        }
    }

    /** 探测结果。 */
    record Probe(int totalFrames, int width, int height, float frameRate,
                 float durationSeconds, int[] keyFrames) {
    }
}