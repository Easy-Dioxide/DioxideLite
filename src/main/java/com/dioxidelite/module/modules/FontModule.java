package com.dioxidelite.module.modules;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.notification.NotificationManager;
import com.dioxidelite.notification.NotificationType;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.ButtonSetting;
import com.dioxidelite.setting.settings.FontSetting;
import com.dioxidelite.util.client.PlatformSupport;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Path;

/** Imports and selects fonts used by the client's Skija text renderer. */
public final class FontModule extends Module {

    public static final FontModule INSTANCE = new FontModule();

    public final FontSetting font = add(new FontSetting("Font", SkijaUi.CLIENT_FONT,
            SkijaUi::availableFontNames));
    public final ButtonSetting importFont = add(new ButtonSetting("Import Font", this::chooseFont));
    public final ButtonSetting reload = add(new ButtonSetting("Reload Fonts", this::reloadFonts));
    public final ButtonSetting openFolder = add(new ButtonSetting("Open Font Folder", this::openFontFolder));

    private FontModule() {
        super("Font", Category.CLIENT);
        setToggleable(false);
        font.onChange(SkijaUi::selectFont);
    }

    @Override
    protected void onTrigger() {
        chooseFont();
    }

    private void chooseFont() {
        Path selected = pickFontFile();
        if (selected != null) {
            mc.execute(() -> importFont(selected));
        }
    }

    /**
     * Native "pick a font file" dialog.
     *
     * <p>Uses LWJGL's tinyfd, i.e. the platform's own dialog: the Cocoa/AppleScript panel on macOS,
     * the Win32 common dialog on Windows and GTK/zenity/xdg-desktop-portal on Linux. The previous
     * Windows-only implementation shelled out to {@code powershell.exe} + WinForms and the fallback
     * used {@code java.awt.FileDialog}, which on macOS needs the AppKit main thread that GLFW already
     * owns, so opening the picker there either hung or aborted the client.</p>
     */
    private static Path pickFontFile() {
        String startDirectory = null;
        try {
            startDirectory = SkijaUi.fontDirectory().toString();
        } catch (IOException error) {
            DioxideLite.LOGGER.warn("Could not resolve the font directory", error);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.mallocPointer(3);
            patterns.put(stack.UTF8("*.ttf"));
            patterns.put(stack.UTF8("*.otf"));
            patterns.put(stack.UTF8("*.ttc"));
            patterns.flip();
            String selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "Import DioxideLite font",
                    startDirectory,
                    patterns,
                    "Font files (*.ttf, *.otf, *.ttc)",
                    false);
            if (selected == null || selected.isBlank()) {
                return null;
            }
            return Path.of(selected);
        } catch (RuntimeException error) {
            DioxideLite.LOGGER.warn("Could not open the font file picker", error);
            notifyResult(NotificationType.ERROR, "Could not open file picker");
            return null;
        }
    }

    private void importFont(Path source) {
        try {
            String imported = SkijaUi.importFont(source);
            font.set(imported);
            notifyResult(NotificationType.SUCCESS, "Imported " + imported);
        } catch (IOException | RuntimeException error) {
            DioxideLite.LOGGER.warn("Could not import font from {}", source, error);
            notifyResult(NotificationType.ERROR, "Invalid or unreadable font file");
        }
    }

    private void reloadFonts() {
        SkijaUi.reloadImportedFonts();
        font.reconcile();
        SkijaUi.selectFont(font.get());
        notifyResult(NotificationType.INFO, "Font list reloaded");
    }

    private void openFontFolder() {
        try {
            PlatformSupport.openDirectory(SkijaUi.fontDirectory());
        } catch (IOException | RuntimeException error) {
            DioxideLite.LOGGER.warn("Could not open the font directory", error);
            notifyResult(NotificationType.ERROR, "Could not open font folder");
        }
    }

    private static void notifyResult(NotificationType type, String message) {
        NotificationManager.INSTANCE.post(type, "Font", message);
    }
}
