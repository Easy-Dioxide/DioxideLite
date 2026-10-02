package com.dioxidelite.ui.theme;

import java.util.EnumMap;
import java.util.Map;

/**
 * 主题的颜色槽集合。
 *
 * <p>只保存“显式设置”的槽：{@link #get} 返回 null 表示该槽没有覆盖，
 * 消费点应该回退到自己的默认取色逻辑。这样 CLASSIC 主题可以全空，
 * 保证默认观感与旧版本完全一致。</p>
 */
public final class ThemePalette {

    private final EnumMap<Slot, Integer> colors = new EnumMap<>(Slot.class);

    public ThemePalette() {
    }

    public ThemePalette(ThemePalette source) {
        if (source != null) {
            colors.putAll(source.colors);
        }
    }

    /** 显式设置的 ARGB；未设置时返回 null。 */
    public Integer get(Slot slot) {
        return colors.get(slot);
    }

    /** 该槽是否被显式覆盖。 */
    public boolean isSet(Slot slot) {
        return colors.get(slot) != null;
    }

    public void set(Slot slot, Integer argb) {
        if (argb == null) {
            colors.remove(slot);
        } else {
            colors.put(slot, argb);
        }
    }

    public void clear(Slot slot) {
        colors.remove(slot);
    }

    public int size() {
        return colors.size();
    }

    public Map<Slot, Integer> entries() {
        return colors;
    }

    public ThemePalette copy() {
        return new ThemePalette(this);
    }
}
