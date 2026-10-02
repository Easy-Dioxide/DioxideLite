package com.dioxidelite.ui.dr;


/**
 * 各章开机演出的宿主：画布 + 帧调度 + 原版 draw_* 函数的实现。
 *
 * 子类（Ch1ProcessLogo / Ch2Intro / Ch3Intro / Ch4Intro / Ch5Intro）里是反编译
 * GML 的逐行转写：原版 Create 写进构造器、Step 事件写进 step()、Draw 事件写进
 * render()，原版对象按内部类拆分。坐标全部照抄 GML 数值（画布就是该房间的
 * camera 视口：ch2/ch3 是 320x240，ch4/ch5 是 640x480）。
 *
 * 帧率由 {@link #frameMs} 决定，默认按 30fps；原版 ch1 的 PROCESS_LOGO 在自己
 * Create_0 里写了 room_speed = 15，所以那一段是 15fps（见 {@link IntroScenes#frameMs}）。
 *
 * 绘制原语与 GML 同名同义：draw_self / draw_sprite_ext / draw_sprite_part_ext /
 * draw_sprite_tiled_ext / draw_rectangle / draw_surface / draw_text_ext，
 * 以及 gpu_set_colorwriteenable 的通道掩码（ch4 卷轴 surface 的两个 pass 用）。
 */
public abstract class IntroScene implements IntroFrameSource {
    /** surface（含 alpha 通道的像素缓冲），对应 GML surface_create。 */
    public static final class Surface {
        public final int w;
        public final int h;
        public final int[] px;

        public Surface(int w, int h) {
            this.w = w;
            this.h = h;
            this.px = new int[w * h];
        }
    }

    protected final int viewW;
    protected final int viewH;
    private final int[] canvas;
    private final byte[] frame;
    private long lastStepMs = -1L;
    protected boolean finished;

    /**
     * 本演出每逻辑帧的毫秒数（= 原版该房间的 1000/room_speed）。
     * ch1 的 PROCESS_LOGO 在 Create_0 里设了 room_speed = 15，其余章走 30fps 默认值；
     * 由 {@link IntroScenes#create} 按章写入。
     */
    protected int frameMs = 33;

    // ---- 绘制状态（draw_set_* / gpu_set_* 的全局）----
    protected Surface target;                                  // null = 直接画到房间画布
    protected int drawColor = 0xFFFFFF;                        // draw_set_color，c_white
    protected float drawAlpha = 1f;                            // draw_set_alpha
    protected final boolean[] colorWrite = {true, true, true, true};
    protected boolean blendAdd;                                // draw_set_blend_mode(bm_add)

    // ---- mod 附加的统一跳过（沿用原版 ch1 obj_fadeout 的 0.04/帧、30 帧结束）----
    private boolean modSkipActive;
    private int modSkipTimer;
    /** 子类可置 false（例如 ch5 走自己的 black_all 跳过分支时避免双重黑幕）。 */
    protected boolean modSkipAllowed = true;

    protected IntroScene(int viewW, int viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
        this.canvas = new int[viewW * viewH];
        this.frame = new byte[viewW * viewH * 4];
        java.util.Arrays.fill(canvas, 0xFF000000);
    }

    @Override
    public final int width() {
        return viewW;
    }

    @Override
    public final int height() {
        return viewH;
    }

    @Override
    public final byte[] frame() {
        return frame;
    }

    @Override
    public final boolean isFinished() {
        return finished;
    }

    @Override
    public boolean requestSkip() {
        if (!modSkipAllowed || modSkipActive || finished) {
            return false;
        }
        modSkipActive = true;
        modSkipTimer = 0;
        onSkipRequested();
        return true;
    }

    /** 跳过键生效时的额外处理（ch1 要把底噪淡出）。 */
    protected void onSkipRequested() {
    }

    protected final boolean isSkipping() {
        return modSkipActive;
    }

