package com.dioxidelite.ui.dr;

import java.util.ArrayList;
import java.util.List;

/**
 * obj_intro_ch5 + obj_marker_fancy 的 Create/Step/Draw/Alarm 逐行转写。
 * room_intro_ch5：camera=port=640x480 → 画布 640x480，roomCenter = (320, 240)。
 *
 * deltarune_logo_ch5_itoki.ogg 的播放进度驱动三段发光标志渐显（0.65s / 1.3s / 2.0s），
 * 2.0s 起三段淡出、全标志 IMAGE_LOGO_256 + 红心渐显、房间预摆的 9 个闪光亮起并向外漂；
 * 120 帧后 black_all 90 帧淡入 + 100 帧 → 进菜单。跳过：button1_p（skip_safety>6 后）
 * → black_all 10 帧淡入 + alarm[0]=skip_time+5 → con=99。
 *
 * logoAll 的 draw_func 是 logoShoujoDraw：timer += timerPace 后套 shd_shoujo
 * （scr_shoujo_draw_on）再 draw_self；着色器按 GLSL 逐式转写成 shoujoRGB。
 */
public final class Ch5Intro extends IntroScene {
    private static boolean loaded;
    private static Spr glow3;
    private static Spr logo256;
    private static Spr sparkle;
    private static Spr centerLogo;
    private static Spr pixelWhite;
    private static Spr gradient;
    private static Spr bubl;
    private static Spr stars;

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        glow3 = Spr.of("IMAGE_LOGO_CENTER_SEPARATE_GLOW_3", 256, 256, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/logo_glow_", 3);
        logo256 = Spr.of1("IMAGE_LOGO_256", 128, 128, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/logo_full_0.png");
        sparkle = Spr.of("spr_bigshoujosparkle", 48, 48, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/sparkle_", 2);
        centerLogo = Spr.of1("IMAGE_LOGO_CENTER", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/center.png");
        pixelWhite = Spr.of1("spr_pixel_white", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/pixel_white_0.png");
        gradient = Spr.of1("spr_shoujogradient", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/gradient.png");
        bubl = Spr.of1("spr_bubl", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/bubl.png");
        stars = Spr.of1("spr_stars_2", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch5/stars.png");
    }

    // 必须在类初始化期就跑一次：下面 logoDel / logoAll 这些 marker 是实例字段初始化器，
    // 它们比构造器体先执行，那时静态 Spr 还是 null —— 带 null sprite 的 marker 一个像素都不画
    // （表现为「第一次播放只剩星光，重播才正常」，因为 load() 跑过一次后静态 Spr 就一直在）。
    // 静态初始化块早于任何实例创建，跑完之后字段初始化器拿到的就是真精灵。
    static {
        load();
    }

    // ---- Create_0 ----
    private int con;
    private int timer;
    private int bgm = -1;
    private double bgmPos;
    private int bgmSilentFrames;
    private final boolean filesExist;
    private int skipSafety;
    private int skipTime = 10;
    private boolean skipped;
    private int alarm0 = -1;
    private final double roomCenterX = 320;
    private final double roomCenterY = 240;
    private double timeDel;
    private double timeTa;
    private double timeRune;
    private int timeDelF;
    private int timeTaF;
    private int timeRuneF;

    private final ObjMarkerFancy logoDel = new ObjMarkerFancy(glow3);
    private final ObjMarkerFancy logoTa = new ObjMarkerFancy(glow3);
    private final ObjMarkerFancy logoRune = new ObjMarkerFancy(glow3);
    private final ObjMarkerFancy logoAll = new ObjMarkerFancy(logo256);
    private final ObjMarker logoHeart = new ObjMarker(centerLogo);
    private final ObjMarker blackAll = new ObjMarker(pixelWhite);
    private final List<ObjMarkerFancy> bigSparkles = new ArrayList<>();
    private final List<ObjLerpVar> lerps = new ArrayList<>();
    private double chapter5TextAlpha;

    // 临时诊断用：定位「无存档只出星光」，定性后整段删
    private int traceTick;
    private int steppedFrames;
    private long firstStepMs = -1L;
    private int lastLoggedCon = -1;

    /** 房间 Assets 层预摆的 9 个 spr_bigshoujosparkle（findspriteinfo_all(2352)）。 */
    private static final double[][] SPARKLE_INFO = {
            {125, 212, 1}, {103, 283, 0}, {189, 283, 1}, {237, 217, 0},
            {308, 283, 0}, {350, 230, 1}, {431, 258, 0}, {512, 224, 0}, {548, 294, 1},
    };

    public Ch5Intro(boolean savedFlavor) {
        super(640, 480);
        load();
        // obj_intro_ch5_Create_0
        con = 0;
        timer = 0;
        bgm = -1;
        bgmPos = 0;
        IntroSfx.stopAll();   // snd_free_all()
        filesExist = savedFlavor;
        // logoDel / logoTa / logoRune
        logoDel.x = roomCenterX;
        logoDel.y = roomCenterY;
        logoDel.imageAlpha = 0;
        logoDel.imageIndex = 0;
        logoDel.depth = 19900;
        logoDel.offset = 32;
        logoTa.x = roomCenterX;
        logoTa.y = roomCenterY;
        logoTa.imageAlpha = 0;
        logoTa.imageIndex = 1;
        logoTa.depth = 19900;
        logoTa.offset = 169;
        logoRune.x = roomCenterX;
        logoRune.y = roomCenterY;
        logoRune.imageAlpha = 0;
        logoRune.imageIndex = 2;
        logoRune.depth = 19900;
        logoRune.offset = 284;
        // logoAll（2771）
        logoAll.x = roomCenterX;
        logoAll.y = roomCenterY;
        logoAll.imageAlpha = 0;
        logoAll.imageIndex = 2;
        logoAll.depth = 20000;
        logoAll.imageXscale = 2;
        logoAll.imageYscale = 2;
        logoAll.timer = 0;
        logoAll.timerPace = 0.03f;
        // logoAll.draw_func = logoShoujoDraw（method 绑定到实例）
        logoAll.drawFunc = () -> {
            ObjMarkerFancy self = logoAll;
            self.timer += self.timerPace;
            // scr_shoujo_draw_on(timer)：着色器作用在随后的 draw_self 上
            shoujoOn = true;
            shoujoTime = self.timer;
            self.drawSelf(this);
            shoujoOn = false;   // shader_replace_simple_reset_hook()
        };
        // logoParts 的 sparking / step_func = logoShoujoSparkles（函数体为空）
        ObjMarkerFancy[] logoParts = {logoDel, logoTa, logoRune};
        for (ObjMarkerFancy part : logoParts) {
            part.sparkling = true;
            part.stepFunc = () -> {
                if (part.sparkling) {
                }
            };
        }
        logoParts[0].sparkling = true;
        // bigSparkles = findspriteinfo_all(2352) → scr_makemarker_fromstruct(s, true)
        for (double[] info : SPARKLE_INFO) {
            ObjMarkerFancy sp = new ObjMarkerFancy(sparkle);
            sp.x = info[0];
            sp.y = info[1];
            sp.imageIndex = (float) info[2];
            sp.imageSpeed = 0;
            // scr_depth()：depth = 100000 - ((y*10) + (sprite_height*10) + 0)
            sp.depth = (float) (100000 - ((sp.y * 10) + (sparkle.height() * 10)));
            sp.fadeSpeed = (float) (Math.random() * 0.5);
            sp.fadeOffset = (float) (Math.random() * 30);
            sp.mainAlpha = 0;
            sp.direction = (float) pointDirection(roomCenterX, roomCenterY, sp.x, sp.y);
            // bigSparkleStep
            sp.stepFunc = () -> {
                sp.imageAlpha = (float) (sp.mainAlpha * scrWave(0.2f, 1f, 1 + sp.fadeSpeed, sp.fadeOffset));
                sp.speed = sp.mainAlpha * 0.25f;
            };
            bigSparkles.add(sp);
        }
        // logoHeart
        logoHeart.x = roomCenterX;
        logoHeart.y = roomCenterY;
        logoHeart.imageAlpha = 0;
        logoHeart.depth = 21000;
        logoHeart.imageXscale = 2;
        logoHeart.imageYscale = 2;
        // black_all
        blackAll.imageBlend = 0x000000;   // c_black
        blackAll.depth = -999;
        blackAll.imageXscale = 500;
        blackAll.imageYscale = 500;
        blackAll.visible = true;
        blackAll.imageAlpha = 0;
        chapter5TextAlpha = 0;
        skipped = false;
        // 原版跳过分支（black_all + alarm）由本类处理，宿主的统一跳过关闭
        modSkipAllowed = false;
        // 临时诊断：每次重建都体检素材，看是哪一层没读到（帧数:首帧尺寸）
        System.out.println("[DioxideLite][DR][Intro][ch5] sprites"
                + " glow3=" + glow3.frameCount() + ":" + glow3.frameW(0) + "x" + glow3.frameH(0)
                + " logo256=" + logo256.frameCount() + ":" + logo256.frameW(0) + "x" + logo256.frameH(0)
                + " sparkle=" + sparkle.frameCount() + ":" + sparkle.frameW(0) + "x" + sparkle.frameH(0)
                + " center=" + centerLogo.frameCount() + ":" + centerLogo.frameW(0) + "x" + centerLogo.frameH(0)
                + " grad=" + gradient.frameW(0) + "x" + gradient.frameH(0)
                + " bubl=" + bubl.frameW(0) + "x" + bubl.frameH(0)
                + " stars=" + stars.frameW(0) + "x" + stars.frameH(0)
                + " pixel=" + pixelWhite.frameW(0) + "x" + pixelWhite.frameH(0));
    }

    /** scr_lerp_instance_var(target, varname, a, b, maxtime, easetype, easeinout)。 */
    private void lerpMarker(String varname, ObjMarker m, double a, double b, int maxtime, int easetype, String easeinout) {
        switch (varname) {
            case "image_alpha" -> lerps.add(new ObjLerpVar(v -> m.imageAlpha = (float) v, null, a, b, maxtime, easetype, easeinout));
            default -> {
            }
        }
    }

    private void lerpTimerPace(double a, double b, int maxtime) {
        lerps.add(new ObjLerpVar(v -> logoAll.timerPace = (float) v, null, a, b, maxtime, 2, "out"));
    }

    private void lerpSparkleMainAlpha(ObjMarkerFancy sp, double a, double b, int maxtime) {
        lerps.add(new ObjLerpVar(v -> sp.mainAlpha = (float) v, null, a, b, maxtime, 2, "out"));
    }

    @Override
    protected void step() {
        // obj_intro_ch5_Step_0
        traceTick++;
        steppedFrames++;
        if (firstStepMs < 0L) {
            firstStepMs = System.currentTimeMillis();
        }
        for (ObjLerpVar l : lerps) {
            l.step();
        }
        lerps.removeIf(l -> l.dead);
        // sparkles 的内建运动：speed 沿 direction（每帧 x += cos*speed，y -= sin*speed）
        for (ObjMarkerFancy sp : bigSparkles) {
            sp.x += Math.cos(Math.toRadians(sp.direction)) * sp.speed;
            sp.y += -Math.sin(Math.toRadians(sp.direction)) * sp.speed;
            sp.imageIndex += sp.imageSpeed;
            sp.stepFunc.run();
        }
        logoDel.stepFunc.run();
        logoTa.stepFunc.run();
        logoRune.stepFunc.run();
        logoAll.stepFunc.run();
        if (bgm != -1) {
            // 原版 audio_sound_get_track_position(bgm)：起线前 JavaSound 还没写出字节、position 返回 -1。
            // 有声音（含起线窗口）时给 0，声音完全起不来时按帧时钟兜底，避免整段停在 con==1。
            double p = sndPosition(bgm);
            if (p >= 0d) {
                bgmPos = p;
            } else if (IntroSfx.playing(bgm)) {
                bgmPos = 0d;
            } else {
                bgmSilentFrames++;
                bgmPos = bgmSilentFrames / 30.0;
            }
        }
        skipSafety++;
        if (button1P() && !skipped && skipSafety > 6) {
            lerpMarker("image_alpha", blackAll, blackAll.imageAlpha, 1, skipTime, 0, null);
            // mus_fade(global.currentsong[1], skip_time)：BGM 淡出
            sndVolumeFade(bgm, skipTime);
            skipped = true;
            alarm0 = skipTime + 5;
        }
        if (alarm0 > 0) {
            alarm0--;
            if (alarm0 == 0) {
                con = 99;   // obj_intro_ch5_Alarm_0
            }
        }
        if (con == 0) {
            timeDel = 0.65;
            timeTa = 1.3;
            timeRune = 2;
            timeDelF = (int) Math.ceil(timeDel * 30);
            timeTaF = (int) Math.ceil(timeTa * 30) - timeDelF;
            timeRuneF = (int) Math.ceil(timeRune * 30) - timeDelF - timeTaF;
            con = 1;
            bgm = sndPlay("ch5_logo.ogg", 1f);   // mus_play(snd_init("deltarune_logo_ch5_itoki.ogg"))
            lerpMarker("image_alpha", logoDel, 0, 1, timeDelF / 3, 2, "out");
        }
        if (con == 1) {
            if (bgmPos >= timeDel) {
                con = 2;
                logoDel.sparkling = false;
                lerpMarker("image_alpha", logoTa, 0, 1, timeTaF / 3, 2, "out");
            }
        }
        if (con == 2) {
            if (bgmPos >= timeTa) {
                logoTa.sparkling = false;
                con = 3;
                lerpMarker("image_alpha", logoRune, 0, 1, timeRuneF / 3, 2, "out");
            }
        }
        if (con == 3) {
            if (bgmPos >= timeRune) {
                logoRune.sparkling = false;
                con = 4;
                lerpMarker("image_alpha", logoDel, 1, 0, 25, 2, "out");
                lerpMarker("image_alpha", logoTa, 1, 0, 25, 2, "out");
                lerpMarker("image_alpha", logoHeart, 0, 1, 25, 2, "out");
                lerpMarker("image_alpha", logoRune, 1, 0, 25, 2, "out");
                lerpTimerPace(0.035, 0, 120);
                for (ObjMarkerFancy sp : bigSparkles) {
                    lerpSparkleMainAlpha(sp, 0, 0.7, 25);
                }
                logoAll.imageAlpha = 1;
            }
        }
        if (con == 4) {
            if (timer++ >= 120) {
                lerpMarker("image_alpha", blackAll, 0, 1, 90, 2, "out");
                timer = 0;
                con = 10;
            }
        }
        if (con == 10) {
            if (timer++ >= 100) {
                timer = 0;
                con = 99;
            }
        }
        if (con == 99) {
            finished = true;   // room_goto(PLACE_MENU)
            con = 999;
        }
        if (con != lastLoggedCon) {
            lastLoggedCon = con;
            trace("con");
        }
        if (traceTick % 15 == 0) {
            trace("tick");
        }
    }

    @Override
    protected void render() {
        // 按 depth 降序：sparkles(~99000) → logoHeart 21000 → logoAll 20000 → 三段 19900 → black_all -999
        for (ObjMarkerFancy sp : bigSparkles) {
            sp.drawSelf(this);
        }
        logoHeart.drawSelf(this);
        // logoAll 的 draw_func 是 logoShoujoDraw（套 shd_shoujo 再 draw_self），必须走钩子
        logoAll.drawFunc.run();
        logoDel.drawSelf(this);
        logoTa.drawSelf(this);
        logoRune.drawSelf(this);
        blackAll.drawSelf(this);
        // obj_intro_ch5_Draw_0（chapter5_text_alpha 恒 0，保留结构）
        if (chapter5TextAlpha > 0) {
            drawAlpha = (float) chapter5TextAlpha;
            drawTextChapter(5, roomCenterX - 31, roomCenterY + 60, (float) chapter5TextAlpha);
            drawAlpha = 1f;
        }
        // 临时诊断：render 跑到底才有这条，和上面同 tick 的 tick 对不上就是画到一半断了
        if (traceTick % 15 == 0) {
            System.out.println("[DioxideLite][DR][Intro][ch5] render ok con=" + con);
        }
    }

    /** 临时诊断：con / 时钟 / 音频句柄 / 各层 alpha 一次打全。 */
    private void trace(String tag) {
        double pos = bgm >= 0 ? IntroSfx.position(bgm) : -1d;
        long el = firstStepMs < 0L ? 0L : System.currentTimeMillis() - firstStepMs;
        double fps = el > 0L ? steppedFrames * 1000.0 / el : 0d;
        System.out.println("[DioxideLite][DR][Intro][ch5] " + tag
                + " con=" + con + " timer=" + timer
                + " bgm=" + bgm + " pos=" + String.format("%.3f", pos)
                + " playing=" + IntroSfx.playing(bgm)
                + " bgmPos=" + String.format("%.3f", bgmPos)
                + " silent=" + bgmSilentFrames
                + " fps=" + String.format("%.1f", fps)
                + " del=" + String.format("%.2f", logoDel.imageAlpha)
                + " all=" + String.format("%.2f", logoAll.imageAlpha)
                + " heart=" + String.format("%.2f", logoHeart.imageAlpha)
                + " spk=" + String.format("%.2f", bigSparkles.isEmpty() ? 0d : bigSparkles.get(0).imageAlpha)
                + " black=" + String.format("%.2f", blackAll.imageAlpha));
    }

    // ---- shd_shoujo（scr_shoujo_draw_on / 片段着色器）----

    private boolean shoujoOn;
    private float shoujoTime;

    /** 挂钩在 draw_self 上：shoujoOn 时把标志的 RGB 整体替换成着色器结果。 */
    @Override
    protected void drawSelf(double x, double y, Spr spr, int imageIndex,
                            float xscale, float yscale, int blend, float alpha) {
        if (!shoujoOn) {
            super.drawSelf(x, y, spr, imageIndex, xscale, yscale, blend, alpha);
            return;
        }
        // 逐像素：RGB 走着色器公式，alpha 保留贴图
        if (spr == null || spr.frameCount() == 0 || alpha <= 0f) {
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
                int a = argb >>> 24;
                if (a == 0) {
                    continue;
                }
                // gl_FragCoord 是 application_surface（640x480，与房间视口 1:1）坐标；
                // ch3 的房间是 320x240、视口 640x480 才需要乘 2，ch5 不能照搬
                int rgb = shoujoRGB(tx, ty, shoujoTime);
                setPixel(tx, ty, (a << 24) | rgb, alpha);
            }
        }
    }

    /**
     * shd_shoujo 片段着色器逐式转写：
     * colour.rgb = texGradient(frac(fx/appSurfWidth + time)) 与相邻纹素插值（去带），
     * 再加两侧白边 abs(-1+frac(fx/appSurfWidth)*2)*0.5、两层 texBubble 与 texStars（加法）。
     */
    private static int shoujoRGB(int fx, int fy, float time) {
        float appSurfWidth = 640f;
        // Gradient
        float gradPos = frac(fx / appSurfWidth + time);
        int[] rgb = sample(gradient, gradPos, 0f);
        // Gradient debanding
        float mixpercent = frac(fx / (appSurfWidth / 128f));
        int[] rgb2 = sample(gradient, frac(gradPos + 0.5f / 128f), 0f);
        rgb[0] = (int) Math.round(lerp(rgb[0], rgb2[0], mixpercent));
        rgb[1] = (int) Math.round(lerp(rgb[1], rgb2[1], mixpercent));
        rgb[2] = (int) Math.round(lerp(rgb[2], rgb2[2], mixpercent));
        // White edges
        float edge = Math.abs(-1f + frac(fx / appSurfWidth) * 2f) * 0.5f;
        rgb[0] += Math.round(edge * 255f);
        rgb[1] += Math.round(edge * 255f);
        rgb[2] += Math.round(edge * 255f);
        // Bubbl
        float bx = frac((fx / appSurfWidth * 4f) + time * 1.25f);
        float by = frac(fy / appSurfWidth * 4f + time * 1.20f);
        int[] bub = sample(bubl, bx, by);
        rgb[0] += bub[0];
        rgb[1] += bub[1];
        rgb[2] += bub[2];
        // Bubbl2
        bx = frac((fx / appSurfWidth * 4f) + time * 2.65f);
        by = frac(fy / appSurfWidth * 4f + time * 2.60f);
        bub = sample(bubl, bx, by);
        rgb[0] += Math.round(bub[0] * 0.15f);
        rgb[1] += Math.round(bub[1] * 0.15f);
        rgb[2] += Math.round(bub[2] * 0.15f);
        // Star
        float sx = frac((fx / appSurfWidth * 4f) + time * 1.95f);
        float sy = frac(fy / appSurfWidth * 4f + time * 1.90f);
        int[] star = sample(stars, sx, sy);
        rgb[0] += star[0];
        rgb[1] += star[1];
        rgb[2] += star[2];
        return (clamp(rgb[0]) << 16) | (clamp(rgb[1]) << 8) | clamp(rgb[2]);
    }

    /** 采样一张纹理（GL repeat 取模）。 */
    private static int[] sample(Spr sp, float u, float v) {
        if (sp == null || sp.frameCount() == 0 || sp.frame(0).length == 0) {
            return new int[]{0, 0, 0};
        }
        int w = sp.frameW(0);
        int h = sp.frameH(0);
        int x = Math.floorMod((int) (u * w), w);
        int y = Math.floorMod((int) (v * h), h);
        int argb = sp.frame(0)[y * w + x];
        return new int[]{(argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF};
    }

    // ---- 原版数学函数 ----

    private static float frac(float v) {
        float r = v % 1f;
        return r < 0 ? r + 1 : r;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static double pointDirection(double x1, double y1, double x2, double y2) {
        double deg = Math.toDegrees(Math.atan2(-(y2 - y1), x2 - x1));
        return deg < 0 ? deg + 360 : deg;
    }

    /** scr_wave(a0, a1, a2, a3)。 */
    private static double scrWave(float a0, float a1, float a2, float a3) {
        double a4 = (a1 - a0) * 0.5;
        double t = System.currentTimeMillis() * 0.001;
        return a0 + a4 + Math.sin(((t + a2 * a3) / a2) * (2 * Math.PI)) * a4;
    }
}
