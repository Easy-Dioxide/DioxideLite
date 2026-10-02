package com.dioxidelite.ui.theme;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动动画样式注册表。当前只有 DR，后续本项目自己的启动动画在这里追加；
 * 每个样式声明自己的显示名、子配置类型与播放器入口，主题编辑器据此渲染。
 */
public final class IntroStyles {

    public static final String DR = "dr";

    /** 一个可选的启动动画样式。 */
    public record Style(String id, String displayName) {
    }

    private static final List<Style> STYLES = new ArrayList<>();

    static {
        STYLES.add(new Style(DR, "DeltaRune"));
    }

    private IntroStyles() {
    }

    public static List<Style> all() {
        return List.copyOf(STYLES);
    }

    public static Style byId(String id) {
        for (Style style : STYLES) {
            if (style.id().equalsIgnoreCase(id)) {
                return style;
            }
        }
        return null;
    }
}