    @Override
    public final void advance(long nowMs) {
        if (finished) {
            return;
        }
        if (lastStepMs < 0L) {
            lastStepMs = nowMs;
        }
        int steps = 0;
        while (nowMs - lastStepMs >= frameMs && steps < 8) {
            lastStepMs += frameMs;
            steps++;
        }
        if (steps <= 0) {
            return;
        }
        for (int i = 0; i < steps && !finished; i++) {
            step();                                   // 原版 Step 事件
            if (modSkipActive) {
                modSkipTimer++;
                if (modSkipTimer >= 30) {
                    finished = true;                  // obj_fadeout + skiptimer>=30 → room_goto(PLACE_MENU)
                }
            }
        }
        java.util.Arrays.fill(canvas, 0xFF000000);
        target = null;
        render();                                      // 原版 Draw 事件
        if (modSkipActive) {
            overlayBlack(Math.min(1f, modSkipTimer * 0.04f));
        }
        toRgba();
    }

    /** 帧数 → 毫秒：按本演出的帧率换算，音效淡出这类原版按帧计的时长要用它。 */
    protected final int framesToMs(int frames) {
        return Math.max(1, frames) * frameMs;
    }

    /** 原版 Step 事件。 */
    protected abstract void step();

    /** 原版 Draw 事件。 */
    protected abstract void render();

    // ---- draw_* 函数 ----

    /** draw_self：以精灵 origin 锚点画当前帧（帧左上 = (x - originX*xscale, y - originY*yscale)）。 */
    protected void drawSelf(double x, double y, Spr spr, int imageIndex,
                            float xscale, float yscale, int blend, float alpha) {
        drawSpriteExt(x, y, spr, imageIndex, xscale, yscale, blend, alpha);
    }

    /** draw_sprite_ext(sprite, index, x, y, xscale, yscale, rot, color, alpha)。 */
    protected void drawSpriteExt(double x, double y, Spr spr, int imageIndex,
                                 float xscale, float yscale, int blend, float alpha) {
        if (spr == null || alpha <= 0f || spr.frameCount() == 0) {
            return;
        }
        int idx = spr.frameIndex(imageIndex);
        int[] px = spr.frame(idx);
        if (px.length == 0) {
            return;
        }
        int fw = spr.frameW(idx);
        int fh = spr.frameH(idx);
        int ox = (int) Math.round(x - spr.originX * xscale);
        int oy = (int) Math.round(y - spr.originY * yscale);
        int w = Math.round(fw * xscale);
        int h = Math.round(fh * yscale);
        for (int row = 0; row < h; row++) {
            int ty = oy + row;
            if (ty < 0 || ty >= viewH) {
                continue;
            }
            int srcRow = Math.min(fh - 1, (int) (row / yscale));
            for (int col = 0; col < w; col++) {
                int tx = ox + col;
                if (tx < 0 || tx >= viewW) {
                    continue;
                }
                int argb = px[Math.min(px.length - 1, srcRow * fw + Math.min(fw - 1, (int) (col / xscale)))];
                setPixel(tx, ty, tint(argb, blend), alpha);
            }
        }
    }

    /** draw_sprite_part_ext(sprite, index, xleft, ytop, w, h, x, y, xs, ys, color, alpha)。 */
    protected void drawSpritePartExt(Spr spr, int imageIndex, int xleft, int ytop, int partW, int partH,
                                     double x, double y, float xscale, float yscale, int blend, float alpha) {
        if (spr == null || alpha <= 0f || spr.frameCount() == 0) {
            return;
        }
        int idx = spr.frameIndex(imageIndex);
        int[] px = spr.frame(idx);
        if (px.length == 0) {
            return;
        }
        int fw = spr.frameW(idx);
        int fh = spr.frameH(idx);
        int ox = (int) Math.round(x);
        int oy = (int) Math.round(y);
        int w = Math.round(partW * xscale);
        int h = Math.round(partH * yscale);
        for (int row = 0; row < h; row++) {
            int ty = oy + row;
            if (ty < 0 || ty >= viewH) {
                continue;
            }
            int sy = ytop + Math.min(partH - 1, (int) (row / yscale));
            if (sy < 0 || sy >= fh) {
                continue;
            }
            for (int col = 0; col < w; col++) {
                int tx = ox + col;
                if (tx < 0 || tx >= viewW) {
                    continue;
                }
                int sx = xleft + Math.min(partW - 1, (int) (col / xscale));
                if (sx < 0 || sx >= fw) {
                    continue;
                }
                setPixel(tx, ty, tint(px[sy * fw + sx], blend), alpha);
            }
        }
    }

