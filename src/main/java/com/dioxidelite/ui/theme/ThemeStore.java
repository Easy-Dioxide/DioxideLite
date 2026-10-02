package com.dioxidelite.ui.theme;

import com.dioxidelite.DioxideLite;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 主题的文件存储。
 *
 * <pre>
 * &lt;gameDir&gt;/dioxide-lite/themes/state.json          当前主题 ID
 * &lt;gameDir&gt;/dioxide-lite/themes/custom/&lt;id&gt;.json   单个自定义主题全量
 * </pre>
 *
 * <p>内置主题不落盘；自定义主题的 ID 与内置防撞；损坏的文件直接忽略，不影响启动。</p>
 */
final class ThemeStore {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String TEMP_PREFIX = ".theme-";
    private static final String TEMP_SUFFIX = ".tmp";

    private static ThemeStore instance;

    private final List<ThemeEntry> customThemes = new ArrayList<>();
    private final Map<String, ThemeEntry> customById = new LinkedHashMap<>();
    private String currentId = Themes.CLASSIC;

    private ThemeStore() {
        load();
    }

    static synchronized ThemeStore get() {
        if (instance == null) {
            instance = new ThemeStore();
        }
        return instance;
    }

    List<ThemeEntry> customThemes() {
        return List.copyOf(customThemes);
    }

    String currentId() {
        return currentId;
    }

    ThemeEntry byId(String id) {
        String key = ThemeEntry.normalize(id);
        if (key.isEmpty()) {
            return null;
        }
        ThemeEntry preset = Themes.preset(key);
        return preset != null ? preset : customById.get(key);
    }

    ThemeEntry customById(String id) {
        return customById.get(ThemeEntry.normalize(id));
    }

    void setCurrentId(String id) {
        String key = ThemeEntry.normalize(id);
        if (key.isEmpty() || key.equals(currentId)) {
            return;
        }
        currentId = key;
        saveState();
    }

    /**
     * 保存一个自定义主题。内置 ID 会被换成唯一的新 ID；同 ID 覆盖旧文件。
     */
    ThemeEntry saveCustom(ThemeEntry entry) {
        if (entry == null) {
            return null;
        }
        String id = ThemeEntry.normalize(entry.id());
        if (id.isEmpty()) {
            id = uniqueId(entry.name());
        } else if (Themes.preset(id) != null) {
            id = uniqueId(entry.name());
        }
        ThemeEntry normalized = new ThemeEntry(
                id,
                entry.name(),
                false,
                entry.palette(),
                entry.gradients(),
                entry.addons(),
                null);
        ThemeEntry previous = customById.get(id);
        if (previous != null) {
            customThemes.remove(previous);
        }
        customThemes.add(normalized);
        customThemes.sort(Comparator.comparing(ThemeEntry::name, String.CASE_INSENSITIVE_ORDER));
        customById.put(id, normalized);
        writeCustomFile(normalized);
        return normalized;
    }

    boolean deleteCustom(String id) {
        String key = ThemeEntry.normalize(id);
        ThemeEntry removed = customById.remove(key);
        if (removed == null) {
            return false;
        }
        customThemes.remove(removed);
        try {
            Files.deleteIfExists(customFile(key));
        } catch (IOException error) {
            DioxideLite.LOGGER.warn("Failed to delete theme file {}", key, error);
        }
        if (key.equals(currentId)) {
            currentId = Themes.CLASSIC;
            saveState();
        }
        return true;
    }

    /** 从主题名生成不与内置/现有自定义冲突的 ID。 */
    String uniqueId(String name) {
        String base = sanitize(name == null || name.isBlank() ? "custom_theme" : name);
        if (base.isEmpty()) {
            base = "custom_theme";
        }
        String candidate = base;
        int index = 2;
        while (Themes.preset(candidate) != null || customById.containsKey(candidate)) {
            candidate = base + "_" + index++;
        }
        return candidate;
    }

