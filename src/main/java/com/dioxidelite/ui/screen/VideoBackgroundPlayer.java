package com.dioxidelite.ui.screen;

import com.dioxidelite.DioxideLite;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

/**
 * 菜单视频背景播放器：按时间轴从烘焙好的帧序列里取帧。
 *
 * <p>设计要点：
 * <ul>
 *   <li>JPEG 解码放在后台线程，主线程只做「取当前帧 + 画」，不阻塞渲染。
 *   <li>环形缓冲预取若干帧；播放器只持有 {@link #RING} 张已解码图，内存与视频长度无关。
 *   <li>循环回卷时返回「尾帧 + 首帧 + 混合权重」，由调用方在画布上用 alpha 叠加，
 *       消除接缝跳变（实测该视频首尾帧差 1.62/255，硬切会有可见跳动）。
 * </ul>
 */
final class VideoBackgroundPlayer {

    /** 循环接缝处交叉淡化的时长（秒）。 */
    private static final float SEAM_BLEND_SECONDS = 0.45F;
    /** 环形缓冲深度：太浅会漏帧，太深白占内存（每帧 width*height*4 字节）。 */
    private static final int RING = 24;
    /** 后台线程连续找不到帧文件多少次就判定缓存损坏。 */
    private static final int MAX_CONSECUTIVE_MISSES = 12;

    private final Path directory;
    private final VideoBackgroundBaker.Manifest manifest;
    private final int frameCount;
    private final float frameDuration;
    private final int seamFrames;
    /** 预取窗口深度：保证「播放位置起 window 帧」都在缓冲里。 */
    private final int window;
    /** 与 bake 一致的分辨率，用于创建可复用的 raster surface（V1）。 */
    private final int surfaceWidth;
    private final int surfaceHeight;

    private final Slot[] ring;
    private Thread worker;
    private volatile boolean running;
    private volatile boolean cacheBroken;
    /** 渲染线程最近一次的播放位置，预取线程据此往前推。 */
    private volatile int playhead;
    /** 上次成功解码的帧索引（V4：冷启动兜底，避免 Frame.EMPTY 闪白）。 */
    private volatile int lastValidIndex = -1;

    /**
     * 被挤出缓冲、但渲染线程可能还在画的图。
     *
     * <p>直接 close 会让渲染线程拿着一张已释放的 Image（Skija 会抛
     * IllegalStateException），所以先挂进队列，等渲染线程又画完一帧再真正释放。
     */
    private final ArrayDeque<Image> retired = new ArrayDeque<>();
    private final ArrayDeque<Image> pendingRelease = new ArrayDeque<>();

    /** 时间轴：按真实流逝时间累积出来的整帧计数器（见 {@link #advanceTimeline}）。 */
    private float frameAccumulator;
    private int frameIndex;
    private float lastCallSeconds;
    private boolean callInitialized;

    private VideoBackgroundPlayer(Path directory, VideoBackgroundBaker.Manifest manifest) {
        this.directory = directory;
        this.manifest = manifest;
        this.frameCount = manifest.frameCount();
        this.frameDuration = Math.max(1.0F / 60.0F, manifest.frameDuration());
        int frames = Math.round(SEAM_BLEND_SECONDS / Math.max(1.0E-4F, frameDuration));
        this.seamFrames = Math.max(1, Math.min(frames, frameCount / 2));
        this.ring = new Slot[RING];
        this.window = Math.min(RING, frameCount);
        // V1: 与 bake 一致的分辨率，用于创建可复用 raster surface（一次性 GPU 纹理分配）。
        this.surfaceWidth = manifest.width();
        this.surfaceHeight = manifest.height();
        this.running = true;
        this.lastValidIndex = -1;
        // 同步预取开头若干帧，让首帧一定能立刻显示。
        prefetch(1);
        startWorker();
    }

    static VideoBackgroundPlayer open(Path directory, VideoBackgroundBaker.Manifest manifest) {
        if (manifest == null || manifest.frameCount() <= 0) return null;
        VideoBackgroundPlayer player = new VideoBackgroundPlayer(directory, manifest);
        return player.cacheBroken ? null : player;
    }

    VideoBackgroundBaker.Manifest manifest() {
        return manifest;
    }

