package com.dioxidelite.ui.dr;

import java.util.ArrayList;
import java.util.List;

/**
 * obj_intro_ch4 + obj_texturescroller_prophecy + obj_dw_church_prophecy_groundshards
 * + obj_doom/obj_script_delayed/obj_afterimage 的逐行转写。
 * room_intro_ch4：camera=port=640x480 → 画布 640x480，room_center = (320, 240)。
 *
 * 无存档分支（!files_exist）：bgm 时间轴 con0~10 —— 心 120 帧渐显 → 4s 分离标志 90 帧 →
 *   7.5s 卷轴 70 帧 → 8s「第4章」90 帧 → 15s 卷轴停滚 → BGM 走到 0 时碎裂
 *   （4673 的 94 片，2x，direction=random(360)、speed4/gravity0.4 延迟 20 帧、doom 120）
 *   → 20 帧后 15 个地面闪光（ytarg=10000、doom 280）→ 45 帧后心淡出 120 帧 → 进菜单
 * 有存档分支：黑底 + 卷轴只染 logo_prophecy、滚速 52 帧停住 → 135 帧后 5776 碎裂 →
 *   20 帧后闪光（_width=90）→ 120 帧进菜单；button1_p 可提前跳（black_all 淡入 10 帧）
 */
public final class Ch4Intro extends IntroScene {
    private static boolean loaded;
    private static Spr heartSep;
    private static Spr shatter;
    private static Spr shatterPieces;
    private static Spr prophecyShatter;
    private static Spr prophecyIcon;
    private static Spr groundShine;
    private static Spr scrollBase;
    private static Spr scrollNoise;
    private static Spr pixelWhite;

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        heartSep = Spr.of("IMAGE_LOGO_CENTER_HEART_SEPARATED", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/heart_sep_", 2);
        shatter = Spr.of1("IMAGE_LOGO_CENTER_SHATTER", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/logo_shatter_0.png");
        shatterPieces = Spr.of("IMAGE_LOGO_CENTER_SHATTER_PIECES", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/shatter_pieces_", 94);
        prophecyShatter = Spr.of("spr_intro_prophecy_shatter", 49, 61, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/prophecy_shatter_", 26);
        prophecyIcon = Spr.of1("spr_dw_church_prophecy_final_icon_w", 49, 61, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/prophecy_icon_0.png");
        groundShine = Spr.of("spr_firework_shine", 2, 2, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/ground_shine_", 4);
        scrollBase = Spr.of1("IMAGE_DEPTH_EXTEND_MONO_SEAMLESS_BRIGHTER", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/scroll_base_0.png");
        scrollNoise = Spr.of1("spr_perlin_noise_looping", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/scroll_noise_0.png");
        pixelWhite = Spr.of1("spr_pixel_white", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch4/pixel_white_0.png");
    }

    // 必须在类初始化期就跑一次：下面 logoHeart / logoAll / logoProphecy / blackAll 是
    // 实例字段初始化器，它们比构造器体先执行，那时静态 Spr 还是 null —— 带 null sprite 的
    // marker 一个像素都不画（表现为「第一次播放只剩碎片和地面闪光，重播才正常」，
    // 因为 load() 跑过一次后静态 Spr 就一直在）。静态初始化块早于任何实例创建。
    static {
        load();
    }

    // ---- Create_0 ----
    private int con;
    private int timer;
    private int bgm = -1;
    private double bgmPos;
    private boolean bgmStarted;
    private int bgmSilentFrames;
    private final boolean filesExist;
    private int skipSafety;
    private final int skipTime = 10;              // Create_0：skip_time = 10
    private int init;                             // Create_0：init = 0
    private int type;                             // Create_0：type = 0
    private final double roomCenterX = 320;
    private final double roomCenterY = 240;
    private final ObjMarker logoHeart = new ObjMarker(heartSep);
    private final ObjMarker logoAll = new ObjMarker(heartSep);
    private final ObjMarker logoProphecy = new ObjMarker(prophecyIcon);
    private final ObjMarker blackAll = new ObjMarker(pixelWhite);
    private final ObjTextureScrollerProphecy prophecyEffect = new ObjTextureScrollerProphecy();
    private double chapter4TextAlpha;
    private final int breakNoise;   // snd_init("ch4_first_intro_breaking.ogg")（这里是 play 得到的句柄）
    private double prophecyEffectScrollSpeed = 2;
    // 对象列表
    private final List<Frag> fragments = new ArrayList<>();
    private final List<ObjGroundshards> groundshards = new ArrayList<>();
    private final List<ObjAfterimage> afterimages = new ArrayList<>();
    private final List<ObjDoom> dooms = new ArrayList<>();
    private final List<ObjScriptDelayed> delayedVars = new ArrayList<>();
    private final List<ObjLerpVar> lerps = new ArrayList<>();

    public Ch4Intro(boolean savedFlavor) {
        super(640, 480);
        load();
        // obj_intro_ch4_Create_0
        con = 0;
        timer = 0;
        bgm = -1;
        bgmPos = 0;
        init = 0;                       // Create_0：init = 0
        type = 0;                       // Create_0：type = 0（本链未分支出其他值）
        IntroSfx.stopAll();   // snd_free_all()
        filesExist = savedFlavor;
        logoHeart.imageAlpha = 0;
        logoHeart.depth = 19800;
        logoHeart.imageXscale = 2;
        logoHeart.imageYscale = 2;
        logoAll.imageAlpha = 0;
        logoAll.imageIndex = 1;
        logoAll.depth = 19900;
        logoAll.imageXscale = 2;
        logoAll.imageYscale = 2;
        logoProphecy.y = roomCenterY + 60;
        logoProphecy.x = roomCenterX;
        logoProphecy.imageAlpha = 0;
        logoProphecy.imageIndex = 0;
        logoProphecy.depth = 19900;
        logoProphecy.imageXscale = 2;
        logoProphecy.imageYscale = 2;
        logoHeart.x = roomCenterX;
        logoHeart.y = roomCenterY;
        logoAll.x = roomCenterX;
        logoAll.y = roomCenterY;
        prophecyEffect.tileObject = logoAll;
        prophecyEffect.imageAlpha = 0;
        prophecyEffect.introMode = true;
        prophecyEffect.scrollSpeed = (float) prophecyEffectScrollSpeed;
        blackAll.imageBlend = 0x000000;   // c_black
        blackAll.depth = -999;
        blackAll.imageXscale = 500;
        blackAll.imageYscale = 500;
        blackAll.visible = false;
        blackAll.imageAlpha = 1;
        chapter4TextAlpha = 0;
        // 原版：break_noise = snd_init("ch4_first_intro_breaking.ogg") —— snd_init 只载入流、不播放，
        // 真正播放是 con7 的 snd_play_delay(break_noise, _delay_sound_time, 0.5) 两次
        breakNoise = -1;
        // mod 跳过：原版无存档分支没有跳过键，有存档分支走 con==3 的黑幕跳过
        modSkipAllowed = !filesExist;
        // 临时诊断：每次重建都体检素材，看是哪一层没读到（帧数:首帧尺寸）
        System.out.println("[DioxideLite][DR][Intro][ch4] sprites"
                + " heartSep=" + heartSep.frameCount() + ":" + heartSep.frameW(0) + "x" + heartSep.frameH(0)
                + " shatter=" + shatter.frameW(0) + "x" + shatter.frameH(0)
                + " shatterPieces=" + shatterPieces.frameCount() + ":" + shatterPieces.frameW(0) + "x" + shatterPieces.frameH(0)
                + " prophecyShatter=" + prophecyShatter.frameCount() + ":" + prophecyShatter.frameW(0) + "x" + prophecyShatter.frameH(0)
                + " prophecyIcon=" + prophecyIcon.frameW(0) + "x" + prophecyIcon.frameH(0)
                + " groundShine=" + groundShine.frameCount() + ":" + groundShine.frameW(0) + "x" + groundShine.frameH(0)
                + " scrollBase=" + scrollBase.frameW(0) + "x" + scrollBase.frameH(0)
                + " scrollNoise=" + scrollNoise.frameW(0) + "x" + scrollNoise.frameH(0)
                + " pixel=" + pixelWhite.frameW(0) + "x" + pixelWhite.frameH(0));
    }

    /** scr_lerp_instance_var(target, varname, a, b, maxtime)。 */
    private void lerpVar(String varname, double a, double b, int maxtime) {
        switch (varname) {
            case "chapter4_text_alpha" -> lerps.add(new ObjLerpVar(v -> chapter4TextAlpha = v, null, a, b, maxtime, 0, null));
            case "image_alpha" -> {
            }
            default -> {
            }
        }
    }

    private void lerpMarker(ObjMarker m, double a, double b, int maxtime) {
        lerps.add(new ObjLerpVar(v -> m.imageAlpha = (float) v, null, a, b, maxtime, 0, null));
    }

    private void lerpScrollSpeed(double a, double b, int maxtime) {
        lerps.add(new ObjLerpVar(v -> prophecyEffect.scrollSpeed = (float) v, null, a, b, maxtime, 0, null));
    }

    private void lerpEffectAlpha(double a, double b, int maxtime) {
        lerps.add(new ObjLerpVar(v -> prophecyEffect.imageAlpha = (float) v, null, a, b, maxtime, 0, null));
    }

    @Override
    protected void step() {
        // obj_intro_ch4_Step_0
        traceTick++;
        steppedFrames++;
        if (firstStepMs < 0L) {
            firstStepMs = System.currentTimeMillis();
        }
        for (ObjLerpVar l : lerps) {
            l.step();
        }
        lerps.removeIf(l -> l.dead);
        for (ObjScriptDelayed d : delayedVars) {
            d.step();
        }
        delayedVars.removeIf(d -> d.dead);
        for (ObjScriptDelayed d : fragmentDelays) {
            d.step();
        }
        fragmentDelays.removeIf(d -> d.dead);
        for (ObjDoom d : dooms) {
            d.step();
        }
        dooms.removeIf(d -> d.dead);
        // 延迟音效（snd_play_delay）
        for (PendingSound s : pendingSounds) {
            if (--s.framesLeft <= 0) {
                s.handled = true;
                sndPlay(s.file, s.gain);
            }
        }
        pendingSounds.removeIf(s -> s.handled);
        // 碎片运动积分（GM 内建运动：gravity 沿 gravity_direction=270 累加 vspeed，friction=0）
        for (Frag f : fragments) {
            if (f.destroyed) {
                continue;
            }
            f.vspeed += f.gravity;
            f.x += f.hspeed;
            f.y += f.vspeed;
        }
        // 地面闪光
        for (ObjGroundshards g : groundshards) {
            if (!g.destroyed) {
                g.step();
            }
        }
        groundshards.removeIf(g -> g.destroyed);
        // 残影（obj_afterimage_Step_0）
        for (ObjAfterimage a : afterimages) {
            a.step();
        }
        afterimages.removeIf(a -> a.dead);
        if (bgm != -1) {
            // 原版 audio_sound_get_track_position(bgm)：曲末归 0。JavaSound 起线前也是 -1，
            // 「还没起线」用帧时钟推进，「播完」才是 0 —— 否则 con==6 会在第一帧就成立。
            double p = sndPosition(bgm);
            if (p >= 0d) {
                bgmStarted = true;
                bgmPos = p;
            } else if (bgmStarted) {
                bgmPos = 0d;
            } else {
                // 无声卡时 track 一直不建立，按音源时长兜底推进
                bgmSilentFrames++;
                double len = sndLength("ch4_first_intro.ogg", 17.68d);
                if (bgmSilentFrames / 30d >= len) {
                    bgmStarted = true;
                    bgmPos = 0d;
                } else {
                    bgmPos = bgmSilentFrames / 30d;
                }
            }
        }
        if (!filesExist) {
            stepNofiles();
        } else {
            stepSaved();
        }
        if (con == 99) {
            finished = true;   // room_goto(PLACE_MENU)
        }
        if (con != lastLoggedCon) {
            lastLoggedCon = con;
            trace("con");
        }
        if (traceTick % 15 == 0) {
            trace("tick");
        }
    }

    /** 无存档分支（Step_0 前半段）。 */
    private void stepNofiles() {
        if (con == 0) {
            con = 1;
            bgm = sndPlay("ch4_first_intro.ogg", 1f);   // mus_play(snd_init("ch4_first_intro.ogg"))
            bgmStarted = false;
            bgmSilentFrames = 0;
            lerpMarker(logoHeart, 0, 1, 120);
        }
        if (con == 1) {
            if (bgmPos >= 4) {
                con = 2;
            }
        }
        if (con == 2) {
            con = 3;
            lerpMarker(logoAll, 0, 1, 90);
        }
        if (con == 3) {
            if (bgmPos >= 7.5) {
                con = 4;
                lerpEffectAlpha(0, 1, 70);
            }
        }
        if (con == 4) {
            if (bgmPos >= 8) {
                con = 5;
                lerpVar("chapter4_text_alpha", 0, 1, 90);
            }
        }
        if (con == 5) {
            if (bgmPos >= 15) {
                con = 6;
                lerpScrollSpeed(prophecyEffectScrollSpeed, 0, 60);
            }
        }
        if (con == 6) {
            if (bgmPos == 0) {
                con = 7;
                timer = 0;
            }
        }
        if (con == 7) {
            con = 8;
            timer = 0;
            prophecyEffect.imageAlpha = 0;
            logoAll.sprite = shatter;                 // sprite_index = IMAGE_LOGO_CENTER_SHATTER
            chapter4TextAlpha = 0;
            int delaySoundTime = 20;
            playBreakSounds(delaySoundTime, 1f, 0.94f);
            spawnFragmentBurst(shatterPieces, roomCenterX, roomCenterY, 20);
            logoAll.visible = false;                  // with (logo_all) instance_destroy()
        }
        if (con == 8) {
            if (timer++ >= 20) {
                timer = 0;
                con = 9;
                int sparklecount = 15;
                for (int i = 0; i < sparklecount; i++) {
                    ObjGroundshards g = new ObjGroundshards();
                    g.x = (roomCenterX - 199) + ((i * 398) / (double) sparklecount) + (Math.random() * 60 - 30);
                    g.y = roomCenterY + Math.random() * 60;
                    g.ytarg = 10000;
                    groundshards.add(g);
                    dooms.add(new ObjDoom(g, 280));
                }
            }
        }
        if (con == 9) {
            if (timer++ >= 45) {
                timer = 0;
                con = 10;
                lerpMarker(logoHeart, 1, 0, 120);
            }
        }
        if (con == 10) {
            if (timer++ >= 120) {
                timer = 0;
                con = 99;
            }
        }
    }

    /** 有存档分支（Step_0 的 else 段）。 */
    private void stepSaved() {
        skipSafety++;
        if (con == 0) {
            con = 1;      // con = 0.5 的等价（用 1 承接，后续判断顺序一致）
            timer = 0;
            blackAll.visible = true;
            blackAll.imageAlpha = 0;
            prophecyEffect.tileObject = logoProphecy;
            prophecyEffect.imageAlpha = 1;
            logoProphecy.imageAlpha = 1;
        }
        if (button1P() && con < 3 && skipSafety > 3) {
            con = 3;
        }
        if (con == 1) {
            if (timer++ >= 75) {
                con = 2;
                timer--;
                lerpScrollSpeed(prophecyEffectScrollSpeed, 0, 52);
            }
        }
        if (con == 2) {
            if (timer++ > 135) {
                con = 4;
                timer = 0;
                logoProphecy.imageAlpha = 0;
                int delaySoundTime = 20;
                playBreakSounds(delaySoundTime, 0.5f, 0.44f);
                spawnFragmentBurst(prophecyShatter, logoProphecy.x, logoProphecy.y, 20);
            }
        }
        if (con == 3) {
            con = 5;
            timer = 0;
            lerpMarker(blackAll, blackAll.imageAlpha, 1, skipTime);
        }
        if (con == 4) {
            if (timer++ >= 20) {
                timer = 0;
                con = 6;
                int sparklecount = 15;
                for (int i = 0; i < sparklecount; i++) {
                    double width = 90;
                    double xx = (roomCenterX - width) + (i * ((width * 2) / sparklecount)) + (Math.random() * 60 - 30);
                    double yy = roomCenterY + Math.random() * 70;
                    if (i == 0) {
                        xx = roomCenterX - width;
                    }
                    if (i == sparklecount - 1) {
                        xx = roomCenterX + width;
                    }
                    ObjGroundshards g = new ObjGroundshards();
                    g.x = xx;
                    g.y = yy;
                    g.ytarg = 10000;
                    groundshards.add(g);
                    dooms.add(new ObjDoom(g, 280));
                }
            }
        }
        if (con == 5) {
            if (timer++ > skipTime) {
                timer = 0;
                con = 99;
            }
        }
        if (con == 6) {
            if (timer++ >= 120) {
                timer = 0;
                con = 99;
            }
        }
    }

    /** snd_play_complex + 两次 snd_play_delay(break_noise)。 */
    private void playBreakSounds(int delaySoundTime, float gainA, float gainB) {
        sndPlay("break1.wav", 1f);                                             // snd_add_complex(snd, 4, 321, 1, 0.95, 0)
        sndPlayDelayed("glassbreak.ogg", 0.6f, (delaySoundTime - 1) + 2);      // snd_add_complex(snd, 0, 236, 0.6, 0.4, ...)
        sndPlayDelayed("punchmed.ogg", 0.7f, (delaySoundTime - 1));            // snd_add_complex(snd, 3, 269, 0.7, 0.95, ...)
        sndPlayDelayed("ch4_breaking.ogg", 0.5f, delaySoundTime);              // snd_play_delay(break_noise, ...)
        sndPlayDelayed("ch4_breaking.ogg", 0.5f, delaySoundTime);
    }

    /** 延迟若干帧后播放音效（snd_play_delay 的等价物）。 */
    private void sndPlayDelayed(String file, float gain, int delayFrames) {
        pendingSounds.add(new PendingSound(file, gain, delayFrames));
    }

    private final List<PendingSound> pendingSounds = new ArrayList<>();

    private static final class PendingSound {
        final String file;
        final float gain;
        int framesLeft;
        boolean handled;

        PendingSound(String file, float gain, int framesLeft) {
            this.file = file;
            this.gain = gain;
            this.framesLeft = framesLeft;
        }
    }

    /** 碎片爆发（碎片 = scr_marker_ext + direction/speed/gravity/friction 延迟 + doom 120）。 */
    private void spawnFragmentBurst(Spr spr, double x, double y, int delay) {
        for (int i = 0; i < spr.frameCount(); i++) {
            Frag f = new Frag();
            f.sprite = spr;
            f.imageIndex = i;
            f.x = x;
            f.y = y;
            f.depth = 19800;
            f.imageXscale = 2;
            f.imageYscale = 2;
            f.direction = Math.random() * 360;
            f.dir = f.direction;
            // scr_delay_var("gravity", 0.4 + random(0.12), _delay) / ("friction", 0, _delay) / ("speed", 4, _delay)
            fragmentDelays.add(new ObjScriptDelayed(v -> f.gravity = (float) v, 0.4 + Math.random() * 0.12, delay));
            fragmentDelays.add(new ObjScriptDelayed(v -> f.friction = (float) v, 0, delay));
            fragmentDelays.add(new ObjScriptDelayed(v -> {
                f.speed = (float) v;
                // 设置 speed/direction 时同步 hspeed/vspeed（GM 的 speed 变量语义）
                f.hspeed = Math.cos(Math.toRadians(f.dir)) * v;
                f.vspeed = -Math.sin(Math.toRadians(f.dir)) * v;
            }, 4, delay));
            fragments.add(f);
            dooms.add(new ObjDoom(f, 120));
        }
    }

    private final List<ObjScriptDelayed> fragmentDelays = new ArrayList<>();

    @Override
    protected void render() {
        // 按 depth 降序（大 depth 先画在底层）：logo_all/logo_prophecy 19900 → 卷轴 19850
        // → 心/碎片 19800 → black_all -999 → 第4章文字（obj_intro_ch4，depth 0）
        logoAll.drawSelf(this);
        logoProphecy.drawSelf(this);
        prophecyEffect.draw(this);
        logoHeart.drawSelf(this);
        for (ObjAfterimage a : afterimages) {
            a.drawSelf(this);
        }
        for (Frag f : fragments) {
            if (!f.destroyed) {
                drawSelf(f.x, f.y, f.sprite, f.imageIndex, f.imageXscale, f.imageYscale, 0xFFFFFF, 1f);
            }
        }
        for (ObjGroundshards g : groundshards) {
            g.drawSelf(this);
        }
        for (ObjMarker m : groundshardMarkers) {
            m.drawSelf(this);
        }
        blackAll.drawSelf(this);
        // obj_intro_ch4_Draw_0
        if (chapter4TextAlpha > 0) {
            drawAlpha = (float) chapter4TextAlpha;
            // draw_set_halign(fa_center)：文字中心对齐 room_center_x
            drawTextChapter(4, roomCenterX - 31, roomCenterY + 60, (float) chapter4TextAlpha);
            drawAlpha = 1f;
        }
    }

    /** 碎片（scr_marker_ext 的等价数据）。 */
    private static final class Frag {
        Spr sprite;
        int imageIndex;
        double x;
        double y;
        double direction;
        double dir;
        float speed;
        float gravity;
        float friction;
        float imageXscale = 1;
        float imageYscale = 1;
        float depth;
        double hspeed;
        double vspeed;
        boolean destroyed;
    }

    /** obj_doom：alarm 到点销毁目标。 */
    private static final class ObjDoom {
        private final Object target;
        private int frames;
        boolean dead;

        ObjDoom(Object target, int frames) {
            this.target = target;
            this.frames = frames;
        }

        void step() {
            if (frames-- <= 0) {
                if (target instanceof Frag f) {
                    f.destroyed = true;
                } else if (target instanceof ObjGroundshards g) {
                    g.destroyed = true;
                }
                dead = true;
            }
        }
    }

    /**
     * obj_texturescroller_prophecy 的 Draw_0：两层 pass —— 卷轴底纹/噪声只写 RGB
     * （alpha 保持 clear 的 0），标志只写 alpha 当蒙版，最后整层按 image_alpha 上屏。
     *
     * 性能：卷轴两层只在「标志蒙版覆盖到的区域」内算（其余像素 alpha 恒为 0、
     * 上屏时被跳过，绘制结果与原版全屏平铺等价）——原版每帧重铺 640x480 的
     * tiled 在 CPU 上会让演出掉到 10fps 以下，时间轴跟不上音频。
     */
    final class ObjTextureScrollerProphecy {
        Surface surfTextured;
        ObjMarker tileObject;
        float scrollSpeed = 1;
        double tick;
        boolean introMode;
        float imageAlpha = 1;

        void draw(IntroScene scene) {
            double cx = 0;   // camerax()
            double cy = 0;   // cameray()
            if (surfTextured == null) {
                surfTextured = new Surface(640, 480);
            }
            Surface prev = target;
            target = surfTextured;
            drawColor = 0xFFFFFF;
            drawAlpha = 1f;
            // draw_clear_alpha(c_white, 0)
            java.util.Arrays.fill(surfTextured.px, 0);
            tick += ((1.0 / 15) * scrollSpeed);
            double off = -((cx * 2) + (tick * 15)) * 0.5;
            // var _amt = sin((other.tick / 15) * (2 * pi)) * other.scroll_speed * 6（原版无 abs，是正负抖动）
            double amt = introMode ? Math.sin((tick / 15) * (2 * Math.PI)) * scrollSpeed * 6 : 0;
            ObjMarker tile = tileObject;
            if (tile != null && tile.visible && tile.sprite != null && tile.imageAlpha > 0f) {
                // 可见区域：标志帧（含 ±amt 重影）在 surface 上的覆盖矩形（amt 有符号，两侧都要外扩）
                int idx = tile.sprite.frameIndex((int) Math.floor(tile.imageIndex));
                int fw = tile.sprite.frameW(idx);
                int fh = tile.sprite.frameH(idx);
                double pad = Math.abs(amt);
                double left = tile.x - cx - tile.sprite.originX * tile.imageXscale - pad;
                double top = tile.y - cy - tile.sprite.originY * tile.imageYscale - pad;
                int x0 = (int) Math.max(0, Math.floor(left));
                int y0 = (int) Math.max(0, Math.floor(top));
                int x1 = (int) Math.min(639, Math.ceil(left + fw * tile.imageXscale + pad * 2));
                int y1 = (int) Math.min(479, Math.ceil(top + fh * tile.imageYscale + pad * 2));
                if (x1 >= x0 && y1 >= y0) {
                    drawScrollRegion(x0, y0, x1, y1, off);
                }
                gpuSetColorwrite(false, false, false, true);
                if (introMode) {
                    drawSelf(tile.x - cx - amt, tile.y - cy - amt, tile.sprite, (int) Math.floor(tile.imageIndex),
                            tile.imageXscale, tile.imageYscale, tile.imageBlend, tile.imageAlpha * 0.4f);
                    drawSelf(tile.x - cx + amt, tile.y - cy + amt, tile.sprite, (int) Math.floor(tile.imageIndex),
                            tile.imageXscale, tile.imageYscale, tile.imageBlend, tile.imageAlpha * 0.4f);
                }
                drawSelf(tile.x - cx, tile.y - cy, tile.sprite, (int) Math.floor(tile.imageIndex),
                        tile.imageXscale, tile.imageYscale, tile.imageBlend, tile.imageAlpha);
                gpuSetColorwrite(true, true, true, true);
            }
            target = prev;
            // draw_set_alpha(image_alpha); draw_surface(surf_textured, _cx, _cy)
            drawSurface(surfTextured, cx, cy, imageAlpha);
            drawAlpha = 1f;
        }

        /**
         * 在 surface 的指定矩形内算卷轴两层（等价于全屏 tiled）：
         * base = draw_sprite_tiled_ext(..., 2, 2, #42D0FF, 1)（只写 RGB），
         * noise = 同偏移平铺、bm_add、alpha = scr_wave(0, 0.4, 4, 0)。
         */
        private void drawScrollRegion(int x0, int y0, int x1, int y1, double off) {
            int[] basePx = scrollBase.frame(0);
            int[] noisePx = scrollNoise.frame(0);
            float wave = (float) (0.2 + 0.2 * Math.sin(System.currentTimeMillis() / 4000d * Math.PI * 2));
            int stepBaseW = 316 * 2;
            int stepBaseH = 238 * 2;
            int stepNoiseW = 256 * 2;
            int stepNoiseH = 256 * 2;
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) {
                    // base：2 倍平铺 + tint #42D0FF
                    int bxi = Math.floorMod((int) Math.round(x - off), stepBaseW) / 2;
                    int byi = Math.floorMod((int) Math.round(y - off), stepBaseH) / 2;
                    int argb = basePx[byi * 316 + bxi];
                    int r = ((argb >>> 16) & 0xFF) * 0x42 / 255;
                    int g = ((argb >>> 8) & 0xFF) * 0xD0 / 255;
                    int b = (argb & 0xFF) * 0xFF / 255;
                    // noise：同偏移 2 倍平铺 + bm_add
                    int nxi = Math.floorMod((int) Math.round(x - off), stepNoiseW) / 2;
                    int nyi = Math.floorMod((int) Math.round(y - off), stepNoiseH) / 2;
                    int nargb = noisePx[nyi * 256 + nxi];
                    float a = ((nargb >>> 24) / 255f) * wave;
                    if (a > 0f) {
                        r = Math.min(255, r + Math.round(((nargb >>> 16) & 0xFF) * 0x42 / 255f * a));
                        g = Math.min(255, g + Math.round(((nargb >>> 8) & 0xFF) * 0xD0 / 255f * a));
                        b = Math.min(255, b + Math.round((nargb & 0xFF) * 0xFF / 255f * a));
                    }
                    // 只写 RGB（alpha 由 gpu_set_colorwriteenable 保持为 clear 的 0）
                    surfTextured.px[y * 640 + x] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    /** obj_dw_church_prophecy_groundshards（Create_0/Step_0/Draw_0=draw_self）。 */
    final class ObjGroundshards {
        double x;
        double y;
        double ytarg = 320;
        int siner;
        double imageIndex;
        float imageAlpha = 1f;
        float depth;
        int fadeoutmode;             // Create_0：fadeoutmode = 0
        boolean destroyed;

        ObjGroundshards() {
            vspeed = 4;              // Create_0：vspeed = 4
            imageSpeed = 1;
            imageIndex = Math.random() * 30;
            imageXscale = 2;         // scr_size(2, 2)
            imageYscale = 2;
        }

        float imageSpeed;
        float imageXscale = 1;
        float imageYscale = 1;
        double vspeed;

        void step() {
            // 每帧内建：image_index += image_speed、y += vspeed
            imageIndex += imageSpeed;
            y += vspeed;
            siner++;
            if ((siner % 8) == 0) {
                // with (scr_afterimage()) { depth = other.depth + 1; scr_size(1, 1); }
                ObjAfterimage ai = new ObjAfterimage(groundShine);
                ai.x = x;
                ai.y = y;
                ai.imageIndex = (float) imageIndex;
                ai.imageSpeed = 0;
                ai.depth = depth + 1;
                ai.imageXscale = 1;
                ai.imageYscale = 1;
                afterimages.add(ai);
            }
            double mytarg = ytarg;
            if (fadeoutmode == 0) {
                if (y >= mytarg && Math.random() < 0.5) {
                    // scr_marker_ext(scr_even(x), scr_even(y), sprite_index, 2, 2, _, random(30), _, _, 1)
                    ObjMarker m = new ObjMarker(groundShine);
                    m.x = Math.round(x / 2) * 2;
                    m.y = Math.round(y / 2) * 2;
                    m.imageIndex = (float) (Math.random() * 30);
                    m.imageXscale = 2;
                    m.imageYscale = 2;
                    m.imageSpeed = 0;
                    groundshardMarkers.add(m);
                    destroyed = true;
                }
            }
        }

        void drawSelf(IntroScene scene) {
            if (!destroyed) {
                scene.drawSelf(x, y, groundShine, (int) Math.floor(imageIndex), imageXscale, imageYscale, 0xFFFFFF, imageAlpha);
            }
        }
    }

    private final List<ObjMarker> groundshardMarkers = new ArrayList<>();

    // 临时诊断用：定位「第一次播放不对」，定性后整段删
    private int traceTick;
    private int steppedFrames;
    private long firstStepMs = -1L;
    private int lastLoggedCon = -1;

    /** 临时诊断：con / 时钟 / 音频句柄 / 各层 alpha 一次打全。 */
    private void trace(String tag) {
        double pos = bgm >= 0 ? IntroSfx.position(bgm) : -1d;
        long el = firstStepMs < 0L ? 0L : System.currentTimeMillis() - firstStepMs;
        double fps = el > 0L ? steppedFrames * 1000.0 / el : 0d;
        System.out.println("[DioxideLite][DR][Intro][ch4] " + tag
                + " con=" + con + " timer=" + timer
                + " bgm=" + bgm + " pos=" + String.format("%.3f", pos)
                + " playing=" + IntroSfx.playing(bgm)
                + " bgmPos=" + String.format("%.3f", bgmPos)
                + " silent=" + bgmSilentFrames
                + " fps=" + String.format("%.1f", fps)
                + " heart=" + String.format("%.2f", logoHeart.imageAlpha)
                + " all=" + String.format("%.2f", logoAll.imageAlpha)
                + " prop=" + String.format("%.2f", logoProphecy.imageAlpha)
                + " scroll=" + String.format("%.2f", prophecyEffect.imageAlpha)
                + " frag=" + fragments.size() + "/" + afterimages.size()
                + " black=" + String.format("%.2f", blackAll.imageAlpha));
    }

    /** scr_wave(a0, a1, a2, a3)：原版全局脚本（卷轴噪声的 alpha 波动）。 */
    private static double scrWave(float a0, float a1, float a2, float a3) {
        double a4 = (a1 - a0) * 0.5;
        double t = System.currentTimeMillis() * 0.001;
        return a0 + a4 + Math.sin(((t + a2 * a3) / a2) * (2 * Math.PI)) * a4;
    }
}
