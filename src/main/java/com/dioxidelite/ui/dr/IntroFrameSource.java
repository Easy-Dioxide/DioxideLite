package com.dioxidelite.ui.dr;

/**
 * 开机演出的统一入口：帧数据 + 推进 + 跳过，DR 模式主菜单加载画面据此呈现。
 * 实现都是 {@link IntroScene} 的子类：ch1 是 PROCESS_LOGO 的逐行复刻
 * （{@link Ch1ProcessLogo}，原版 room_speed = 15），ch2~ch5 是各章 obj_intro_chN
 * 的 GML 逐行翻译（{@link Ch2Intro} / {@link Ch3Intro} / {@link Ch4Intro} / {@link Ch5Intro}）。
 */
public interface IntroFrameSource {
    /** 当前帧（RGBA）；调用方拿到的是内部缓冲，只读。 */
    byte[] frame();

    /** 帧宽度（ch1/DR 老管线 320，ch4/ch5 的房间是 640）。 */
    default int width() {
        return 320;
    }

    /** 帧高度（ch1/DR 老管线 240，ch4/ch5 的房间是 480）。 */
    default int height() {
        return 240;
    }

    boolean isFinished();

    /** 推进动画到 now。 */
    void advance(long nowMs);

    /** 按下跳过键。返回 true 表示这一下生效（调用方据此触发音效淡出）。 */
    boolean requestSkip();
}