    /**
     * 播放器是否可用。
     *
     * <p>注意不能写成「第0 帧在缓冲里」—— 播放头一过第 {@link #RING} 帧，第0 帧就会被挤出
     * 环形缓冲，那样会被误判成不可用，于是整段视频被静态图顶掉。
     */
    boolean isUsable() {
        return running && !cacheBroken && hasDecodedFrame();
    }

    /**
     * 取当前应显示的帧。返回的 {@link Frame} 持有两张图引用，
     * 必须在同一帧内用完再调用下一次（缓存会被后台线程复用）。
     */
    Frame current(float nowSeconds) {
        releaseRetiredImages();
        if (cacheBroken) return Frame.EMPTY;
        advanceTimeline(nowSeconds);
        int index = frameIndex;
        playhead = index;

        // V4: 若当前帧尚未就绪且首帧未初始化，使用上次成功的帧作为兜底，避免冷启动闪白。
        Slot current = readyFrameAt(index);
        if (current == null && lastValidIndex >= 0) {
            current = readyFrameAt(lastValidIndex);
        }
        if (current == null) {
            return Frame.EMPTY;
        }
        int shown = current.index;

        // 回卷前的最后几帧，与首帧交叉淡化。
        // frame 0 在缓冲里只存活 RING 帧（约 0.8s），过了第一圈后 frame 0 已被覆盖。
        // 进入 blend 区域时主动把 frame 0 重新载入缓冲，确保首尾平滑过渡。
        int toEnd = frameCount - 1 - shown;
        if (toEnd < seamFrames && frameCount > 2) {
            Slot head = readyFrameAt(0);
            if (head == null) {
                // frame 0 已离线：从磁盘重新解码并载入缓冲
                Image img = decode(0);
                if (img != null) {
                    store(0, img);
                    lastValidIndex = 0;
                    head = readyFrameAt(0);
                }
            }
            if (head != null && head.index == 0) {
                float weight = (toEnd + 1.0F) / (seamFrames + 1.0F);
                return new Frame(current.image, head.image, weight);
            }
            // frame 0 仍无法获取：继续播放当前帧，不做淡出（避免画面跳变）
        }
        return new Frame(current.image, null, 0.0F);
    }

    /**
     * 从 {@code index} 往前找最近的一个已解码帧。
     *
     * <p>解码偶尔跟不上时宁可继续显示上一帧，也不要让调用方切回静态图
     * （那正是「视频突然被默认图片挤掉」的观感来源）。
     *
     * <p>注意：搜索范围是「从 {@code index} 开始往后数 {@code window} 个不同的环槽位置」。
     * 因为环缓冲只有 {@link #RING} 个槽，当视频播放超过一圈后，较早的帧已经被新帧覆盖，
     * 按帧号线性搜索会全部 miss。改为按环槽遍历可以稳定命中当前缓冲中的任何帧。
     */
    private Slot readyFrameAt(int index) {
        if (index < 0 || index >= frameCount) return null;
        // 先检查目标帧本身
        Slot target = slot(index);
        if (target != null && target.index == index) return target;
        // 环缓冲内最多 RING 个不同帧号，逐个槽检查即可找到最近的可用帧
        for (int i = 0; i < ring.length; i++) {
            Slot s = slot(i);
            if (s != null && s.index == index) return s;
        }
        return null;
    }

    private Slot slot(int index) {
        synchronized (ring) {
            Slot slot = ring[Math.floorMod(index, ring.length)];
            return slot != null && slot.index == index ? slot : null;
        }
    }

    private boolean hasDecodedFrame() {
        synchronized (ring) {
            for (Slot slot : ring) {
                if (slot != null) return true;
            }
            return false;
        }
    }

