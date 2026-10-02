package com.dioxidelite.ui.dr;

/**
 * obj_marker_fancy 的转写：Create 里挂三个函数钩子（step_func / draw_func / end_step_func），
 * Step_0 调 step_func、Step_2 调 end_step_func、Draw_0 调 draw_func；
 * 默认 draw_func 是「visible 时 draw_self」。
 * offset / timer / timerPace / sparkling / main_alpha / fadespeed / fadeoffset 是
 * ch5 在这些实例上写的动态变量。
 */
class ObjMarkerFancy extends ObjMarker {
    Runnable stepFunc = () -> {
    };
    Runnable endStepFunc = () -> {
    };
    /** 默认 draw_func 是「visible 时 draw_self」——由宿主 render 里按 depth 顺序调 drawSelf 完成；
     *  ch5 给 logoAll 换成 logoShoujoDraw（套 shader 再 draw_self）时会覆盖这个钩子。 */
    Runnable drawFunc = () -> {
    };
    float offset;
    float timer;
    float timerPace;
    boolean sparkling;
    float mainAlpha;
    float fadeSpeed;
    float fadeOffset;
    // GameMaker 的内建运动变量（bigSparkleStep 里用）
    float speed;
    float direction;

    ObjMarkerFancy(Spr sprite) {
        super(sprite);
    }
}
