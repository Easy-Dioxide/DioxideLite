package com.dioxidelite.ui.theme;

import com.dioxidelite.DioxideLite;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 主题文件的导入导出：走 LWJGL 的 tinyfd（和项目里的字体/皮肤导入一致），
 * 不依赖 AWT，也就绕开了 macOS 主线程问题。
 */
public final class ThemeFiles {

    private static final String FILTER_LABEL = "DioxideLite theme (*.json)";

    private ThemeFiles() {
    }

    /** 弹出保存对话框并写入主题文件；取消或失败返回 null。 */
    public static Path export(ThemeEntry entry) {
        ThemeEntry target = entry == null ? Themes.currentEntry() : entry;
        String defaultName = ThemeStore.sanitize(target.name()) + ".theme.json";
        String home = Minecraft.getInstance().gameDirectory.getAbsolutePath();
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(1);
            patterns.put(stack.UTF8("*.json"));
            patterns.flip();
            selected = TinyFileDialogs.tinyfd_saveFileDialog(
                    "Export DioxideLite theme",
                    Path.of(home, defaultName).toString(),
                    patterns,
                    FILTER_LABEL);
        } catch (RuntimeException error) {
            DioxideLite.LOGGER.warn("Could not open the theme save dialog", error);
            return null;
        }
        if (selected == null || selected.isBlank()) {
            return null;
        }
        try {
            Path file = Path.of(selected);
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                file = file.resolveSibling(file.getFileName() + ".json");
            }
            Files.writeString(file, Themes.exportTheme(target), StandardCharsets.UTF_8);
            return file;
        } catch (IOException | RuntimeException error) {
            DioxideLite.LOGGER.warn("Unable to export theme to {}", selected, error);
            showMessage("Export failed", String.valueOf(error.getMessage()));
            return null;
        }
    }

    /** 弹出打开对话框并导入主题；取消或解析失败返回 null。 */
    public static ThemeEntry importTheme() {
        String home = Minecraft.getInstance().gameDirectory.getAbsolutePath();
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(1);
            patterns.put(stack.UTF8("*.json"));
            patterns.flip();
            selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "Import DioxideLite theme",
                    home,
                    patterns,
                    FILTER_LABEL,
                    false);
        } catch (RuntimeException error) {
            DioxideLite.LOGGER.warn("Could not open the theme import dialog", error);
            return null;
        }
        if (selected == null || selected.isBlank()) {
            return null;
        }
        try {
            String raw = Files.readString(Path.of(selected), StandardCharsets.UTF_8);
            ThemeEntry imported = Themes.importTheme(raw);
            if (imported == null) {
                showMessage("Import failed", "The selected file is not a valid theme.");
            }
            return imported;
        } catch (IOException | RuntimeException error) {
            DioxideLite.LOGGER.warn("Unable to import theme from {}", selected, error);
            showMessage("Import failed", String.valueOf(error.getMessage()));
            return null;
        }
    }

    private static void showMessage(String title, String message) {
        TinyFileDialogs.tinyfd_messageBox(
                "DioxideLite " + title,
                message == null || message.isBlank() ? "Unknown error." : message,
                "ok",
                "error",
                1);
    }
}