    /** 文件名安全化：小写、空白与符号折叠成下划线。 */
    static String sanitize(String raw) {
        StringBuilder out = new StringBuilder();
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        boolean lastUnderscore = false;
        for (int i = 0; i < lower.length() && out.length() < 48; i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
                lastUnderscore = false;
            } else if (!lastUnderscore && out.length() > 0) {
                out.append('_');
                lastUnderscore = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '_') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // 读写
    // ------------------------------------------------------------------

    private void load() {
        currentId = readCurrentId();
        customThemes.clear();
        customById.clear();
        Path dir = customDir();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(this::readCustomFile);
            } catch (IOException error) {
                DioxideLite.LOGGER.warn("Failed to list custom themes in {}", dir, error);
            }
        }
        customThemes.sort(Comparator.comparing(ThemeEntry::name, String.CASE_INSENSITIVE_ORDER));
        if (byId(currentId) == null) {
            currentId = Themes.CLASSIC;
            saveState();
        }
    }

    private void readCustomFile(Path path) {
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(raw);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            ThemeEntry entry = parseEntry(parsed.getAsJsonObject());
            if (entry == null || entry.id().isEmpty() || Themes.preset(entry.id()) != null) {
                return;
            }
            customThemes.removeIf(existing -> existing.id().equals(entry.id()));
            customThemes.add(entry);
            customById.put(entry.id(), entry);
        } catch (Exception error) {
            DioxideLite.LOGGER.warn("Skipping unreadable theme file {}", path, error);
        }
    }

    private String readCurrentId() {
        Path file = stateFile();
        if (!Files.isRegularFile(file)) {
            return Themes.CLASSIC;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (parsed != null && parsed.isJsonObject()) {
                JsonElement current = parsed.getAsJsonObject().get("current");
                if (current != null && current.isJsonPrimitive()) {
                    String id = ThemeEntry.normalize(current.getAsString());
                    if (!id.isEmpty()) {
                        return id;
                    }
                }
            }
        } catch (Exception error) {
            DioxideLite.LOGGER.warn("Could not read theme state {}", file, error);
        }
        return Themes.CLASSIC;
    }

    private void saveState() {
        try {
            Files.createDirectories(themeRoot());
            JsonObject root = new JsonObject();
            root.addProperty("current", currentId);
            writeAtomically(stateFile(), GSON.toJson(root));
        } catch (IOException error) {
            DioxideLite.LOGGER.warn("Could not save theme state", error);
        }
    }

    private void writeCustomFile(ThemeEntry entry) {
        try {
            Files.createDirectories(customDir());
            writeAtomically(customFile(entry.id()), GSON.toJson(entryToJson(entry)));
        } catch (IOException error) {
            DioxideLite.LOGGER.warn("Could not save custom theme {}", entry.id(), error);
        }
    }

    private static void writeAtomically(Path target, String json) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), TEMP_PREFIX, TEMP_SUFFIX);
        Files.writeString(temp, json + "\n", StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    Path themeRoot() {
        return DioxideLite.mc().gameDirectory.toPath().resolve(DioxideLite.MOD_ID).resolve("themes");
    }

    Path customDir() {
        return themeRoot().resolve("custom");
    }

    private Path stateFile() {
        return themeRoot().resolve("state.json");
    }

    private Path customFile(String id) {
        return customDir().resolve(sanitize(id) + ".json");
    }

    // ------------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------------

    /** 把一个主题序列化成 JSON（保存自定义、导出文件共用同一格式）。 */
    static JsonObject entryToJson(ThemeEntry entry) {
        JsonObject root = new JsonObject();
        root.addProperty("id", entry.id());
        root.addProperty("name", entry.name());
        root.addProperty("builtin", entry.builtin());

        JsonObject colors = new JsonObject();
        for (Map.Entry<Slot, Integer> pair : entry.palette().entries().entrySet()) {
            colors.addProperty(pair.getKey().key(), colorToString(pair.getValue()));
        }
        root.add("colors", colors);

        JsonObject gradients = new JsonObject();
        for (GradientSpec.Kind kind : GradientSpec.Kind.values()) {
            GradientSpec spec = entry.gradient(kind);
            JsonObject node = new JsonObject();
            node.addProperty("enabled", spec.enabled());
            node.addProperty("start", colorToString(spec.start()));
            node.addProperty("end", colorToString(spec.end()));
            node.addProperty("angle", spec.angleDeg());
            gradients.add(kind.key(), node);
        }
        root.add("gradients", gradients);

        JsonObject addons = new JsonObject();
        JsonObject intro = new JsonObject();
        intro.addProperty("enabled", entry.addons().intro().enabled());
        intro.addProperty("style", entry.addons().intro().styleId());
        intro.addProperty("chapter", entry.addons().intro().chapter());
        intro.addProperty("savedFlavor", entry.addons().intro().savedFlavor());
        addons.add("intro", intro);
        JsonObject ui = new JsonObject();
        ui.addProperty("enabled", entry.addons().ui().enabled());
        ui.addProperty("style", entry.addons().ui().styleId());
        ui.addProperty("chapter", entry.addons().ui().chapter());
        ui.addProperty("background", entry.addons().ui().backgroundMode().key());
        addons.add("ui", ui);
        root.add("addons", addons);
        return root;
    }

    /** 从 JSON 解析自定义主题；缺字段按“未设置”处理，损坏值落到默认。 */
    static ThemeEntry parseEntry(JsonObject root) {
        if (root == null) {
            return null;
        }
        String id = ThemeEntry.normalize(string(root, "id", ""));
        if (id.isEmpty()) {
            return null;
        }
        String name = string(root, "name", id);

        ThemePalette palette = new ThemePalette();
        JsonObject colors = object(root, "colors");
        if (colors != null) {
            for (Slot slot : Slot.values()) {
                if (colors.has(slot.key())) {
                    Integer parsed = parseColor(colors.get(slot.key()));
                    if (parsed != null) {
                        palette.set(slot, parsed);
                    }
                }
            }
        }

        Map<GradientSpec.Kind, GradientSpec> gradients = new LinkedHashMap<>();
        JsonObject gradientRoot = object(root, "gradients");
        for (GradientSpec.Kind kind : GradientSpec.Kind.values()) {
            JsonObject node = object(gradientRoot, kind.key());
            if (node == null) {
                continue;
            }
            boolean enabled = bool(node, "enabled", false);
            Integer start = parseColor(node.get("start"));
            Integer end = parseColor(node.get("end"));
            float angle = (float) number(node, "angle", 90.0);
            gradients.put(kind, new GradientSpec(enabled,
                    start == null ? 0xFFFFFFFF : start,
                    end == null ? 0xFFFFFFFF : end,
                    angle));
        }

        ThemeAddons.Intro intro = ThemeAddons.Intro.off();
        ThemeAddons.Ui ui = ThemeAddons.Ui.off();
        JsonObject addons = object(root, "addons");
        if (addons != null) {
            JsonObject introNode = object(addons, "intro");
            if (introNode != null) {
                intro = new ThemeAddons.Intro(
                        bool(introNode, "enabled", false),
                        string(introNode, "style", IntroStyles.DR),
                        ThemeAddons.clampChapter((int) number(introNode, "chapter", 1)),
                        bool(introNode, "savedFlavor", true));
            }
            JsonObject uiNode = object(addons, "ui");
            if (uiNode != null) {
                ui = new ThemeAddons.Ui(
                        bool(uiNode, "enabled", false),
                        string(uiNode, "style", UiStyles.DIOXIDE),
                        ThemeAddons.clampChapter((int) number(uiNode, "chapter", 1)),
                        ThemeAddons.BackgroundMode.fromKey(string(uiNode, "background", "fountain")));
            }
        }

        return new ThemeEntry(id, name, false, palette, gradients, new ThemeAddons(intro, ui), null);
    }

    static Integer parseColor(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        var primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return primitive.getAsInt();
        }
        if (!primitive.isString()) {
            return null;
        }
        String text = primitive.getAsString().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        } else if (text.startsWith("0x") || text.startsWith("0X")) {
            text = text.substring(2);
        }
        try {
            long value = Long.parseUnsignedLong(text, 16);
            if (text.length() <= 6) {
                return (int) (0xFF000000L | value);
            }
            return (int) value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String colorToString(int argb) {
        return String.format("#%08X", argb);
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || key == null) {
            return null;
        }
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject parent, String key, String fallback) {
        if (parent == null) {
            return fallback;
        }
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        String value = element.getAsString();
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean bool(JsonObject parent, String key, boolean fallback) {
        if (parent == null) {
            return fallback;
        }
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        var primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return Boolean.parseBoolean(primitive.getAsString());
    }

    private static double number(JsonObject parent, String key, double fallback) {
        if (parent == null) {
            return fallback;
        }
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    /** 导出：把主题写成独立文件字节（供文件对话框保存）。 */
    static String toExportJson(ThemeEntry entry) {
        JsonObject root = entryToJson(entry);
        root.addProperty("builtin", false);
        return GSON.toJson(root);
    }

    /** 导入：从文件内容解析主题；失败返回 null。 */
    static ThemeEntry fromImportJson(String raw) {
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            if (parsed == null || !parsed.isJsonObject()) {
                return null;
            }
            JsonObject root = parsed.getAsJsonObject();
            // 兼容直接导出数组的情况（多主题文件）
            if (root.has("themes") && root.get("themes").isJsonArray()) {
                JsonArray array = root.getAsJsonArray("themes");
                if (array.size() > 0 && array.get(0).isJsonObject()) {
                    return parseEntry(array.get(0).getAsJsonObject());
                }
                return null;
            }
            return parseEntry(root);
        } catch (Exception ignored) {
            return null;
        }
    }
}
