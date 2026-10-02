package com.dioxidelite.ui.dr;

/**
 * ch1 开机演出：PROCESS_LOGO 的 Create_0/Draw_0 逐行转写
 * （PLACE_LOGO 房间 320x240，view width/height=320x240、port 640x480：
 *  PROCESS_LOGO_Create_0 里 x = (view_w/2)-(w/2) = 48，y = (view_h/2)-(h/2)-10 = 93）。
 *
 * 原版结构：PHASE 0 按 2px 横条切 logo、每条画 3 层相位 0/+0.6/+1.2 的错位拷贝；
 * PHASE 1 定格 30 帧（PHASETIMER 在 Draw 里递增）；PHASE 2 整标志 AB 渐隐、
 * 中心图案四象限散开（偏移 = sin((siner/8)+(i/2)) * (i*factor2)）、心 AA 渐隐；
 * AA<=-0.5 进 PLACE_MENU。跳过：button1_p → obj_fadeout(0.04/帧) + 底噪淡出 20 帧，
 * skiptimer>=28 恢复 30fps、>=30 进菜单。
 *
 * Create_0 里那句 room_speed = 15 由 {@link IntroScenes#frameMs} 还原成 66ms/帧，
 * 整段是 15fps（PHASE 1 的 30 帧定格因此是 2 秒）。本类的推进一步在 render() 里
 * （原版 PROCESS_LOGO 没有 Step 事件，全部逻辑在 Draw_0），而 render() 每次
 * {@link IntroScene#advance} 只跑一次，所以帧率就是 frameMs。
 */
public final class Ch1ProcessLogo extends IntroScene {
    private static boolean loaded;
    private static Spr logo;
    private static Spr centerLogo;
    private static Spr centerHeart;

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        logo = Spr.of1("IMAGE_LOGO", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/logo.png");
        centerLogo = Spr.of1("IMAGE_LOGO_CENTER", 112, 17, "/assets/dioxide-lite/mainmenu/dr/intro/center.png");
        centerHeart = Spr.of1("IMAGE_LOGO_CENTER_HEART", 0, 0, "/assets/dioxide-lite/mainmenu/dr/intro/heart.png");
    }

    // ---- Create_0 ----
    private final int w;
    private final int h;
    private double x;
    private double y;
    private final double inity;
    private final double mid;
    private int phase;         // PHASE
    private int phaseTimer;    // PHASETIMER
    private int phasePlus;     // PHASEPLUS
    private double siner;
    private double factor = 1;
    private double factor2;
    private double aa = 1;     // AA
    private double ab = 1;     // AB
    private int ingame;
    private int skipped;
    private int skipTimer;
    private int noise;         // NOISE = snd_play(AUDIO_INTRONOISE)
    private double mina;

    public Ch1ProcessLogo(boolean savedFlavor) {
        super(320, 240);
        load();
        // Create_0
        w = logo.width();
        h = logo.height();
        noise = sndPlay("intronoise.ogg", 1f);
        siner = 0;
        factor = 1;
        factor2 = 0;
        mid = h / 2.0;
        x = (viewW / 2.0) - (w / 2.0);
        y = (viewH / 2.0) - (h / 2.0) - 10;
        inity = y;
        phase = 0;
        phaseTimer = 0;
        phasePlus = 0;
        aa = 1;
        ab = 1;
        // ingame = (global.plot != 0)：mod 里用「有无存档」开关代替存档状态
        ingame = savedFlavor ? 1 : 0;
        skipped = 0;
        skipTimer = 0;
        // 原版这里还设 room_speed = 15 / draw_screen = true；帧调度由宿主固定 30fps 处理
    }

    @Override
    protected void onSkipRequested() {
        // 原版：fade = instance_create(0,0,obj_fadeout); fade.fadespeed = 0.04; snd_volume(NOISE, 0, 20)
        skipped = 1;
        sndVolumeFade(noise, 20);
    }

    @Override
    protected void step() {
        // PROCESS_LOGO 的 Step 事件在原版里是空的（全部逻辑在 Draw_0），这里同样留空
    }

    @Override
    protected void render() {
        // ---- Draw_0 ----
        if (phase == 0) {
            siner += 1;
            factor -= (0.003 + (siner / 900));
            if (factor < 0) {
                factor = 0;
                phase = 1;
            }
            double alpha = (1 - factor) / 2;
            for (int i = 0; i < h; i += 1) {
                double xoff = 40 * Math.sin((siner / 5) + (i / 3.0)) * factor;
                double xoff2 = 40 * Math.sin((siner / 5) + (i / 3.0) + 0.6) * factor;
                double xoff3 = 40 * Math.sin((siner / 5) + (i / 3.0) + 1.2) * factor;
                drawSpritePartExt(logo, 0, 0, i, w, 2, x + xoff, y + i, 1f, 1f, 0xFFFFFF, (float) alpha);
                drawSpritePartExt(logo, 0, 0, i, w, 2, x + xoff2, y + i, 1f, 1f, 0xFFFFFF, (float) alpha);
                drawSpritePartExt(logo, 0, 0, i, w, 2, x + xoff3, y + i, 1f, 1f, 0xFFFFFF, (float) alpha);
            }
        }
        if (phase == 1) {
            drawSelf(x, y, logo, 0, 1f, 1f, 0xFFFFFF, 1f);
            phaseTimer += 1;
            if (phaseTimer >= 30) {
                siner = 0;
                factor = 0;
                phase = 2;
            }
        }
        if (phase == 2) {
            if (phasePlus == 0) {
                siner += 0.5;
            }
            if (siner >= 20) {
                phasePlus = 1;
            }
            if (phasePlus == 1) {
                siner += 0.5;
                aa -= 0.02;
                ab -= 0.08;
            }
            drawSpriteExt(x, y, logo, 0, 1f, 1f, 0xFFFFFF, (float) ab);
            mina = siner / 30;
            if (mina >= 0.14) {
                mina = 0.14;
            }
            factor2 += 0.05;
            for (int i = 0; i < 10; i += 1) {
                double offX = Math.sin((siner / 8) + (i / 2.0)) * (i * factor2);
                double offY = Math.cos((siner / 8) + (i / 2.0)) * (i * factor2);
                drawSpriteExt((x + (w / 2.0)) - offX, (y + (h / 2.0)) - offY, centerLogo, 0, 1f, 1f, 0xFFFFFF, (float) (mina * aa));
                drawSpriteExt(x + (w / 2.0) + offX, (y + (h / 2.0)) - offY, centerLogo, 0, 1f, 1f, 0xFFFFFF, (float) (mina * aa));
                drawSpriteExt((x + (w / 2.0)) - offX, y + (h / 2.0) + offY, centerLogo, 0, 1f, 1f, 0xFFFFFF, (float) (mina * aa));
                drawSpriteExt(x + (w / 2.0) + offX, y + (h / 2.0) + offY, centerLogo, 0, 1f, 1f, 0xFFFFFF, (float) (mina * aa));
            }
            drawSpriteExt(x, y, centerHeart, 0, 1f, 1f, 0xFFFFFF, (float) aa);
            if (aa <= -0.5 && skipped == 0) {
                finished = true;   // room_goto(PLACE_MENU)（ingame=1 的 room_ed 分支 mod 不需要）
            }
        }
        // 跳过黑幕（obj_fadeout）由宿主的 mod 跳过层绘制，这里推进 skiptimer 语义
        if (skipped == 1) {
            skipTimer += 1;
        }
    }
}
