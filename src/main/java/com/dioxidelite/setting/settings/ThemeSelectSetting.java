package com.dioxidelite.setting.settings;

import com.dioxidelite.setting.Setting;
import com.dioxidelite.ui.theme.ThemeEntry;
import com.dioxidelite.ui.theme.Themes;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 主题选择设置：值域是运行时主题列表（内置 + 自定义），点击循环切换。
 *
 * <p>和普通 Setting 不同，它不自己存值——{@link #toJson()} 直接回报当前主题 ID，
 * 这样在 ClickGUI 主题卡片里换了主题，模块下拉与配置文件都会保持一致。</p>
 */
public class ThemeSelectSetting extends Setting<String> {

    public ThemeSelectSetting(String name) {
        super(name, "");
    }

    /** 在主题列表里前后循环。 */
    public void cycle(int direction) {
        var list = Themes.themes();
        if (list.isEmpty()) {
            return;
        }
        int index = 0;
        String currentId = Themes.currentId();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equalsIgnoreCase(currentId)) {
                index = i;
                break;
            }
        }
        int next = Math.floorMod(index + direction, list.size());
        set(list.get(next).id());
    }

    /** 当前主题的显示名。 */
    public String displayValue() {
        ThemeEntry entry = Themes.currentEntry();
        return entry == null ? "Classic" : entry.name();
    }

    @Override
    public JsonElement toJson() {
        String id = Themes.currentId();
        return new JsonPrimitive(id == null || id.isBlank() ? Themes.CLASSIC : id);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            return;
        }
        String id = json.getAsString();
        if (id == null || id.isBlank()) {
            return;
        }
        // 旧配置里的 DEFAULT / LIQUID_GLASS 等大写枚举名会被规整并映射到新主题
        set(ThemeEntry.normalize(id));
    }
}
