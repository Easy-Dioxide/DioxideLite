package com.dioxidelite.ui.theme;

import java.util.EnumMap;
import java.util.Map;

/**
 * 编辑器的工作模型。改动先落在这里，保存时生成新的 {@link ThemeEntry}。
 *
 * <p>色槽沿用“显式/未设置”语义：编辑器里重置某一格即恢复它的回退逻辑，
 * 不会把回退值固化成显式值。</p>
 */
public final class EditableTheme {

    private final String sourceId;
    private final boolean builtinSource;

    private String name;
    private final ThemePalette palette;
    private final EnumMap<GradientSpec.Kind, GradientSpec> gradients = new EnumMap<>(GradientSpec.Kind.class);
    private ThemeAddons.Intro intro;
    private ThemeAddons.Ui ui;

    private EditableTheme(String sourceId, boolean builtinSource, String name, ThemePalette palette,
                          Map<GradientSpec.Kind, GradientSpec> gradients, ThemeAddons addons) {
        this.sourceId = sourceId;
        this.builtinSource = builtinSource;
        this.name = name;
        this.palette = palette == null ? new ThemePalette() : palette;
        for (GradientSpec.Kind kind : GradientSpec.Kind.values()) {
            GradientSpec spec = gradients == null ? null : gradients.get(kind);
            this.gradients.put(kind, spec == null ? GradientSpec.off() : spec);
        }
        this.intro = addons == null ? ThemeAddons.Intro.off() : addons.intro();
        this.ui = addons == null ? ThemeAddons.Ui.off() : addons.ui();
    }

    public static EditableTheme from(ThemeEntry entry) {
        ThemeEntry source = entry == null ? Themes.presetEntries().get(0) : entry;
        return new EditableTheme(
                source.id(),
                source.builtin(),
                source.name(),
                source.palette().copy(),
                source.gradients(),
                source.addons());
    }

    public String sourceId() {
        return sourceId;
    }

    public boolean builtinSource() {
        return builtinSource;
    }

    public String name() {
        return name;
    }

    public void rename(String value) {
        if (value != null && !value.isBlank()) {
            name = value.trim();
        }
    }

    public ThemePalette palette() {
        return palette;
    }

    /** 当前显式色值；未设置返回 null（编辑器显示生效回退值）。 */
    public Integer color(Slot slot) {
        return palette.get(slot);
    }

    public void color(Slot slot, Integer argb) {
        palette.set(slot, argb);
    }

    public void resetColor(Slot slot) {
        palette.clear(slot);
    }

    public GradientSpec gradient(GradientSpec.Kind kind) {
        return gradients.get(kind);
    }

    public void gradient(GradientSpec.Kind kind, GradientSpec spec) {
        gradients.put(kind, spec == null ? GradientSpec.off() : spec);
    }

    public ThemeAddons.Intro intro() {
        return intro;
    }

    public void intro(ThemeAddons.Intro value) {
        intro = value == null ? ThemeAddons.Intro.off() : value;
    }

    public ThemeAddons.Ui ui() {
        return ui;
    }

    public void ui(ThemeAddons.Ui value) {
        ui = value == null ? ThemeAddons.Ui.off() : value;
    }

    /** 生成最终条目；内置来源留空 ID，由存储层换发新的自定义 ID。 */
    public ThemeEntry toEntry() {
        String id = builtinSource ? "" : sourceId;
        return new ThemeEntry(id, name, false, palette,
                new EnumMap<>(gradients), new ThemeAddons(intro, ui), null);
    }

    /** 保存到磁盘并选中。 */
    public ThemeEntry save() {
        return Themes.saveCustomTheme(toEntry());
    }
}
