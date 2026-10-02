package com.dioxidelite.ui.dr;

import com.dioxidelite.audio.AudioManager;

/**
 * DELTARUNE 主菜单音频：menu.ogg 循环 BGM + 四个菜单音效。
 * 与演出音效共用 AudioManager 的混音线与外部替换目录。
 */
public final class DrSound {

    /** 0 dB 的 -12dB 增益，与原型两路音量保持一致。 */
    private static final float GAIN = 0.251f;
    private static final String BGM = "menu.ogg";
    private static final String SFX_RESOURCE = "/assets/dioxide-lite/mainmenu/dr/sfx/";

    public enum Sfx {
        MOVE("snd_menumove.wav"),
        SELECT("snd_select.wav"),
        BACK("snd_swing.wav"),
        ERROR("snd_error.wav");

        private final String file;

        Sfx(String file) {
            this.file = file;
        }
    }

    private static volatile int bgmHandle = -1;
    private static volatile boolean registered;

    private DrSound() {
    }

    static synchronized void registerSources() {
        if (registered) {
            return;
        }
        registered = true;
        AudioManager.register(BGM, SFX_RESOURCE + BGM);
        for (Sfx sfx : Sfx.values()) {
            AudioManager.register(sfx.file, SFX_RESOURCE + sfx.file);
        }
    }

    public static void play(Sfx sfx) {
        IntroSfx.prepare();
        AudioManager.play(sfx.file, GAIN);
    }

    public static void startBgm() {
        IntroSfx.prepare();
        if (bgmHandle >= 0 && AudioManager.playing(bgmHandle)) {
            return;
        }
        bgmHandle = AudioManager.playLoop(BGM, GAIN);
    }

    public static void stopBgm() {
        int handle = bgmHandle;
        bgmHandle = -1;
        if (handle >= 0) {
            // 菜单 BGM 收尾短淡出，避免切屏时硬切
            AudioManager.fadeOut(handle, 240);
        }
    }
}