    /** draw_sprite_tiled_ext(sprite, index, x, y, xscale, yscale, color, alpha)：从 (x,y) 起平铺。 */
    protected void drawSpriteTiledExt(Spr spr, int imageIndex, double x, double y,
                                      float xscale, float yscale, int blend, float alpha) {
        if (spr == null || alpha <= 0f || spr.frameCount() == 0) {
            return;
        }
        int idx = spr.frameIndex(imageIndex);
        if (spr.frame(idx).length == 0) {
            return;
        }
        int fw = Math.max(1, Math.round(spr.frameW(idx) * xscale));
        int fh = Math.max(1, Math.round(spr.frameH(idx) * yscale));
        int startX = (int) Math.round(x) % fw;
        if (startX > 0) {
            startX -= fw;
        }
        int startY = (int) Math.round(y) % fh;
        if (startY > 0) {
            startY -= fh;
        }
        for (int ty = startY; ty < viewH; ty += fh) {
            for (int tx = startX; tx < viewW; tx += fw) {
                drawSpriteExt(tx + spr.originX * xscale, ty + spr.originY * yscale,
                        spr, idx, xscale, yscale, blend, alpha);
            }
        }
    }

    /** draw_rectangle（实心，用 draw_set_color/draw_set_alpha 的状态）。 */
    protected void drawRectangle(double x1, double y1, double x2, double y2) {
        int xa = Math.max(0, (int) Math.round(x1));
        int xb = Math.min(viewW - 1, (int) Math.round(x2));
        int ya = Math.max(0, (int) Math.round(y1));
        int yb = Math.min(viewH - 1, (int) Math.round(y2));
        int argb = (Math.round(drawAlpha * 255f) << 24) | (drawColor & 0xFFFFFF);
        for (int y = ya; y <= yb; y++) {
            for (int x = xa; x <= xb; x++) {
                setPixel(x, y, argb, 1f);
            }
        }
    }

    /** draw_surface(surface, x, y)：整层按 alpha over 合成。 */
    protected void drawSurface(Surface surf, double x, double y, float alpha) {
        if (surf == null || alpha <= 0f) {
            return;
        }
        int ox = (int) Math.round(x);
        int oy = (int) Math.round(y);
        boolean[] saved = colorWrite.clone();
        java.util.Arrays.fill(colorWrite, true);
        for (int row = 0; row < surf.h; row++) {
            for (int col = 0; col < surf.w; col++) {
                setPixel(ox + col, oy + row, surf.px[row * surf.w + col], alpha);
            }
        }
        System.arraycopy(saved, 0, colorWrite, 0, 4);
    }

    /** gpu_set_colorwriteenable(r,g,b,a)。 */
    protected void gpuSetColorwrite(boolean r, boolean g, boolean b, boolean a) {
        colorWrite[0] = r;
        colorWrite[1] = g;
        colorWrite[2] = b;
        colorWrite[3] = a;
    }

    /** 当前绘制目标（surface 或房间画布）。 */
    protected int[] targetPixels() {
        return target != null ? target.px : canvas;
    }

    protected int targetWidth() {
        return target != null ? target.w : viewW;
    }

    protected int targetHeight() {
        return target != null ? target.h : viewH;
    }

