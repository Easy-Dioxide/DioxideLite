package com.dioxidelite.ui.dr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 原版精灵（sprite）：origin 是精灵级的（对所有帧相同），各帧可有自己的尺寸。
 * 数据对应 data.win 里 sprites 的 Width/Height/OriginX/OriginY 与逐帧 source 矩形。
 *
 * 帧是**按需解码**的：注册时只解码第 0 帧（拿 sprite_width/sprite_height），
 * 其余帧首次被画到时才解码 —— 避免 ch3 的 164 帧、ch4 的 94 片这类大精灵
 * 在演出开始前把渲染线程卡住。
 */
public final class Spr {
    public final String name;
    public final int originX;
    public final int originY;
    private final String pathPattern;
    private final int count;
    private final int[][] frames;
    private final int[] frameW;
    private final int[] frameH;

    private Spr(String name, int ox, int oy, String pathPattern, int count) {
        this.name = name;
        this.originX = ox;
        this.originY = oy;
        this.pathPattern = pathPattern;
        this.count = count;
        this.frames = new int[count][];
        this.frameW = new int[count];
        this.frameH = new int[count];
    }

    private static final Map<String, Spr> CACHE = new HashMap<>();

    /** 缓存键要带路径：同名精灵（如 spr_pixel_white / IMAGE_LOGO_CENTER）各章素材不一样。 */
    private static String key(String name, String path) {
        return name + "|" + path;
    }

    /** 注册一个多帧精灵（帧文件 <pathPattern>0.png、1.png…）。 */
    public static Spr of(String name, int originX, int originY, String pathPattern, int frameCount) {
        Spr cached = CACHE.get(key(name, pathPattern));
        if (cached != null) {
            return cached;
        }
        Spr s = new Spr(name, originX, originY, pathPattern,
                Math.max(1, frameCount));
        s.loadFrame(0);   // 第 0 帧立即解码：sprite_width/sprite_height 要用
        CACHE.put(key(name, pathPattern), s);
        return s;
    }

    /** 注册单帧精灵。 */
    public static Spr of1(String name, int originX, int originY, String path) {
        Spr cached = CACHE.get(key(name, path));
        if (cached != null) {
            return cached;
        }
        Spr s = new Spr(name, originX, originY, null, 1);
        s.frames[0] = readPng(path, s, 0);
        CACHE.put(key(name, path), s);
        return s;
    }

    private synchronized void loadFrame(int idx) {
        if (frames[idx] != null) {
            return;
        }
        frames[idx] = readPng(pathPattern + idx + ".png", this, idx);
    }

    private static int[] readPng(String path, Spr s, int idx) {
        try (InputStream is = Spr.class.getResourceAsStream(path)) {
            BufferedImage img = is == null ? null : ImageIO.read(is);
            if (img == null) {
                s.frameW[idx] = 0;
                s.frameH[idx] = 0;
                return new int[0];
            }
            int w = img.getWidth();
            int h = img.getHeight();
            int[] px = new int[w * h];
            img.getRGB(0, 0, w, h, px, 0, w);
            s.frameW[idx] = w;
            s.frameH[idx] = h;
            return px;
        } catch (Exception e) {
            s.frameW[idx] = 0;
            s.frameH[idx] = 0;
            return new int[0];
        }
    }

    public int frameCount() {
        return count;
    }

    /** 帧索引与 GML 一样取整后按帧数回绕。 */
    public int frameIndex(int imageIndex) {
        return Math.floorMod(imageIndex, count);
    }

    /** 取一帧像素（按需解码）。 */
    public int[] frame(int imageIndex) {
        int idx = frameIndex(imageIndex);
        if (frames[idx] == null) {
            loadFrame(idx);
        }
        return frames[idx];
    }

    public int frameW(int imageIndex) {
        int idx = frameIndex(imageIndex);
        if (frames[idx] == null) {
            loadFrame(idx);
        }
        return frameW[idx];
    }

    public int frameH(int imageIndex) {
        int idx = frameIndex(imageIndex);
        if (frames[idx] == null) {
            loadFrame(idx);
        }
        return frameH[idx];
    }

    /** sprite_width（= 第 0 帧宽）。 */
    public int width() {
        return frameW(0);
    }

    /** sprite_height（= 第 0 帧高）。 */
    public int height() {
        return frameH(0);
    }
}
