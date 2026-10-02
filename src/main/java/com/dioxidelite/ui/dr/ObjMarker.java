package com.dioxidelite.ui.dr;

/**
 * obj_marker 的转写：纯数据 + draw_self（对象无 Draw 事件时 GameMaker 自动 draw_self）。
 * scr_marker(x, y, sprite) 创建的实例就是这个。
 */
class ObjMarker {
    double x;
    double y;
    Spr sprite;
    float imageIndex;
    float imageSpeed;
    float imageXscale = 1f;
    float imageYscale = 1f;
    float imageAlpha = 1f;
    int imageBlend = 0xFFFFFF;   // c_white
    float depth;
    boolean visible = true;

    ObjMarker(Spr sprite) {
        this.sprite = sprite;
    }

    /** 每帧 image_index += image_speed（GameMaker 引擎侧）。 */
    void advance() {
        imageIndex += imageSpeed;
    }

    /** draw_self。 */
    void drawSelf(IntroScene scene) {
        if (visible && sprite != null) {
            scene.drawSelf(x, y, sprite, (int) Math.floor(imageIndex), imageXscale, imageYscale, imageBlend, imageAlpha);
        }
    }
}
