package com.dioxidelite.ui.theme;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 一套完整主题：颜色槽 + 五组渐变 + 两组附加选项，外加旧主题的 ClickGUI 预设。
 *
 * <p>入场数据不可变；编辑器改动请用 {@link EditableTheme}，保存时再生成新的
 * ThemeEntry。{@code clickGuiPreset} 只有旧四个内置主题才有，其他主题为 null。</p>
 */
public final class ThemeEntry {

    private final String id;
    private final String name;
    private final boolean builtin;
    private final ThemePalette palette;
    private final Map<GradientSpec.Kind, GradientSpec> gradients;
    private final ThemeAddons addons;
    private final ClickGuiPreset clickGuiPreset;

    public ThemeEntry(String id, String name, boolean builtin, ThemePalette palette,
                      Map<GradientSpec.Kind, GradientSpec> gradients, ThemeAddons addons,
                      ClickGuiPreset clickGuiPreset) {
        this.id = normalize(id);
        this.name = name == null || name.isBlank() ? this.id : name.trim();
        this.builtin = builtin;
        this.palette = palette == null ? new ThemePalette() : palette.copy();
        this.gradients = new EnumMap<>(GradientSpec.Kind.class);
        if (gradients != null) {
            this.gradients.putAll(gradients);
        }
        for (GradientSpec.Kind kind : GradientSpec.Kind.values()) {
            this.gradients.putIfAbsent(kind, GradientSpec.off());
        }
        this.addons = addons == null ? new ThemeAddons() : addons;
        this.clickGuiPreset = clickGuiPreset;
    }

    public static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).trim();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean builtin() {
        return builtin;
    }

    public ThemePalette palette() {
        return palette;
    }

    public GradientSpec gradient(GradientSpec.Kind kind) {
        GradientSpec spec = gradients.get(kind);
        return spec == null ? GradientSpec.off() : spec;
    }

    public Map<GradientSpec.Kind, GradientSpec> gradients() {
        return gradients;
    }

    public ThemeAddons addons() {
        return addons;
    }

    public ClickGuiPreset clickGuiPreset() {
        return clickGuiPreset;
    }
}