    /**
     * 按真实流逝时间推进帧号（渲染线程每帧调一次）。
     *
     * <p>不能用「绝对时间 ÷ 帧长」直接取整：视频 30fps 碰上 60fps 渲染时两者正好 1:2，
     * 采样点会精确落在帧边界上，时间戳里毫秒级的抖动就足以让每个视频帧的保持长度
     * 在 1/2/3 之间乱跳 —— 观感就是「一顿一顿」。
     *
     * <p>这里把流逝时间累积成一个整帧计数器：只按整帧推进，所以每个视频帧的保持长度是均匀的；
     * 又因为累积的是真实时间，长期播放也不会漂移。
     */
    private void advanceTimeline(float nowSeconds) {
        if (!callInitialized) {
            callInitialized = true;
            lastCallSeconds = nowSeconds;
            frameAccumulator = 0.0F;
            frameIndex = 0;
            return;
        }
        float delta = nowSeconds - lastCallSeconds;
        lastCallSeconds = nowSeconds;
        if (delta <= 0.0F) return;

        frameAccumulator += delta / frameDuration;
        int steps = (int) frameAccumulator;
        if (steps <= 0) return;
        frameAccumulator -= steps;
        frameIndex = Math.floorMod(frameIndex + Math.min(steps, frameCount), frameCount);
    }

    private void startWorker() {
        worker = new Thread(this::pump, "DioxideLite-VideoBackground");
        worker.setDaemon(true);
        worker.setPriority(Thread.NORM_PRIORITY);
        worker.start();
    }

