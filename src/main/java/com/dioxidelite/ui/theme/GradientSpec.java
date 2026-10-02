package com.dioxidelite.ui.theme;

/**
 * 一组渐变的描述。默认关闭；开启后才在实际渐变绘制里生效。
 *
 * @param enabled  开关
 * @param start    起点 ARGB
 * @param end      终点 ARGB
 * @param angleDeg 角度（0 = 从左到右，顺时针）
 */
public record GradientSpec(boolean enabled, int start, int end, float angleDeg) {

    /** 五组渐变各挂在一个 UI 层级上，和参考实现的槽位一一对应。 */
    public enum Kind {
        WINDOW("window"),
        HEADER("header"),
        SURFACE("surface"),
        CARD("card"),
        STROKE("stroke");

        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public static GradientSpec off() {
        return new GradientSpec(false, 0xFFFFFFFF, 0xFFFFFFFF, 90.0F);
    }

    public GradientSpec withEnabled(boolean value) {
        return new GradientSpec(value, start, end, angleDeg);
    }

    public GradientSpec withStart(int value) {
        return new GradientSpec(enabled, value, end, angleDeg);
    }

    public GradientSpec withEnd(int value) {
        return new GradientSpec(enabled, start, value, angleDeg);
    }

    public GradientSpec withAngle(float value) {
        return new GradientSpec(enabled, start, end, value);
    }
}
