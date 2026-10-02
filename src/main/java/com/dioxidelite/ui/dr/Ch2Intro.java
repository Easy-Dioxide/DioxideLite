package com.dioxidelite.ui.dr;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * obj_intro_ch2 的 Create_0/Step_0/Draw_0 逐行转写
 * （room_intro_ch2：camera 320x240，port 640x480 → view_wport[0]/4 = 160、view_hport[0]/4 = 120）。
 *
 * 有存档：Queen 线框降落 → 定格 → 大笑 → 爆炸 → 淡出（Step_0 第 13~50 行）
 * 无存档：黑屏 75 帧 → DELTARUNE 9 块碎片按 0,8,1,7,2,6,3,5,4 顺序点亮 + 短噪声 →
 *         120 帧起 Queen 笑声 +「第2章」→ 240 帧进菜单（Draw_0 第 16~93 行）
 * 分支由「有无存档」设置（UI 里的章节开关）决定，等价原版 files_exist。
 */
public final class Ch2Intro extends IntroScene {
    private static boolean loaded;
    private static Spr queenRotate;
    private static Spr queenIdle;
    private static Spr queenLaugh;
    private static Spr queenExplode;
    private static Spr logoPieces;

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        queenRotate = Spr.of("spr_queen_wireframe_rotate", 39, 53, "/assets/dioxide-lite/mainmenu/dr/intro/ch2/queen_rotate_", 8);
        queenIdle = Spr.of("spr_queen_wireframe", 39, 53, "/assets/dioxide-lite/mainmenu/dr/intro/ch2/queen_idle_", 3);
        queenLaugh = Spr.of("spr_queen_wireframe_laugh", 39, 53, "/assets/dioxide-lite/mainmenu/dr/intro/ch2/queen_laugh_", 2);
        queenExplode = Spr.of("spr_queen_wireframe_explode", 64, 64, "/assets/dioxide-lite/mainmenu/dr/intro/ch2/queen_explode_", 6);
        logoPieces = Spr.of("IMAGE_LOGO_CENTER_SEPARATE", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/ch2/logo_pieces_", 9);
    }

    private static final Random RND = new Random();
    /** view_wport[0] / view_hport[0]（端口 640x480）。 */
    private static final int VIEW_WPORT0 = 640;
    private static final int VIEW_HPORT0 = 480;

    // ---- Create_0 ----
    private int con;
    private int timer;
    private final boolean filesExist;
    private boolean showQueen;
    private Spr queenSprite = queenRotate;
    private double queenSpriteIndex;
    private int queenSiner;
    private boolean queenAnimate = true;
    private double queenYPos = -100;
    private double queenAlpha;
    private int init;
    private int type;
    // Draw_0（无存档分支）
    private int logotimer;
    private int logopieces;
    private final int[] drawlogopart = new int[9];
    private final List<ObjLerpVar> lerps = new ArrayList<>();

    public Ch2Intro(boolean savedFlavor) {
        super(320, 240);
        load();
        // obj_intro_ch2_Create_0
        con = 0;
        timer = 0;
        IntroSfx.stopAll();   // snd_free_all()
        filesExist = savedFlavor;
        showQueen = false;
        queenSprite = queenRotate;
        queenSpriteIndex = 0;
        queenSiner = 0;
        queenAnimate = true;
        queenYPos = -100;
        queenAlpha = 0;
        init = 0;
        type = 0;
    }

    /** scr_lerpvar("var", a, b, maxtime[, easetype, easeinout])：对自身变量补间。 */
    private void lerpVar(String varname, double a, double b, int maxtime, int easetype, String easeinout) {
        switch (varname) {
            case "queen_y_pos" -> lerps.add(new ObjLerpVar(v -> queenYPos = v, null, a, b, maxtime, easetype, easeinout));
            case "queen_alpha" -> lerps.add(new ObjLerpVar(v -> queenAlpha = v, null, a, b, maxtime, easetype, easeinout));
            default -> {
            }
        }
    }

