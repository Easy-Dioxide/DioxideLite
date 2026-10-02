package com.dioxidelite.ui.dr;

import com.dioxidelite.audio.AudioManager;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

/**
 * 各章开机演出的音效/音乐，全部交给 AudioManager 混音。
 * 音源清单沿用原型 14 条（13 个演出音效 + ch1 的底噪）。
 */
public final class IntroSfx {

    private record Resource(String name, String resource) {
    }

    private static final String SFX_RESOURCE = "/assets/dioxide-lite/mainmenu/dr/intro/sfx/";

    private static final Resource[] RESOURCES = {
            new Resource("queen_bitcrush.ogg", SFX_RESOURCE + "queen_bitcrush.ogg"),
            new Resource("explosion.ogg", SFX_RESOURCE + "explosion.ogg"),
            new Resource("queen_laugh_title.ogg", SFX_RESOURCE + "queen_laugh_title.ogg"),
            new Resource("noise.wav", SFX_RESOURCE + "noise.wav"),
            new Resource("its_tv_time.ogg", SFX_RESOURCE + "its_tv_time.ogg"),
            new Resource("tv_static.ogg", SFX_RESOURCE + "tv_static.ogg"),
            new Resource("crowd_cheer.ogg", SFX_RESOURCE + "crowd_cheer.ogg"),
            new Resource("ch4_first_intro.ogg", SFX_RESOURCE + "ch4_first_intro.ogg"),
            new Resource("ch4_breaking.ogg", SFX_RESOURCE + "ch4_breaking.ogg"),
            new Resource("break1.wav", SFX_RESOURCE + "break1.wav"),
            new Resource("glassbreak.ogg", SFX_RESOURCE + "glassbreak.ogg"),
            new Resource("punchmed.ogg", SFX_RESOURCE + "punchmed.ogg"),
            new Resource("ch5_logo.ogg", SFX_RESOURCE + "ch5_logo.ogg"),
            new Resource("intronoise.ogg", "/assets/dioxide-lite/mainmenu/dr/intro/intronoise.ogg"),
    };

    private static volatile boolean prepared;

    private IntroSfx() {
    }

    static Path audioDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("dioxide-lite").resolve("drmenu");
    }

    /** 注册整组音源并预热解码；DrSound 的菜单音源也在这里一起登记。 */
    public static synchronized void prepare() {
        if (prepared) {
            return;
        }
        prepared = true;
        AudioManager.setExternalDirectory(audioDir());
        for (Resource r : RESOURCES) {
            AudioManager.register(r.name(), r.resource());
        }
        DrSound.registerSources();
        AudioManager.prepare();
        AudioManager.warmDevice();
    }

    private static void ensure() {
        if (!prepared) {
            prepare();
        }
    }

    public static int play(String name, float gain) {
        ensure();
        return AudioManager.play(name, gain);
    }

    public static int playLoop(String name, float gain) {
        ensure();
        return AudioManager.playLoop(name, gain);
    }

    public static int playLoopPitch(String name, float gain, double pitch) {
        ensure();
        return AudioManager.playLoopPitch(name, gain, pitch);
    }

    public static double position(int handle) {
        return AudioManager.position(handle);
    }

    public static boolean playing(int handle) {
        return AudioManager.playing(handle);
    }

    public static boolean finished(int handle) {
        return AudioManager.finished(handle);
    }

    public static void stop(int handle) {
        AudioManager.stop(handle);
    }

    public static double duration(String name, double fallback) {
        ensure();
        return AudioManager.duration(name, fallback);
    }

    public static void fadeOut(int handle, int ms) {
        AudioManager.fadeOut(handle, ms);
    }

    public static void fadeOutAll(int ms) {
        AudioManager.fadeOutAll(ms);
    }

    public static void stopAll() {
        AudioManager.stopAll();
    }
}