    /** 把一个像素写进当前目标，按 colorWrite 掩码与 alpha 混合（GML 的绘制语义）。 */
    protected void setPixel(int x, int y, int argb, float alpha) {
        if (x < 0 || x >= targetWidth() || y < 0 || y >= targetHeight()) {
            return;
        }
        int[] dstArr = targetPixels();
        int i = y * targetWidth() + x;
        int srcA = argb >>> 24;
        if (srcA == 0 && !blendAdd) {
            return;
        }
        float a = (srcA / 255f) * alpha;
        int dst = dstArr[i];
        float dstA = ((dst >>> 24) & 0xFF) / 255f;
        float outA = blendAdd ? Math.min(1f, dstA + a) : a + dstA * (1f - a);
        int out = colorWrite[3] ? (Math.round(outA * 255f) << 24) : (dst & 0xFF000000);
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ch = shift / 8;
            if (!colorWrite[ch]) {
                out |= dst & (0xFF << shift);
                continue;
            }
            float sc = ((argb >> shift) & 0xFF) / 255f;
            float dc = ((dst >> shift) & 0xFF) / 255f;
            int v;
            if (blendAdd) {
                v = Math.round(Math.min(255f, dc * 255f + sc * 255f * alpha));
            } else if (outA <= 0f) {
                v = 0;
            } else {
                v = Math.round(((sc * a + dc * dstA * (1f - a)) / outA) * 255f);
            }
            out |= Math.max(0, Math.min(255, v)) << shift;
        }
        dstArr[i] = out;
    }

    /** draw_text_ext(x, y, "第N章", 10, 900)（fnt_mainbig 字形，见 FontBig）。 */
    protected void drawTextChapter(int chapter, double x, double y, float alpha) {
        FontBig.drawChapterText(this, chapter, x, y, alpha);
    }

    private static int tint(int argb, int blend) {
        if (blend == 0xFFFFFF || (argb >>> 24) == 0) {
            return argb;
        }
        int r = ((argb >>> 16) & 0xFF) * ((blend >>> 16) & 0xFF) / 255;
        int g = ((argb >>> 8) & 0xFF) * ((blend >>> 8) & 0xFF) / 255;
        int b = (argb & 0xFF) * (blend & 0xFF) / 255;
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** 画布整体向黑压（obj_fadeout 的黑幕）。 */
    protected void overlayBlack(float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float keep = 1f - alpha;
        for (int i = 0; i < canvas.length; i++) {
            int c = canvas[i];
            int r = Math.round(((c >>> 16) & 0xFF) * keep);
            int g = Math.round(((c >>> 8) & 0xFF) * keep);
            int b = Math.round((c & 0xFF) * keep);
            canvas[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private void toRgba() {
        int n = viewW * viewH;
        for (int i = 0, p = 0; i < n; i++, p += 4) {
            int c = canvas[i];
            frame[p] = (byte) ((c >> 16) & 0xFF);
            frame[p + 1] = (byte) ((c >> 8) & 0xFF);
            frame[p + 2] = (byte) (c & 0xFF);
            frame[p + 3] = (byte) 255;
        }
    }

    // ---- 常用原版函数 ----

    /** snd_play / audio_play_sound。 */
    protected static int sndPlay(String file, float gain) {
        return IntroSfx.play(file, gain);
    }

    /** snd_loop / audio_play_sound(snd, 50, true)。 */
    protected static int sndPlayLoop(String file, float gain) {
        return IntroSfx.playLoop(file, gain);
    }

    /** audio_play_sound + audio_sound_pitch：循环轨带音高（ch3 三条欢呼轨用）。 */
    protected static int sndPlayLoopPitch(String file, float gain, double pitch) {
        return IntroSfx.playLoopPitch(file, gain, pitch);
    }

    /** audio_sound_get_track_position（轨道已结束返回 -1）。 */
    protected static double sndPosition(int handle) {
        return handle >= 0 ? IntroSfx.position(handle) : -1d;
    }

    /** audio_sound_length。 */
    protected static double sndLength(String file, double fallback) {
        return IntroSfx.duration(file, fallback);
    }

    /** snd_free / snd_stop。 */
    protected static void sndFree(int handle) {
        IntroSfx.stop(handle);
    }

    /** snd_volume(handle, 0, frames) 单轨淡出（frames 按本演出的帧率换算成毫秒）。 */
    protected final void sndVolumeFade(int handle, int frames) {
        IntroSfx.fadeOut(handle, framesToMs(frames));
    }

    /** 跳过键（button1_p）。 */
    protected static boolean button1P() {
        long w = net.minecraft.client.Minecraft.getInstance().getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_Z) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }
}
