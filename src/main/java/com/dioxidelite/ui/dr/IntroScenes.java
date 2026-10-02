package com.dioxidelite.ui.dr;


/**
 * 按章节（启动动画附加项的 1~5）创建对应的开机演出。
 * 每章一个类，内容是反编译 GML 的逐行转写（Ch1ProcessLogo / Ch2Intro / … / Ch5Intro）。
 * 「已存档分支」开关决定各章走 files_exist 的哪条分支。
 */
public final class IntroScenes {
    private IntroScenes() {
    }

    public static IntroFrameSource create(int chapter) {
        int ch = Math.max(1, Math.min(DrTheme.CHAPTERS.length, chapter));
        boolean savedFlavor = DrThemeState.drIntroSavedFlavor;
        IntroScene scene = switch (ch) {
            // ch1 原版开机就是 PROCESS_LOGO（该章没有 intro 演出房）
            case 1 -> new Ch1ProcessLogo(savedFlavor);
            case 2 -> new Ch2Intro(savedFlavor);
            case 3 -> new Ch3Intro(savedFlavor);
            case 4 -> new Ch4Intro(savedFlavor);
            default -> new Ch5Intro(savedFlavor);
        };
        scene.frameMs = frameMs(ch);
        return scene;
    }

    /**
     * 该章原版演出的每帧毫秒数：ch1 的 PROCESS_LOGO 在 Create_0 里写了 room_speed = 15，
     * 其余章的房间走 30fps 默认值（没有任何 ch2~5 的演出对象改过 room_speed）。
     */
    public static int frameMs(int chapter) {
        return Math.max(1, Math.min(DrTheme.CHAPTERS.length, chapter)) == 1 ? 66 : 33;
    }

    /** ch1 的 PROCESS_LOGO 需要底噪（AUDIO_INTRONOISE）。 */
    public static boolean usesIntronoise(int chapter) {
        return Math.max(1, Math.min(DrTheme.CHAPTERS.length, chapter)) == 1;
    }

    /** 是否是演出章（ch2~5）：跳过/音效淡出的收尾走 IntroSfx。 */
    public static boolean isCinematic(int chapter) {
        return !usesIntronoise(chapter);
    }
}
