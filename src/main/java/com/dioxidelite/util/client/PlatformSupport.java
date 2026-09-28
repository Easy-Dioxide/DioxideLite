package com.dioxidelite.util.client;

import com.dioxidelite.DioxideLite;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Central place for every OS-specific behaviour of DioxideLite.
 *
 * <p>Before this class existed the client shelled out to {@code powershell.exe} / {@code explorer.exe},
 * opened folders through {@code java.awt.Desktop} and read the CPU name through JNA's
 * {@code Advapi32Util}. None of that works on macOS (AWT needs the AppKit main thread, which is
 * already owned by GLFW, and both JNA-platform and the PowerShell cmdlets are Windows-only), so all
 * of it is funnelled through here and dispatched per platform.</p>
 *
 * <p>Everything is deliberately implemented with JDK APIs plus the OS' own helper processes, so the
 * mod keeps working on Windows, macOS (Intel + Apple Silicon) and Linux with no extra native
 * dependency.</p>
 */
public final class PlatformSupport {

    /** Rough OS family of the running JVM. */
    public enum Family {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }

    private static final Family FAMILY = detectFamily();
    private static final boolean APPLE_SILICON = detectAppleSilicon();
    /** Lazily resolved: reading the CPU name spawns a helper process, so it must not run during
     *  class initialisation (PlatformSupport is first touched from the render path). */
    private static volatile String cpuName;

    private PlatformSupport() {
    }

    public static Family family() {
        return FAMILY;
    }

    public static boolean isWindows() {
        return FAMILY == Family.WINDOWS;
    }

    public static boolean isMacOS() {
        return FAMILY == Family.MACOS;
    }

    public static boolean isLinux() {
        return FAMILY == Family.LINUX;
    }

    /** True when running on an arm64 Mac (Apple Silicon); false on Intel Macs and other systems. */
    public static boolean isAppleSilicon() {
        return APPLE_SILICON;
    }

    /**
     * Opens a directory in Finder (macOS), Explorer (Windows) or the desktop's file manager (Linux).
     *
     * @param directory directory to reveal; created beforehand by the caller when needed
     */
    public static void openDirectory(Path directory) throws IOException {
        if (directory == null) {
            throw new IOException("No directory given");
        }
        Path absolute = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(absolute)) {
            throw new IOException("Not a directory: " + absolute);
        }
        switch (FAMILY) {
            case MACOS -> run("/usr/bin/open", absolute.toString());
            case WINDOWS -> run("explorer.exe", absolute.toString());
            case LINUX -> run("xdg-open", absolute.toString());
            default -> openWithAwt(absolute.toUri().toString());
        }
    }

    /**
     * Opens a URL (or any file/URI) with the system default handler.
     *
     * @param uri absolute http(s) URL or file URI
     */
    public static void openUri(String uri) throws IOException {
        if (uri == null || uri.isBlank()) {
            throw new IOException("No URI given");
        }
        switch (FAMILY) {
            case MACOS -> run("/usr/bin/open", uri);
            case WINDOWS -> {
                try {
                    run("rundll32.exe", "url.dll,FileProtocolHandler", uri);
                } catch (IOException firstFailure) {
                    openWithAwt(uri);
                }
            }
            case LINUX -> run("xdg-open", uri);
            default -> openWithAwt(uri);
        }
    }

    /**
     * CPU marketing name used for the NetEase Cloud Music device fingerprint.
     *
     * @return best-effort CPU name, empty string when the platform does not expose one
     */
    public static String cpuName() {
        String value = cpuName;
        if (value == null) {
            value = detectCpuName();
            cpuName = value;
        }
        return value;
    }

    /** Human readable platform string, used in logs and diagnostics. */
    public static String describe() {
        return FAMILY.name().toLowerCase(Locale.ROOT)
                + '/' + System.getProperty("os.arch", "unknown")
                + " (" + System.getProperty("os.name", "unknown")
                + ' ' + System.getProperty("os.version", "unknown") + ')';
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private static Family detectFamily() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return Family.WINDOWS;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return Family.MACOS;
        }
        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            return Family.LINUX;
        }
        return Family.OTHER;
    }

    private static boolean detectAppleSilicon() {
        if (FAMILY != Family.MACOS) {
            return false;
        }
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return arch.contains("aarch64") || arch.contains("arm64");
    }

    private static String detectCpuName() {
        try {
            return switch (FAMILY) {
                case MACOS -> firstLine(runCapturing("/usr/sbin/sysctl", "-n", "machdep.cpu.brand_string"));
                case LINUX -> linuxCpuName();
                case WINDOWS -> windowsCpuName();
                default -> "";
            };
        } catch (Throwable error) {
            DioxideLite.LOGGER.debug("Could not read the CPU name on {}", describe(), error);
            return "";
        }
    }

    private static String linuxCpuName() {
        try {
            Path cpuinfo = Path.of("/proc/cpuinfo");
            if (!Files.isReadable(cpuinfo)) {
                return "";
            }
            for (String line : Files.readAllLines(cpuinfo, StandardCharsets.UTF_8)) {
                int separator = line.indexOf(':');
                if (separator < 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                if (key.equals("model name") || key.equals("hardware") || key.equals("cpu model")) {
                    return line.substring(separator + 1).trim();
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // fall through to the empty result
        }
        return "";
    }

    private static String windowsCpuName() {
        // reg.exe is available on every supported Windows version, so the mod does not need JNA at runtime.
        String output = runCapturingQuietly("reg.exe", "query",
                "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString");
        for (String line : output.split("\\R")) {
            int marker = line.indexOf("REG_SZ");
            if (marker >= 0) {
                return line.substring(marker + "REG_SZ".length()).trim();
            }
        }
        return System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "");
    }

    /**
     * Runs a helper process and waits for it, draining its output so it cannot block on a full pipe.
     */
    private static void run(String... command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        drain(process);
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                throw new IOException("Timed out while running " + command[0]);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            process.destroy();
            throw new IOException("Interrupted while running " + command[0], error);
        }
    }

    private static String runCapturing(String... command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output;
        try (InputStream stream = process.getInputStream()) {
            output = stream.readAllBytes();
        }
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy();
                return "";
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            process.destroy();
            return "";
        }
        return new String(output, StandardCharsets.UTF_8);
    }

    private static String runCapturingQuietly(String... command) {
        try {
            return runCapturing(command);
        } catch (IOException | RuntimeException error) {
            return "";
        }
    }

    private static void drain(Process process) {
        try (InputStream stream = process.getInputStream()) {
            stream.readAllBytes();
        } catch (IOException ignored) {
            // the helper process closed its pipe early; the exit code still tells us what happened
        }
    }

    private static String firstLine(String value) {
        if (value == null) {
            return "";
        }
        List<String> lines = value.lines().toList();
        return lines.isEmpty() ? "" : lines.get(0).trim();
    }

    /**
     * Last resort for unknown desktops; only used when no OS helper process is available.
     */
    private static void openWithAwt(String uri) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("No desktop integration available for " + uri);
        }
        Desktop desktop = Desktop.getDesktop();
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            desktop.browse(URI.create(uri));
        } else {
            desktop.open(Path.of(URI.create(uri)).toFile());
        }
    }
}