    @Override
    protected void step() {
        // obj_intro_ch2_Step_0
        for (ObjLerpVar l : lerps) {
            l.step();
        }
        lerps.removeIf(l -> l.dead);
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
        if (!showQueen) {
            showQueen = true;
        }
        if (showQueen) {
            timer++;
            if (timer == 30) {
                queenSprite = queenRotate;
                // scr_lerpvar("queen_y_pos", -150, view_hport[0]/4, 15, -1, "out")
                lerpVar("queen_y_pos", -150, VIEW_HPORT0 / 4.0, 15, -1, "out");
                // scr_lerpvar("queen_alpha", 0, 1, 10)
                lerpVar("queen_alpha", 0, 1, 10, 0, null);
            }
            if (timer == 50) {
                queenSprite = queenIdle;
                queenAnimate = false;
            }
            if (timer == 80) {
                queenSprite = queenLaugh;
                queenAnimate = true;
                sndPlay("queen_bitcrush.ogg", 1f);   // snd_play(snd_queen_bitcrushlaugh)
            }
            if (timer == 130) {
                sndPlay("explosion.ogg", 1f);        // snd_play(snd_explosion_mmx3)
                queenSprite = queenExplode;
            }
            if (timer == 170) {
                lerpVar("queen_alpha", 1, 0, 10, 0, null);
            }
            if (timer == 200) {
                con = 99;
                finished = true;                     // room_goto(PLACE_MENU)
            }
        }
    }

    @Override
    protected void render() {
        // obj_intro_ch2_Draw_0
        if (showQueen) {
            if (queenAnimate) {
                queenSiner++;
                queenSpriteIndex = queenSiner / 3;   // image_speed=0，帧号 = siner/3
            } else {
                queenSpriteIndex = 0;
            }
            drawSpriteExt(VIEW_WPORT0 / 4.0, queenYPos, queenSprite, (int) Math.floor(queenSpriteIndex),
                    1f, 1f, 0xFFFFFF, (float) queenAlpha);
            return;
        }
        if (init == 0 && con == 1) {
            logopieces = 0;
            init = 1;
            for (int i = 0; i < 9; i++) {
                drawlogopart[i] = 0;
            }
            con = 2;
            logotimer = 0;
        }
        if (con == 2) {
            logotimer++;
            if ((logotimer % 8) == 0 && logotimer < 80) {
                // snd_play_pitch(snd_noise, 0.8, (0.5 + random(1)))：变调未复刻，固定 0.8
                sndPlay("noise.wav", 0.8f);
                switch (logopieces) {
                    case 0 -> drawlogopart[0] = 1;
                    case 1 -> drawlogopart[8] = 1;
                    case 2 -> drawlogopart[1] = 1;
                    case 3 -> drawlogopart[7] = 1;
                    case 4 -> drawlogopart[2] = 1;
                    case 5 -> drawlogopart[6] = 1;
                    case 6 -> drawlogopart[3] = 1;
                    case 7 -> drawlogopart[5] = 1;
                    case 8 -> drawlogopart[4] = 1;
                    default -> {
                    }
                }
                logopieces++;
            }
            for (int i = 0; i < 9; i++) {
                if (drawlogopart[i] == 1) {
                    // draw_sprite_ext(IMAGE_LOGO_CENTER_SEPARATE, i, 160, 100, 1, 1, 0, c_white, 1)
                    drawSpriteExt(160, 100, logoPieces, i, 1f, 1f, 0xFFFFFF, 1f);
                }
            }
            if (logotimer == 120) {
                sndPlay("queen_laugh_title.ogg", 1f);   // snd_play(snd_queen_laugh_title)
            }
            if (logotimer >= 120) {
                // draw_text_ext(130, 120, "第2章", 10, 900)
                drawTextChapter(2, 130, 120, 1f);
            }
            if (logotimer >= 240) {
                finished = true;                        // room_goto(PLACE_MENU)
            }
        }
    }
}
