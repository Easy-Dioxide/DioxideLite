package com.dioxidelite.ui.theme;

import java.util.ArrayList;
import java.util.List;

/**
 * 主菜单 UI 样式注册表。dioxide 是项目默认版式，dr 是 DELTARUNE 版式；
 * 后续新增版式在这里注册，主题编辑器的样式下拉自动多出一项。
 */
public final class UiStyles {

    public static final String DIOXIDE = "dioxide";
    public static final String DR = "dr";

    public record Style(String id, String displayName) {
    }

    private static final List<Style> STYLES = new ArrayList<>();

    static {
        STYLES.add(new Style(DIOXIDE, "Dioxide"));
        STYLES.add(new Style(DR, "DeltaRune"));
    }

    private UiStyles() {
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

    /** 该样式是否需要 DR 版式（章节 / 背景）的子配置。 */
    public static boolean isDr(String id) {
        return DR.equalsIgnoreCase(id);
    }
}
