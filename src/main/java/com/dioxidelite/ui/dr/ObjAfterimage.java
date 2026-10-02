package com.dioxidelite.ui.dr;

/**
 * obj_afterimage 的转写（scr_afterimage 创建的残影，groundshards 每 8 帧生成一个）：
 * Create 里 fadeSpeed = 0.04，Step 里 image_alpha -= fadeSpeed，<0 自毁。
 */
final class ObjAfterimage extends ObjMarker {
    private float fadeSpeed = 0.04f;
    boolean dead;

    ObjAfterimage(Spr sprite) {
        super(sprite);
    }

    void step() {
        imageAlpha -= fadeSpeed;
        if (imageAlpha < 0) {
            dead = true;
        }
    }
}