    /**
     * 后台预取：让「播放位置起 {@link #window} 帧」始终处于已解码状态。
     *
     * <p>不能只看「缓冲满了就睡」—— 帧被播放头用掉并不会腾出槽位，
     * 那样缓冲填满一次之后就再也不前进了，视频播完第一圈就卡住。
     */
    private void pump() {
        int misses = 0;
        while (running) {
            int target = playhead;
            int ahead = 0;
            while (ahead < window && slot(target) != null) {
                target = advance(target);
                ahead++;
            }
            if (ahead >= window) {
                // V3: 使用 wait/notify 代替固定间隔轮询，减少 CPU 空闲唤醒开销。
                synchronized (this) {
                    try {
                        wait(100);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                continue;
            }
            Image image = decode(target);
            if (image == null) {
                if (++misses >= MAX_CONSECUTIVE_MISSES) {
                    cacheBroken = true;
                    return;
                }
                try { Thread.sleep(25L); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            misses = 0;
            store(target, image);
            lastValidIndex = target;
        }
    }

    private int advance(int index) {
        return Math.floorMod(index + 1, frameCount);
    }

    /**
     * 同步预取开头若干帧，让首帧一定能立刻显示（否则进菜单会先黑一下）。
     */
    private void prefetch(int count) {
        for (int i = 0; i < Math.min(count, frameCount); i++) {
            Image image = decode(i);
            if (image == null) {
                cacheBroken = true;
                return;
            }
            store(i, image);
            lastValidIndex = i;
        }
    }

    /**
     * 把一帧放进环槽。
     *
     * <p>⚠️ 这里有两个必须守住的点，之前就是栽在这两处（症状：视频播约 1 秒后整个背景变黑，
     * 且日志里没有任何异常）：
     * <ol>
     *   <li>{@code makeImageSnapshot()} <b>不会</b>被之后的绘制更新 —— Skija 官方文档写得很明确：
     *       “Subsequent drawing to Surface contents are not captured”。
     *       所以复用 surface 更新完像素之后，必须<b>重新取一次快照</b>拿到一个全新的 Image；
     *       沿用旧 Image 对象的话，每个槽的画面会永远冻结在它第一次被填入的那一帧。</li>
     *   <li>退役队列里只能放<b>被挤出的旧 Image</b>。旧实现把 {@code evicted.image}
     *       同时塞进新槽和退役队列，渲染线程下一帧就把它 close 了；
     *       环槽里于是剩下一个已释放的 Image（{@code _ptr == 0}），
     *       Skija 画它会静默地什么都不画（{@code SkCanvas::drawImage(nullptr)}），
     *       于是「背景还在，只是没了」——不报错、不崩溃、{@code isUsable()} 还是 true。
     *       这也是必须重新取快照的原因：新旧必须是两个不同对象。</li>
     * </ol>
     *
     * <p>顺带一提：{@code Image.makeFromEncoded()} 是惰性的（实测 0.13ms），
     * 真正的 JPEG 解码发生在第一次 draw 时（实测 4.3ms）。所以解码必须留在本方法里
     * 借 surface 完成（工作线程上，实测 3.1ms/帧），不能把惰性 Image 直接塞进环槽，
     * 否则解码会挪到渲染线程上逐帧发生。
     */
    private void store(int index, Image image) {
        int position = Math.floorMod(index, ring.length);
        Slot evicted;
        Image stored;
        synchronized (ring) {
            evicted = ring[position];
            if (evicted != null && evicted.surface != null) {
                // 复用已有 surface：像素覆盖 + 重新快照，缓冲不重新分配。
                try (Canvas c = evicted.surface.getCanvas()) {
                    c.drawImage(image, 0, 0);
                }
                stored = evicted.surface.makeImageSnapshot();
                ring[position] = new Slot(index, stored, evicted.surface);
            } else {
                // 首次：创建 surface + image 快照对。
                Surface s = Surface.makeRasterN32Premul(surfaceWidth, surfaceHeight);
                try (Canvas c = s.getCanvas()) {
                    c.drawImage(image, 0, 0);
                }
                // surface 保持打开状态（不在 try-with-resources 中关闭），
                // 以便后续帧可以复用同一 surface 更新像素而无需 alloc/free。
                stored = s.makeImageSnapshot();
                ring[position] = new Slot(index, stored, s);
            }
        }
        // 解码出来的源图只用来填一次像素，填完立刻释放，别堆到 GC 才回收（每帧 2MB）。
        closeQuietly(image);
        // 退役被挤出的旧图；它与刚放进环槽的 stored 一定不是同一个对象。
        if (evicted != null && evicted.image != null && evicted.image != stored) {
            synchronized (retired) {
                retired.add(evicted.image);
            }
        }
    }

    /**
     * 释放退役下来的图。只在渲染线程（{@link #current}）里调，
     * 且必须比退役时刻晚一整帧以上，保证没人还在画它。
     *
     * <p>策略：在 synchronized 块内把 {@code retired} 整体移入 {@code pendingRelease}，
     * 然后关闭队列中上一轮积累的旧图（已经比当前使用的帧早至少两帧，渲染线程不可能还在访问）。
     * 这样就不会出现后台线程刚 close 了某张图、渲染线程紧接着在读同一张图导致的
     * Native access violation。
     */
    private void releaseRetiredImages() {
        // 先只把「上一轮」挂进来的图取出来关掉，本轮新退役的压后一轮再关。
        // 旧实现是把 retired 整个搬进 pendingRelease 后立刻全部 close，
        // 等于「退役即释放」，一旦某张图还压在渲染线程手里就会踩到已释放的 Image。
        ArrayDeque<Image> toClose;
        synchronized (retired) {
            toClose = new ArrayDeque<>(pendingRelease);
            pendingRelease.clear();
            pendingRelease.addAll(retired);
            retired.clear();
        }
        // 这些图比当前播放帧至少早两轮，渲染线程早已不再引用它们。
        for (Image image : toClose) {
            closeQuietly(image);
        }
    }

    private static void closeQuietly(Image image) {
        try {
            image.close();
        } catch (RuntimeException ignored) {
            // 已经释放过了：忽略
        }
    }

    private Image decode(int index) {
        try {
            byte[] bytes = Files.readAllBytes(VideoBackgroundBaker.frameFile(directory, index));
            return Image.makeFromEncoded(bytes);
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    /** 停止播放并释放所有已解码的帧及关联 surface。 */
    void close() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
        worker = null;
        Slot[] slots;
        synchronized (ring) {
            slots = ring.clone();
            java.util.Arrays.fill(ring, null);
        }
        for (Slot slot : slots) {
            if (slot != null) {
                closeQuietly(slot.image());
                try { slot.surface().close(); } catch (RuntimeException ignored) {}
            }
        }
        synchronized (retired) {
            pendingRelease.addAll(retired);
            retired.clear();
        }
        for (Image image : pendingRelease) {
            closeQuietly(image);
        }
        pendingRelease.clear();
    }

    /** 一帧的画面。{@code blendImage} 非空时按 {@code blendWeight} 叠加在主图之上。 */
    record Frame(Image image, Image blendImage, float blendWeight) {
        static final Frame EMPTY = new Frame(null, null, 0.0F);

        boolean isEmpty() {
            return image == null;
        }

        boolean hasBlend() {
            return blendImage != null && blendWeight > 0.001F;
        }
    }

    /** 环形缓冲槽位：image（GPU 纹理引用）+ surface（CPU 端持久化画布，供 V1 复用）。 */
    private record Slot(int index, Image image, Surface surface) {
    }
}
