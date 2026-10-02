package com.dioxidelite.audio;

import com.dioxidelite.DioxideLite;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import repackage.com.jsyn.data.FloatSample;
import repackage.com.jsyn.util.soundfile.streamed.buffered.BufferedSampleLoader;
import repackage.javazoom.jl.converter.Converter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端通用音频管理器：统一解码 wav / mp3 / ogg / flac，混成一条 44100/16/2 输出线。
 *
 * 音源按名字注册（name -> jar 内路径），prepare() 会先把它们导出到外部目录，
 * 玩家可用同名文件替换；随后后台全部预解码，play 那一刻不再现场解码。
 * 每条播放只登记一个实例，混音线程每 20ms 把它们相加写出，没有实例时关线把设备让回游戏。
 */
public final class AudioManager {

    /** 统一输出格式：所有音源都重采样到这里，否则没法混成一条线。 */
    private static final int OUT_RATE = 44100;
    private static final int OUT_CH = 2;
    private static final int BLOCK_MS = 20;
    private static final int BLOCK_FRAMES = OUT_RATE * BLOCK_MS / 1000;
    private static final int BLOCK_BYTES = BLOCK_FRAMES * OUT_CH * 2;

    private static final Map<String, String> SOURCES = new ConcurrentHashMap<>();
    private static final Map<String, Pcm> CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, Voice> VOICES = new ConcurrentHashMap<>();
    private static int nextHandle = 1;

    private static volatile Path externalDir;
    private static volatile boolean prepared;
    private static volatile SourceDataLine out;
    private static volatile boolean mixing;
    /** 这台机器开不出输出线，别再无谓重试。 */
    private static volatile boolean noDevice;
    private static final Object lineLock = new Object();

    /** 游戏主音量，由 syncGameVolume() 从 MC 选项同步；< 0 表示还没读到过。 */
    private static volatile float musicVolume = 1f;
    private static volatile long volumeSyncedAt;

    // 全局淡出（跳过整段演出时）：fadeStartMs < 0 表示没在淡出
    private static volatile long fadeStartMs = -1L;
    private static volatile long fadeDurationMs = 0L;

    private AudioManager() {
    }

    /** 解码好的音源：交织 PCM（已重采样到 OUT_RATE / OUT_CH）。 */
    private record Pcm(short[] pcm, int frames) {
    }

    /** 解码的中间结果，保留原始采样率/声道，交给 resample。 */
    private record Raw(short[] pcm, int frames, int rate, int channels) {
    }

    /** 一次播放：共享音源 + 自己的进度、音量、pitch。 */
    private static final class Voice {
        final int handle;
        final Pcm pcm;
        final boolean loop;
        /** 每个输出帧前进多少个源帧：重采样已在解码期做完，这里只承载 pitch。 */
        final double step;
        /** 起播音量：单路淡出拿它当基准，不能默认从 1.0 起。 */
        final float baseGain;
        volatile double pos;
        volatile float gain;
        volatile long playedFrames;
        volatile boolean dead;

        Voice(int handle, Pcm pcm, boolean loop, double pitch, float baseGain) {
            this.handle = handle;
            this.pcm = pcm;
            this.loop = loop;
            this.step = pitch;
            this.baseGain = baseGain;
            this.gain = baseGain;
        }
    }

    // 注册与准备

    /** 注册一个音源：name 是外部文件名/缓存键，resource 是 jar 内路径。 */
    public static void register(String name, String resource) {
        SOURCES.put(name, resource);
    }

    /** 外部替换目录；为 null 时只读 jar 内资源。 */
    public static void setExternalDirectory(Path dir) {
        externalDir = dir;
    }

