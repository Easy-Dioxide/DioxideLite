package com.dioxidelite.ui.theme;

import com.dioxidelite.module.modules.ClickGui;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主题门面：内置注册表、当前主题、自定义主题增删改查。
 *
 * <p>数据层只关心“选了什么”，应用副作用（ClickGUI 预设、DR 状态同步、主菜单路由）
 * 由 {@link ThemeRuntime} 负责。当前主题的槽在 {@link #resolve} 里统一取值：
 * 显式设置优先，否则回退调用方给的旧逻辑。</p>
 */
public final class Themes {

    public static final String CLASSIC = "classic";
    public static final String LIQUID_GLASS = "liquid_glass";
    public static final String MINIMAL = "minimal";
    public static final String SIGNATURE = "signature";
    public static final String SIGNATURE_DARK = "signature_dark";

    /** 旧四个主题原本的 ClickGUI 配色职责（acc 色值与槽值一致）。 */
    private static final ThemeEntry LIQUID_GLASS_ENTRY = builtin(
            LIQUID_GLASS, "Liquid Glass",
            accentPalette(0xFF78C8FF),
            ClickGuiPreset.of(ClickGui.Mode.Setsuna, false, null, 0x78C8FF));
    private static final ThemeEntry MINIMAL_ENTRY = builtin(
            MINIMAL, "Minimal",
            accentPalette(0xFFCDD7E1),
            ClickGuiPreset.of(ClickGui.Mode.LegacyStyle, false, null, 0xCDD7E1));
    private static final ThemeEntry SIGNATURE_ENTRY = builtin(
            SIGNATURE, "Signature",
            accentPalette(0xFF69B9FF),
            ClickGuiPreset.of(ClickGui.Mode.Setsuna, true, null, 0x69B9FF));
    private static final ThemeEntry SIGNATURE_DARK_ENTRY = builtin(
            SIGNATURE_DARK, "Signature Dark",
            accentPalette(0xFF9BD7FF),
            ClickGuiPreset.of(ClickGui.Mode.LegacyStyle, false, 3, 0x9BD7FF));

    /** CLASSIC 全空：所有槽回退消费点的原有取色，等于旧观感。 */
    private static final ThemeEntry CLASSIC_ENTRY = builtin(CLASSIC, "Classic", null, null);

    private static final Map<String, ThemeEntry> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put(CLASSIC, CLASSIC_ENTRY);
        PRESETS.put(LIQUID_GLASS, LIQUID_GLASS_ENTRY);
        PRESETS.put(MINIMAL, MINIMAL_ENTRY);
        PRESETS.put(SIGNATURE, SIGNATURE_ENTRY);
        PRESETS.put(SIGNATURE_DARK, SIGNATURE_DARK_ENTRY);
    }

    private static volatile ThemeEntry current = CLASSIC_ENTRY;
    private static volatile boolean loaded;

    private Themes() {
    }

    private static ThemeEntry builtin(String id, String name, ThemePalette palette, ClickGuiPreset preset) {
        return new ThemeEntry(id, name, true, palette, null, new ThemeAddons(), preset);
    }

    private static ThemePalette accentPalette(int argb) {
        ThemePalette palette = new ThemePalette();
        palette.set(Slot.ACCENT, argb);
        return palette;
    }

    // ------------------------------------------------------------------
    // 加载与查询
    // ------------------------------------------------------------------

    /** 游戏目录可用后加载磁盘状态；不可用时停留在内置默认并保持可重试。 */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        synchronized (Themes.class) {
            if (loaded) {
                return;
            }
            ThemeStore store = ThemeStore.get();
            ThemeEntry entry = store.byId(store.currentId());
            current = entry != null ? entry : CLASSIC_ENTRY;
            loaded = true;
        }
    }

    public static ThemeEntry preset(String id) {
        return id == null ? null : PRESETS.get(ThemeEntry.normalize(id));
    }

    public static List<ThemeEntry> presetEntries() {
        return List.copyOf(PRESETS.values());
    }

    public static ThemeEntry currentEntry() {
        ensureLoaded();
        return current;
    }

    public static String currentId() {
        return currentEntry().id();
    }

    /** 内置 + 自定义，按内置顺序在前、自定义按名称排序。 */
    public static List<ThemeEntry> themes() {
        ensureLoaded();
        List<ThemeEntry> out = new ArrayList<>(PRESETS.values());
        ThemeStore store = storeOrNull();
        if (store != null) {
            out.addAll(store.customThemes());
        }
        return Collections.unmodifiableList(out);
    }

    public static List<ThemeEntry> customThemes() {
        ensureLoaded();
        ThemeStore store = storeOrNull();
        return store == null ? List.of() : store.customThemes();
    }

    public static ThemeEntry byId(String id) {
        ensureLoaded();
        ThemeEntry preset = preset(id);
        if (preset != null) {
            return preset;
        }
        ThemeStore store = storeOrNull();
        return store == null ? null : store.customById(id);
    }

    // ------------------------------------------------------------------
    // 选择与自定义增删改
    // ------------------------------------------------------------------

    /** 只切数据；应用副作用请走 ThemeRuntime。 */
    static ThemeEntry setCurrent(String id) {
        ThemeEntry entry = byId(id);
        if (entry == null) {
            entry = CLASSIC_ENTRY;
        }
        current = entry;
        ThemeStore store = storeOrNull();
        if (store != null) {
            store.setCurrentId(entry.id());
        }
        return entry;
    }

    /** 以 {@code base}（缺省为当前主题）为模板新建自定义主题并选中。 */
    public static ThemeEntry createCustomTheme(String name, ThemeEntry base) {
        ensureLoaded();
        ThemeStore store = storeOrNull();
        if (store == null) {
            return null;
        }
        ThemeEntry source = base != null ? base : current;
        String displayName = name == null || name.isBlank() ? "Custom Theme" : name.trim();
        ThemeEntry created = new ThemeEntry(
                store.uniqueId(displayName),
                displayName,
                false,
                source.palette(),
                source.gradients(),
                source.addons(),
                null);
        return saveCustomTheme(created);
    }

    /** 保存自定义主题并选中（编辑器即时保存的入口）。 */
    public static ThemeEntry saveCustomTheme(ThemeEntry entry) {
        ensureLoaded();
        ThemeStore store = storeOrNull();
        if (store == null || entry == null) {
            return null;
        }
        ThemeEntry saved = store.saveCustom(entry);
        if (saved != null) {
            current = saved;
            store.setCurrentId(saved.id());
        }
        return saved;
    }

    public static boolean deleteCustomTheme(String id) {
        ensureLoaded();
        ThemeStore store = storeOrNull();
        if (store == null) {
            return false;
        }
        boolean removed = store.deleteCustom(id);
        if (removed && ThemeEntry.normalize(id).equals(current.id())) {
            current = CLASSIC_ENTRY;
        }
        return removed;
    }

    /** 导入一个主题文件内容；成功后返回新主题并选中。 */
    public static ThemeEntry importTheme(String rawJson) {
        ensureLoaded();
        ThemeStore store = storeOrNull();
        if (store == null) {
            return null;
        }
        ThemeEntry parsed = ThemeStore.fromImportJson(rawJson);
        if (parsed == null) {
            return null;
        }
        ThemeEntry importable = new ThemeEntry(
                store.uniqueId(parsed.name()),
                parsed.name(),
                false,
                parsed.palette(),
                parsed.gradients(),
                parsed.addons(),
                null);
        return saveCustomTheme(importable);
    }

    /** 导出为可写文件的 JSON 文本。 */
    public static String exportTheme(ThemeEntry entry) {
        return ThemeStore.toExportJson(entry == null ? current : entry) + "\n";
    }

    // ------------------------------------------------------------------
    // 取值
    // ------------------------------------------------------------------

    /** 显式槽值优先，否则返回 fallback。HUD/屏幕的每帧取色走这里，保持轻量。 */
    public static int resolve(Slot slot, int fallback) {
        ThemeEntry entry = current;
        Integer value = entry.palette().get(slot);
        return value != null ? value : fallback;
    }

    /** 显式槽值优先，否则现算回退值（GuiPalette 的派生色走这条）。 */
    public static int resolve(Slot slot, java.util.function.IntSupplier fallback) {
        ThemeEntry entry = current;
        Integer value = entry.palette().get(slot);
        return value != null ? value : fallback.getAsInt();
    }

    /** 当前主题的显式槽值；未设置返回 null。绘制层做“有则覆盖、无则原逻辑”时用。 */
    public static Integer explicit(Slot slot) {
        return current.palette().get(slot);
    }

    /** 当前主题生效的 accent：显式槽优先，否则沿用旧配置的 ClickGui.accent。 */
    public static int accent() {
        ThemeEntry entry = current;
        Integer value = entry.palette().get(Slot.ACCENT);
        if (value != null) {
            return value;
        }
        Color legacy = ClickGui.INSTANCE.accent.get();
        return 0xFF000000 | (legacy.getRed() << 16) | (legacy.getGreen() << 8) | legacy.getBlue();
    }

    private static ThemeStore storeOrNull() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : ThemeStore.get();
    }
}
