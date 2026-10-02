package com.dioxidelite.ui.dr;

/**
 * obj_intro_ch3 + obj_intro_tv_time 的 Create/Step/Draw 逐行转写
 * （room_intro 320x240，port 640x320 → view_wport[0]/4 = 160）。
 *
 * 有存档：75 帧后 obj_intro_tv_time 起播 IT'S TV TIME（164 帧动画按音频进度同步），
 *         105 帧后可取消（fade_time_max 缩到 60），tv_time_max+90+30 帧进菜单
 * 无存档：Draw_0 的 con==1 分支：黑底 logo + 静电遮罩逐块揭示 + 190 帧欢呼/「第3章」+
 *         280 帧黑幕 + 370 帧进菜单
 */
public final class Ch3Intro extends IntroScene {
    private static boolean loaded;
    private static Spr tvIntro;
    private static Spr tvLoop;
    private static Spr staticEffect;
    private static Spr centerLogo;

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        tvIntro = Spr.of("spr_dw_tv_time_intro", 160, 120, "/assets/dioxide-lite/mainmenu/dr/intro/ch3/tv_", 164);
        tvLoop = Spr.of("spr_dw_tv_time_intro_loop", 160, 120, "/assets/dioxide-lite/mainmenu/dr/intro/ch3/tvloop_", 2);
        staticEffect = Spr.of("spr_static_effect", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/ch3/static_", 8);
        centerLogo = Spr.of1("IMAGE_LOGO_CENTER", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/logo/center.png");
    }

    // ---- Create_0 ----
    private int con;
    private int timer;
    private final boolean filesExist;
    private int init;
    private int logotimer;
    private double staticAnim;
    /** logo_piece[i] = {x, char_width, y_offset}（Create_0 第 13~39 行）。 */
    private static final int[][] LOGO_PIECE = {
            {48, 21, 0}, {252, 19, 10}, {76, 19, 10}, {226, 19, 10}, {102, 7, 0},
            {200, 19, 10}, {116, 19, 0}, {174, 19, 10}, {142, 25, 10},
    };
    private static final int CHAR_Y_POS = 83;
    private static final int CHAR_HEIGHT = 33;
    private int maxLogoPieces;
    private int revealedPieces;
    private final int[] cheerTrack = {-1, -1, -1};
    private double fadeAlpha;
    private int fadeTimeMax = 90;
    private int tvTimer;
    private int tvTimeMax = 240;
    private boolean isCanceled;
    private boolean chapterDisplay;    // Create_0：chapter_display = false
    private boolean showOverlay = true; // Create_0：show_overlay = true
    // fade_out() 的补间与 tv 实例
    private ObjLerpVar fadeLerp;
    private ObjTvTime tvTimeVfx;

    public Ch3Intro(boolean savedFlavor) {
        super(320, 240);
        load();
        // obj_intro_ch3_Create_0
        con = 0;
        timer = 0;
        IntroSfx.stopAll();   // snd_free_all()
        filesExist = savedFlavor;
        init = 0;
        logotimer = 0;
        staticAnim = 0;
        maxLogoPieces = 0;
        revealedPieces = 0;
        fadeAlpha = 0;
        fadeTimeMax = 90;
        tvTimer = 0;
        tvTimeMax = 240;
        isCanceled = false;
        chapterDisplay = false;
        showOverlay = true;
    }

    /** play_cheer()：三条欢呼轨（audio_play_sound(snd_crowd_cheer_single, 50, true)，gain 0.8，pitch 0.7 + 0.1i）。 */
    private void playCheer() {
        for (int i = 0; i < cheerTrack.length; i++) {
            cheerTrack[i] = sndPlayLoopPitch("crowd_cheer.ogg", 0.8f, 0.7 + 0.1 * i);
        }
    }

    /** fade_out()：scr_lerpvar("fade_alpha", 0, 1, fade_time_max) + 欢呼轨淡出。 */
    private void fadeOut() {
        fadeLerp = new ObjLerpVar(v -> fadeAlpha = v, null, 0, 1, fadeTimeMax, 0, null);
        for (int track : cheerTrack) {
            if (track >= 0) {
                sndVolumeFade(track, fadeTimeMax);   // mus_volume(track, 0, fade_time_max)
            }
        }
    }

    /** exit_screen()：snd_free(cheer_track[i]) + room_goto(PLACE_MENU)。 */
    private void exitScreen() {
        for (int track : cheerTrack) {
            if (track >= 0) {
                sndFree(track);
            }
        }
        finished = true;
    }

    @Override
    protected void step() {
        if (fadeLerp != null) {
            fadeLerp.step();
            if (fadeLerp.dead) {
                fadeLerp = null;
            }
        }
        if (tvTimeVfx != null) {
            tvTimeVfx.step();
        }
        // obj_intro_ch3_Step_0
        if (con != 0) {
            return;
        }
        if (!filesExist) {
            timer++;
            if (timer == 75) {
                con = 1;
            }
            return;
        }
        tvTimer++;
        if (tvTimer == 75) {
            tvTimeVfx = new ObjTvTime();
        }
        if (!isCanceled && tvTimer >= 105) {
            if (button1P() || button2P()) {
                isCanceled = true;
                tvTimeMax = tvTimer;
                fadeTimeMax = 60;
                int fadeTime = fadeTimeMax;
                if (tvTimeVfx != null) {
                    sndVolumeFade(tvTimeVfx.tvSound, fadeTime);   // with (tv_time_vfx) snd_volume(tv_sound, 0, _fade_time)
                }
            }
        }
        if (tvTimer == tvTimeMax) {
            isCanceled = true;
            fadeOut();
        }
        if (tvTimer == (tvTimeMax + fadeTimeMax + 30)) {
            exitScreen();
        }
    }

    @Override
    protected void render() {
        // obj_intro_ch3_Draw_0
        if (con == 1) {
            logotimer++;
            drawSpriteExt(160, 100, centerLogo, 0, 1f, 1f, 0xFFFFFF, 1f);
            if ((logotimer % 15) == 1) {
                if (maxLogoPieces < LOGO_PIECE.length) {
                    if (maxLogoPieces == 0) {
                        // snd_stop(snd_tv_static); snd_loop(snd_tv_static)
                        if (staticSound >= 0) {
                            sndFree(staticSound);
                        }
                        staticSound = IntroSfx.playLoop("tv_static.ogg", 0.7f);
                    }
                    maxLogoPieces++;
                }
                if (maxLogoPieces > 1 && revealedPieces < LOGO_PIECE.length) {
                    revealedPieces++;
                    if (revealedPieces == LOGO_PIECE.length) {
                        sndFree(staticSound);   // snd_stop(snd_tv_static)
                        staticSound = -1;
                    }
                }
            }
            if (logotimer == 190) {
                playCheer();
            }
            if (logotimer >= 190) {
                // draw_text_ext(camerax()+130, cameray()+120, "第3章", 10, 900)
                drawTextChapter(3, 130, 120, 1f);
            }
            if (logotimer == 280) {
                fadeOut();
            }
            if (logotimer == 370) {
                exitScreen();
            }
            // 遮罩：revealed..max 之间的块画静电雪花（Draw_0 第 56~62 行的 mask 段），
            // 未激活块（max 起）画黑（第 63~69 行）
            staticAnim += 0.4;
            for (int i = revealedPieces; i < LOGO_PIECE.length; i++) {
                int xx = LOGO_PIECE[i][0];
                int cw = LOGO_PIECE[i][1];
                int yy = CHAR_Y_POS + LOGO_PIECE[i][2];
                // mask 矩形的 y 范围是 [char_y_pos + offset, char_y_pos + char_height]：
                // 起点跟着 offset 走、终点不跟，所以高度得减掉它，
                // 否则 offset=10 的块会往下多铺 10 行雪花。
                int bh = CHAR_Y_POS + CHAR_HEIGHT - yy;
                if (i < maxLogoPieces) {
                    drawStaticOver(staticEffect, xx, yy, cw, bh);
                } else {
                    drawColor = 0x000000;
                    drawAlpha = 1f;
                    drawRectangle(xx, yy, xx + cw, CHAR_Y_POS + CHAR_HEIGHT);
                }
            }
            drawColor = 0xFFFFFF;
        }
        // draw_set_alpha(fade_alpha); ossafe_fill_rectangle(-10, -10, room_width + 10, room_height + 10)
        drawAlpha = (float) fadeAlpha;
        drawColor = 0x000000;
        drawRectangle(-10, -10, viewW + 10, viewH + 10);
        drawAlpha = 1f;
        drawColor = 0xFFFFFF;
        // tv_time_vfx 的 logo_marker（同 depth，后创建 → 画在最上）
        if (tvTimeVfx != null && tvTimeVfx.marker != null) {
            tvTimeVfx.marker.drawSelf(this);
        }
    }

    private int staticSound = -1;

    /**
     * mask 内的静电：原版把 128x128 的雪花整张画在 (40,0) 与 (150,0)、
     * 只有落在 mask（这些块）内的像素显示；这里直接对块内像素取雪花对应位置的值，
     * 第二张覆盖第一张。
     */
    private void drawStaticOver(Spr snow, int bx, int by, int bw, int bh) {
        int frame = snow.frameIndex((int) Math.floor(staticAnim));
        int[] px = snow.frame(frame);
        int fw = snow.frameW(frame);
        int fh = snow.frameH(frame);
        for (int y = by; y < by + bh; y++) {
            if (y < 0 || y >= viewH) {
                continue;
            }
            for (int x = bx; x < bx + bw; x++) {
                if (x < 0 || x >= viewW) {
                    continue;
                }
                int sx = -1;
                if (x >= 150 && x < 150 + fw) {
                    sx = x - 150;
                } else if (x >= 40 && x < 40 + fw) {
                    sx = x - 40;
                }
                if (sx < 0 || y >= fh) {
                    continue;
                }
                setPixel(x, y, px[y * fw + sx], 1f);
            }
        }
    }

    /** button2_p。 */
    private static boolean button2P() {
        long w = net.minecraft.client.Minecraft.getInstance().getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_X) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    /** obj_intro_tv_time：logo_marker = scr_marker(view_wport[0]/4, cameray()+110, spr_dw_tv_time_intro)。 */
    final class ObjTvTime {
        final ObjMarker marker = new ObjMarker(tvIntro);
        int tvSound;
        boolean audioFinish;

        ObjTvTime() {
            marker.x = 640 / 4.0;      // view_wport[0]/4（端口 640）
            marker.y = 0 + 110;        // cameray() + 110
            marker.imageSpeed = 0;
            tvSound = sndPlay("its_tv_time.ogg", 1f);   // audio_play_sound(snd_its_tv_time, 50, 0)
            audioFinish = false;
        }

        /** obj_intro_tv_time_Step_0。 */
        void step() {
            marker.advance();
            if (audioFinish) {
                return;
            }
            if (IntroSfx.playing(tvSound)) {   // audio_is_playing(tv_sound)
                // audio_sound_get_track_position / length：起线窗口内 position 暂为 -1，原版此时按 0
                double trackProgress = Math.max(0d, sndPosition(tvSound));
                double normal = trackProgress / sndLength("its_tv_time.ogg", 5.53d);
                marker.imageIndex = (float) (tvIntro.frameCount() * normal);
            } else {
                audioFinish = true;
                marker.sprite = tvLoop;
                marker.imageSpeed = 0.1f;
                sndFree(tvSound);      // snd_free(51)
            }
        }
    }
}