    /** 导出全部音源（已存在的不覆盖，玩家可换），随后后台预热解码。 */
    public static synchronized void prepare() {
        if (prepared) {
            return;
        }
        prepared = true;
        Path dir = externalDir;
        if (dir != null) {
            try {
                Files.createDirectories(dir);
                for (Map.Entry<String, String> entry : SOURCES.entrySet()) {
                    Path target = dir.resolve(entry.getKey());
                    if (Files.exists(target)) {
                        continue;
                    }
                    try (InputStream is = AudioManager.class.getResourceAsStream(entry.getValue())) {
                        if (is != null) {
                            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } catch (Throwable t) {
                DioxideLite.LOGGER.warn("[AudioManager] export failed", t);
            }
        }
        Thread warm = new Thread(() -> {
            for (String name : SOURCES.keySet()) {
                load(name);
            }
        }, "dioxidelite-audio-warm");
        warm.setDaemon(true);
        warm.start();
    }

    /** 提前把声卡打开再关一次，规避起播瞬间首次初始化造成的卡顿。 */
    public static void warmDevice() {
        Thread warm = new Thread(() -> {
            SourceDataLine line = null;
            try {
                AudioFormat format = new AudioFormat(OUT_RATE, 16, OUT_CH, true, false);
                line = AudioSystem.getSourceDataLine(format);
                line.open(format);
            } catch (Throwable ignored) {
            } finally {
                if (line != null) {
                    try {
                        line.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, "dioxidelite-audio-device");
        warm.setDaemon(true);
        warm.start();
    }

    // 播放

    public static int play(String name, float gain) {
        return start(name, gain, false, 1.0);
    }

    public static int playLoop(String name, float gain) {
        return start(name, gain, true, 1.0);
    }

    public static int playLoopPitch(String name, float gain, double pitch) {
        return start(name, gain, true, pitch);
    }

    private static int start(String name, float gain, boolean loop, double pitch) {
        int handle = nextHandle++;
        Pcm pcm = load(name);
        if (pcm == null) {
            // 句柄照样发出去，只是这路不会发声（调用方的 stop 仍然安全）
            DioxideLite.LOGGER.warn("[AudioManager] missing audio source {}", name);
            return handle;
        }
        Voice v = new Voice(handle, pcm, loop, pitch > 0d ? pitch : 1.0, gain);
        VOICES.put(handle, v);
        ensureMixing();
        return handle;
    }

    private static Pcm load(String name) {
        Pcm cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        synchronized (CACHE) {
            Pcm again = CACHE.get(name);
            if (again != null) {
                return again;
            }
            Raw raw = decodeSource(name);
            if (raw == null) {
                return null;
            }
            Pcm resampled = resample(raw);
            if (resampled != null) {
                CACHE.put(name, resampled);
            }
            return resampled;
        }
    }

    /** 读外部替换文件优先，其次 jar 内资源；都没有返回 null。 */
    private static byte[] sourceBytes(String name) {
        Path dir = externalDir;
        if (dir != null) {
            Path file = dir.resolve(name);
            if (Files.isRegularFile(file)) {
                try {
                    return Files.readAllBytes(file);
                } catch (Throwable t) {
                    DioxideLite.LOGGER.warn("[AudioManager] unreadable external audio {}", file, t);
                }
            }
        }
        String resource = SOURCES.get(name);
        if (resource == null) {
            return null;
        }
        try (InputStream is = AudioManager.class.getResourceAsStream(resource)) {
            return is == null ? null : is.readAllBytes();
        } catch (Throwable t) {
            DioxideLite.LOGGER.warn("[AudioManager] unreadable bundled audio {}", resource, t);
            return null;
        }
    }

    private static Raw decodeSource(String name) {
        byte[] data = sourceBytes(name);
        if (data == null || data.length < 4) {
            return null;
        }
        try {
            // 按文件头真实格式分发：打包进来的音源里混着 .ogg 后缀的 RIFF 文件
            if (isOgg(data)) {
                return decodeOgg(data);
            }
            if (isRiff(data)) {
                return decodeJavaSound(data);
            }
            if (isFlac(data)) {
                return decodeFlac(data);
            }
            if (isMp3(data)) {
                return decodeMp3(data);
            }
            String lower = name.toLowerCase();
            if (lower.endsWith(".mp3")) {
                return decodeMp3(data);
            }
            if (lower.endsWith(".ogg")) {
                return decodeOgg(data);
            }
            if (lower.endsWith(".flac")) {
                return decodeFlac(data);
            }
            return decodeJavaSound(data);
        } catch (Throwable t) {
            DioxideLite.LOGGER.warn("[AudioManager] decode failed {}", name, t);
            return null;
        }
    }

    private static boolean isOgg(byte[] data) {
        return data.length >= 4 && data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S';
    }

    private static boolean isRiff(byte[] data) {
        return data.length >= 4 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';
    }

    private static boolean isFlac(byte[] data) {
        return data.length >= 4 && data[0] == 'f' && data[1] == 'L' && data[2] == 'a' && data[3] == 'C';
    }

    private static boolean isMp3(byte[] data) {
        if (data.length >= 3 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }
        return data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0;
    }

    private static Raw decodeJavaSound(byte[] data) throws Exception {
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
            AudioFormat src = raw.getFormat();
            int channels = Math.max(1, src.getChannels());
            float rate = src.getSampleRate();
            if (rate <= 0) {
                return null;
            }
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    rate, 16, channels, channels * 2, rate, false);
            if (!AudioSystem.isConversionSupported(target, src)) {
                // 压缩编码又不给转：只能按原始 16bit 小端读
                byte[] bytes = raw.readAllBytes();
                return fromLittleEndian(bytes, channels, (int) rate);
            }
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(target, raw)) {
                return fromLittleEndian(converted.readAllBytes(), channels, (int) rate);
            }
        }
    }

    private static Raw decodeMp3(byte[] data) throws Exception {
        Converter converter = new Converter();
        byte[] wav = converter.convert(new BufferedInputStream(new ByteArrayInputStream(data)), null, null);
        if (wav == null || wav.length == 0) {
            return null;
        }
        return decodeJavaSound(wav);
    }

    private static Raw decodeFlac(byte[] data) throws Exception {
        FloatSample sample = new BufferedSampleLoader()
                .loadFromFlacStream(new BufferedInputStream(new ByteArrayInputStream(data)));
        int frames = sample.getNumFrames();
        int channels = Math.max(1, sample.getChannelsPerFrame());
        float[] floats = new float[frames * channels];
        sample.read(floats);
        short[] pcm = new short[floats.length];
        for (int i = 0; i < floats.length; i++) {
            float v = Math.max(-1f, Math.min(1f, floats[i]));
            pcm[i] = (short) Math.round(v * 32767f);
        }
        return new Raw(pcm, frames, (int) sample.getFrameRate(), channels);
    }

    /**
     * jorbis 低层解码：整段数据先喂给 SyncState，按页 pagein / packetout。
     * 前三个包是 vorbis 头，之后才进 DspState 合成。
     */
    private static Raw decodeOgg(byte[] data) throws Exception {
        SyncState oy = new SyncState();
        StreamState os = new StreamState();
        Page og = new Page();
        Packet op = new Packet();
        Info vi = new Info();
        Comment vc = new Comment();
        DspState vd = new DspState();
        Block vb = new Block(vd);

        oy.init();
        vi.init();
        vc.init();

        int offset = oy.buffer(data.length);
        System.arraycopy(data, 0, oy.data, offset, data.length);
        oy.wrote(data.length);

        if (oy.pageout(og) != 1) {
            cleanOgg(oy, os, vi, vc, vd);
            throw new IllegalStateException("not an ogg bitstream");
        }
        os.init(og.serialno());

        int rate = 0;
        int sourceChannels = 0;
        short[] accum = new short[1 << 14];
        int accumLen = 0;
        int headers = 0;
        float[][][] pcmOut = new float[1][][];
        int[] pcmIndex = null;
        boolean havePage = true;

        while (havePage) {
            if (os.pagein(og) < 0) {
                break;
            }
            int result;
            while ((result = os.packetout(op)) != 0) {
                if (result < 0) {
                    continue;
                }
                if (headers < 3) {
                    if (vi.synthesis_headerin(vc, op) < 0) {
                        cleanOgg(oy, os, vi, vc, vd);
                        throw new IllegalStateException("not a vorbis stream");
                    }
                    headers++;
                    if (headers == 3) {
                        vd.synthesis_init(vi);
                        vb.init(vd);
                        rate = vi.rate;
                        sourceChannels = vi.channels;
                        pcmIndex = new int[sourceChannels];
                    }
                    continue;
                }
                if (vb.synthesis(op) == 0) {
                    vd.synthesis_blockin(vb);
                }
                int samples;
                int useChannels = Math.min(sourceChannels, 2);
                while ((samples = vd.synthesis_pcmout(pcmOut, pcmIndex)) > 0) {
                    float[][] pcm = pcmOut[0];
                    int need = accumLen + samples * useChannels;
                    if (need > accum.length) {
                        short[] bigger = new short[Math.max(accum.length * 2, need)];
                        System.arraycopy(accum, 0, bigger, 0, accumLen);
                        accum = bigger;
                    }
                    for (int s = 0; s < samples; s++) {
                        for (int c = 0; c < useChannels; c++) {
                            int idx = pcmIndex[c] + s;
                            float value = idx < pcm[c].length ? pcm[c][idx] : 0f;
                            value = Math.max(-1f, Math.min(1f, value));
                            accum[accumLen++] = (short) Math.round(value * 32767f);
                        }
                    }
                    vd.synthesis_read(samples);
                }
            }
            havePage = oy.pageout(og) == 1;
        }

        cleanOgg(oy, os, vi, vc, vd);
        if (headers < 3 || rate <= 0 || accumLen == 0) {
            return null;
        }
        int useChannels = Math.min(sourceChannels, 2);
        int frames = accumLen / useChannels;
        return new Raw(accum, frames, rate, sourceChannels);
    }

    private static void cleanOgg(SyncState oy, StreamState os, Info vi, Comment vc, DspState vd) {
        try {
            vd.clear();
        } catch (Throwable ignored) {
        }
        try {
            vi.clear();
        } catch (Throwable ignored) {
        }
        try {
            os.clear();
        } catch (Throwable ignored) {
        }
        try {
            oy.clear();
        } catch (Throwable ignored) {
        }
    }

    /** 重采样到 OUT_RATE / OUT_CH（线性插值；单声道复制成双声道）。 */
    private static Pcm resample(Raw raw) {
        if (raw.frames() <= 0 || raw.rate() <= 0) {
            return null;
        }
        int srcCh = Math.max(1, raw.channels());
        int useCh = Math.min(srcCh, 2);
        int srcFrames = raw.frames();
        short[] src = raw.pcm();
        int outFrames = (int) Math.round(srcFrames * (double) OUT_RATE / raw.rate());
        if (outFrames <= 0) {
            return null;
        }
        if (raw.rate() == OUT_RATE && raw.channels() == OUT_CH) {
            short[] direct = new short[outFrames * OUT_CH];
            System.arraycopy(src, 0, direct, 0, Math.min(src.length, direct.length));
            return new Pcm(direct, outFrames);
        }
        short[] dst = new short[outFrames * OUT_CH];
        for (int i = 0; i < outFrames; i++) {
            double sp = i * (double) raw.rate() / OUT_RATE;
            int i0 = (int) sp;
            if (i0 >= srcFrames - 1) {
                i0 = Math.max(0, srcFrames - 2);
            }
            double fr = sp - i0;
            int i1 = Math.min(srcFrames - 1, i0 + 1);
            if (useCh == 1) {
                short v = lerp16(src[i0 * srcCh], src[i1 * srcCh], fr);
                dst[i * 2] = v;
                dst[i * 2 + 1] = v;
            } else {
                dst[i * 2] = lerp16(src[i0 * srcCh], src[i1 * srcCh], fr);
                dst[i * 2 + 1] = lerp16(src[i0 * srcCh + 1], src[i1 * srcCh + 1], fr);
            }
        }
        return new Pcm(dst, outFrames);
    }

    private static Raw fromLittleEndian(byte[] bytes, int channels, int rate) {
        int frameBytes = Math.max(1, channels) * 2;
        int frames = bytes.length / frameBytes;
        short[] pcm = new short[frames * channels];
        for (int i = 0, p = 0; i < pcm.length; i++, p += 2) {
            pcm[i] = (short) ((bytes[p] & 0xFF) | (bytes[p + 1] << 8));
        }
        return new Raw(pcm, frames, rate, channels);
    }

    private static short lerp16(short a, short b, double t) {
        double v = a + (b - a) * t;
        if (v > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (v < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) Math.round(v);
    }

    // 混音输出

    private static void ensureMixing() {
        synchronized (lineLock) {
            if (mixing || noDevice) {
                return;
            }
            mixing = true;
            Thread t = new Thread(AudioManager::mixLoop, "dioxidelite-audio-mix");
            t.setDaemon(true);
            t.start();
        }
    }

    /** 设备线在混音线程里开：open() 冷启动要几百毫秒，放渲染线程就是起播那一下卡住。 */
    private static boolean openLine() {
        if (out != null) {
            return true;
        }
        AudioFormat format = new AudioFormat(OUT_RATE, 16, OUT_CH, true, false);
        try {
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            out = line;
            return true;
        } catch (Throwable t) {
            DioxideLite.LOGGER.warn("[AudioManager] output line unavailable", t);
            return false;
        }
    }

    private static void mixLoop() {
        int[] acc = new int[BLOCK_FRAMES * OUT_CH];
        byte[] block = new byte[BLOCK_BYTES];
        if (!openLine()) {
            noDevice = true;
            mixing = false;
            return;
        }
        while (true) {
            if (VOICES.isEmpty()) {
                // 判空和收线必须同一把锁下完成：登记实例的一方是「先 put 再 ensureMixing」，
                // 这里放锁会让新实例看到 mixing 还是 true，整段演出直接静音
                synchronized (lineLock) {
                    if (VOICES.isEmpty()) {
                        closeLine();
                        return;
                    }
                }
                continue;
            }
            java.util.Arrays.fill(acc, 0);
            for (Voice v : VOICES.values()) {
                mixVoice(v, acc);
            }
            float g = currentGain() * masterGain();
            if (g < 0.999f) {
                for (int i = 0; i < acc.length; i++) {
                    acc[i] = Math.round(acc[i] * g);
                }
            }
            toBytes(acc, block);
            SourceDataLine line = out;
            if (line == null) {
                closeLine();
                return;
            }
            try {
                // write 会在缓冲满时阻塞，天然把混音节流到实时播放速度
                line.write(block, 0, block.length);
            } catch (Throwable t) {
                closeLine();
                return;
            }
            VOICES.entrySet().removeIf(e -> e.getValue().dead);
        }
    }

    /** 把一路播放实例这一块的样本叠进累加缓冲（int 累加，最后统一饱和）。 */
    private static void mixVoice(Voice v, int[] acc) {
        if (v.dead) {
            return;
        }
        Pcm pcm = v.pcm;
        int frames = pcm.frames();
        for (int i = 0; i < BLOCK_FRAMES; i++) {
            if (v.pos >= frames) {
                if (!v.loop) {
                    v.dead = true;
                    break;
                }
                // 绕过整圈再取样：原版循环是无缝的，留空档会听到断音
                while (v.pos >= frames) {
                    v.pos -= frames;
                }
            }
            int idx = (int) v.pos;
            if (idx >= frames) {
                idx = frames - 1;
            }
            float gain = v.gain;
            if (gain > 0f) {
                acc[i * 2] += Math.round(pcm.pcm()[idx * 2] * gain);
                acc[i * 2 + 1] += Math.round(pcm.pcm()[idx * 2 + 1] * gain);
            }
            v.pos += v.step;
            v.playedFrames++;
        }
    }

    private static void toBytes(int[] acc, byte[] block) {
        for (int i = 0, p = 0; i < acc.length; i++, p += 2) {
            int s = acc[i];
            if (s > Short.MAX_VALUE) {
                s = Short.MAX_VALUE;
            } else if (s < Short.MIN_VALUE) {
                s = Short.MIN_VALUE;
            }
            block[p] = (byte) s;
            block[p + 1] = (byte) (s >> 8);
        }
    }

    private static void closeLine() {
        synchronized (lineLock) {
            mixing = false;
            SourceDataLine line = out;
            out = null;
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static float currentGain() {
        long start = fadeStartMs;
        if (start < 0L) {
            return 1f;
        }
        float g = 1f - (System.currentTimeMillis() - start) / (float) fadeDurationMs;
        return g < 0f ? 0f : g;
    }

    /** 游戏主音量乘数；syncGameVolume() 之后生效。 */
    private static float masterGain() {
        if (System.currentTimeMillis() - volumeSyncedAt > 1000L) {
            syncGameVolume();
        }
        return musicVolume;
    }

    /** 读 MC 的 MASTER 音量（含静音状态）；由客户端 tick 主动调用。 */
    public static void syncGameVolume() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null) {
                return;
            }
            float volume = mc.options.getFinalSoundSourceVolume(SoundSource.MASTER);
            musicVolume = Math.max(0f, Math.min(1f, volume));
            volumeSyncedAt = System.currentTimeMillis();
        } catch (Throwable ignored) {
        }
    }

    // 查询与控制

    /** 某路已播放的秒数；这路不在播（或已播完）返回 -1。 */
    public static double position(int handle) {
        Voice v = VOICES.get(handle);
        if (v == null) {
            return -1d;
        }
        return v.playedFrames / (double) OUT_RATE;
    }

    public static boolean playing(int handle) {
        return handle >= 0 && VOICES.containsKey(handle);
    }

    public static boolean finished(int handle) {
        return handle >= 0 && !VOICES.containsKey(handle);
    }

    public static void stop(int handle) {
        if (handle >= 0) {
            VOICES.remove(handle);
        }
    }

    /** 音源总时长（秒）：解过码就用精确帧数，否则返回 fallback。 */
    public static double duration(String name, double fallback) {
        Pcm cached = CACHE.get(name);
        return cached == null ? fallback : cached.frames() / (double) OUT_RATE;
    }

    /** 单路淡出（snd_volume(handle, 0, ms) 语义），其余轨不受影响。 */
    public static void fadeOut(int handle, int ms) {
        Voice v = VOICES.get(handle);
        if (v == null) {
            return;
        }
        long dur = Math.max(1, ms);
        Thread f = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (VOICES.get(handle) == v) {
                long el = System.currentTimeMillis() - start;
                if (el >= dur) {
                    break;
                }
                v.gain = v.baseGain * Math.max(0f, 1f - el / (float) dur);
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ie) {
                    return;
                }
            }
            if (VOICES.get(handle) == v) {
                VOICES.remove(handle);
            }
        }, "dioxidelite-audio-fade");
        f.setDaemon(true);
        f.start();
    }

    /** 全局淡出（跳过演出时），时长毫秒。 */
    public static void fadeOutAll(int ms) {
        fadeStartMs = System.currentTimeMillis();
        fadeDurationMs = Math.max(1, ms);
    }

    public static void stopAll() {
        VOICES.clear();
        fadeStartMs = -1L;
        fadeDurationMs = 0L;
    }

    /** 客户端退出时彻底收线。 */
    public static void close() {
        stopAll();
        closeLine();
    }
}
