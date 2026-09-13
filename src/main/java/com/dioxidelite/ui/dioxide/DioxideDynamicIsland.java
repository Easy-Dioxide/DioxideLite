package com.dioxidelite.ui.dioxide;

import com.dioxidelite.module.modules.player.IrcModule;
import com.dioxidelite.DioxideLite;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.render.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DioxideLite Dynamic Island.
 *
 * <p>Visual/QoL only. This is an independent Skija implementation of the
 * rounded OPAI-style island presentation: dark glass body, white inner edge,
 * soft neon glow, logo-led status content and smooth morphing while the player
 * list key is held. No automation/client-module state is read here.</p>
 */
public final class DioxideDynamicIsland {
    private static final DioxideDynamicIsland INSTANCE = new DioxideDynamicIsland();
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath(
            DioxideLite.MOD_ID, "textures/hud/dioxide_logo.png");

    private float width = 176f;
    private float height = 30f;
    private float radius = 15f;
    private long lastNs = System.nanoTime();
    private long lastDataNs;
    private float time;
    private int cachedFps;
    private int cachedPing = -1;
    private String cachedServer = "Singleplayer";
    private List<PlayerInfo> cachedPlayers = List.of();
    private boolean cachedExpanded;
    private float cachedTitleWidth = -1f;
    private float cachedVersionWidth = -1f;
    private float cachedCompactRightWidth = -1f;
    private String cachedFpsText = "";
    private String cachedCompactRightText = "";
    private String cachedCompactRightWidthKey = "";
    private SkijaRenderer.BorrowedImage cachedLogo;

    /** Pre-rendered island body (glow+gradient+outline+gloss) cache. */
    private Image shapeCache;
    private boolean shapeCacheExpanded;
    private int shapeCacheW = -1;
    private int shapeCacheH = -1;
    private float shapeCacheR = -1f;

    /** Padding around the island body for glow spill; baked into the cache. */
    private static final float GLOW_PAD = 14f;

    private DioxideDynamicIsland() {}

    public static DioxideDynamicIsland getInstance() {
        return INSTANCE;
    }

    /** Last island render duration (ns), for the debug performance HUD. */
    public static volatile long lastIslandRenderNanos;

    public void render(Canvas canvas, float screenW, float screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (canvas == null || mc == null || mc.player == null || mc.screen != null) return;
        long t0 = System.nanoTime();
        try {
        boolean expanded = mc.options != null && mc.options.keyPlayerList.isDown();
        updateDataCache(mc, expanded);
        List<PlayerInfo> players = expanded ? cachedPlayers : List.of();
        String server = cachedServer;
        String fps = cachedFpsText;
        int ping = cachedPing;

        float targetW = expanded
                ? Math.min(screenW - 24f, Math.max(340f, Math.min(470f, 300f + players.size() * 2f)))
                : Math.min(screenW - 24f, Math.max(176f,
                        SkijaUi.textWidth(DioxideLite.NAME + " " + DioxideLite.VERSION, 8.5f) + 82f));
        float targetH = expanded
                ? Math.min(screenH - 24f, 84f + Math.max(0, ((players.size() + 5) / 6) - 1) * 14f)
                : 30f;

        float dt = Math.min(0.05f, Math.max(0f,
                (System.nanoTime() - lastNs) / 1_000_000_000f));
        lastNs = System.nanoTime();
        time += dt;

        width = ease(width, targetW, dt, 14f);
        height = ease(height, targetH, dt, 14f);
        radius = ease(radius, expanded ? Math.min(20f, height / 2f) : 15f, dt, 16f);

        float x = (screenW - width) * 0.5f;
        float y = 7f + (float) Math.sin(time * 1.7f) * 0.25f;

        drawIsland(canvas, x, y, width, height, radius, expanded);

        if (expanded) {
            drawExpanded(canvas, mc, players, server, ping, x, y, width, height);
        } else {
            drawCompact(canvas, mc, server, fps, ping, x, y, width, height);
        }
        } finally {
            lastIslandRenderNanos = System.nanoTime() - t0;
        }
    }

    /**
     * Keeps the animation fully frame-rate driven, while relatively expensive
     * Minecraft data lookups are refreshed at ~10 Hz. This is intentionally
     * separate from animation timing so the island never becomes choppy.
     */
    private void updateDataCache(Minecraft mc, boolean expanded) {
        long now = System.nanoTime();
        if (now - lastDataNs < 100_000_000L && expanded == cachedExpanded) return;
        lastDataNs = now;
        cachedExpanded = expanded;
        cachedFps = mc.getFps();
        cachedPing = playerPing(mc);
        cachedServer = serverName(mc);
        cachedFpsText = cachedFps + " FPS";
        cachedCompactRightText = cachedPing >= 0
                ? cachedFpsText + "  " + cachedPing + "ms"
                : cachedFpsText;
        if (!cachedCompactRightText.equals(cachedCompactRightWidthKey)) {
            cachedCompactRightWidthKey = cachedCompactRightText;
            cachedCompactRightWidth = SkijaUi.textWidth(cachedCompactRightText, 8.5f);
        }
        if (expanded) cachedPlayers = tabPlayers(mc);
        else cachedPlayers = List.of();

        if (cachedTitleWidth < 0f) {
            cachedTitleWidth = SkijaUi.textWidth(DioxideLite.NAME, 9.5f);
            cachedVersionWidth = SkijaUi.textWidth("v" + DioxideLite.VERSION, 8.5f);
        }
    }

    private void drawCompact(Canvas canvas, Minecraft mc, String server,
                             String fps, int ping, float x, float y, float w, float h) {
        float iconSize = 20f;
        drawLogo(canvas, x + 8f, y + (h - iconSize) * 0.5f, iconSize);

        String title = DioxideLite.NAME;
        String version = "v" + DioxideLite.VERSION;
        SkijaUi.boldText(canvas, title, x + 34f, y + 8f, 10f, 0xFFF5F8FF, 9.5f);
        SkijaUi.text(canvas, version, x + 34f + cachedTitleWidth + 5f,
                y + 8f, 10f, 0xFF91A0B4, 8.5f);

        String right = cachedCompactRightText;
        float rw = cachedCompactRightWidth;
        SkijaUi.text(canvas, right, x + w - rw - 10f, y + 9f, 9f, 0xFFD7E4F2, 8.5f);
    }

    private static String ircStatusText() {
        IrcModule module = IrcModule.INSTANCE;
        if (!module.isEnabled()) {
            return "IRC  Off";
        }
        if (module.isConnected()) {
            return "IRC  Online  \u00B7  " + IrcModule.ircOnlineUsers().size() + " online";
        }
        return "IRC  Connecting...";
    }

    private static int ircStatusColor() {
        IrcModule module = IrcModule.INSTANCE;
        if (!module.isEnabled()) {
            return 0xFF69788A;
        }
        return module.isConnected() ? 0xFF7FD6A8 : 0xFFE8C96A;
    }

    private void drawExpanded(Canvas canvas, Minecraft mc, List<PlayerInfo> players,
                              String server, int ping, float x, float y, float w, float h) {
        float iconSize = 24f;
        drawLogo(canvas, x + 10f, y + 9f, iconSize);

        SkijaUi.boldText(canvas, DioxideLite.NAME, x + 43f, y + 8f, 10f,
                0xFFF7FAFF, 10f);
        String build = "v" + DioxideLite.VERSION;
        SkijaUi.text(canvas, build, x + 43f + SkijaUi.textWidth(DioxideLite.NAME, 10f) + 6f,
                y + 9f, 9f, 0xFF9AA9BE, 8.5f);

        String status = server;
        if (ping >= 0) status += "  ·  " + ping + "ms";
        SkijaUi.text(canvas, truncate(status, 46), x + 43f, y + 20f, 9f,
                0xFFB9C7D8, 8f);

        String fps = cachedFps + " FPS";
        float fpsW = SkijaUi.textWidth(fps, 8.5f);
        SkijaUi.text(canvas, fps, x + w - fpsW - 11f, y + 10f, 9f,
                0xFF8BD7FF, 8.5f);

        String ircStatus = ircStatusText();
        SkijaUi.text(canvas, ircStatus, x + 43f, y + 31f, 9f,
                ircStatusColor(), 7.5f);

        if (players.isEmpty()) {
            SkijaUi.text(canvas, "NO PLAYERS LISTED", x + 43f, y + 56f, 9f,
                    0xFF69788A, 7.5f);
            return;
        }

        int cols = Math.min(6, Math.max(1, (players.size() + 4) / 5));
        int rows = Math.min(3, (players.size() + cols - 1) / cols);
        float gridTop = y + 50f;
        float cellW = (w - 22f) / cols;
        for (int i = 0; i < Math.min(players.size(), cols * rows); i++) {
            PlayerInfo info = players.get(i);
            int col = i % cols;
            int row = i / cols;
            String name = info.getProfile().name();
            if (name == null || name.isEmpty()) name = "Player";
            name = truncate(name, 14);
            int color = info.getGameMode() == GameType.SPECTATOR ? 0xFF758193 : 0xFFE5EDF7;
            float tx = x + 11f + col * cellW;
            float ty = gridTop + row * 12f;
            SkijaUi.text(canvas, name, tx, ty, 8f, color, 7.5f);
        }
    }

    /**
     * OPAI-like glass pill: glow, deep-black body, inner white edge, top gloss.
     * The whole static body (including its glow layers) is pre-rendered into an
     * off-screen cache per collapsed/expanded state; animation only repositions
     * or scales the cached texture instead of re-running blur passes every frame.
     */
    private void drawIsland(Canvas canvas, float x, float y, float w, float h, float r,
                            boolean expanded) {
        int cw = Math.round(w + GLOW_PAD * 2f);
        int ch = Math.round(h + GLOW_PAD * 2f);
        boolean shapeChanged = shapeCache == null
                || shapeCacheExpanded != expanded
                || Math.abs(shapeCacheW - cw) > 12
                || Math.abs(shapeCacheH - ch) > 12
                || Math.abs(shapeCacheR - r) > 3f;
        if (shapeChanged) {
            if (shapeCache != null) {
                try { shapeCache.close(); } catch (Throwable ignored) {}
            }
            shapeCache = renderIslandShape(cw, ch, r, expanded);
            shapeCacheW = cw;
            shapeCacheH = ch;
            shapeCacheR = r;
            shapeCacheExpanded = expanded;
        }
        if (shapeCache == null) {
            // Fallback: draw the body directly (should not normally happen).
            drawIslandDirect(canvas, x, y, w, h, r);
            return;
        }
        // Snap to whole GUI pixels so the cached glass body and glow keep crisp
        // edges instead of half-pixel blur.
        float dx = Math.round(x - GLOW_PAD);
        float dy = Math.round(y - GLOW_PAD);
        Rect dst = Rect.makeXYWH(dx, dy, cw, ch);
        try (Paint cachePaint = new Paint().setAntiAlias(true)) {
            canvas.drawImageRect(shapeCache,
                    Rect.makeXYWH(0, 0, shapeCache.getWidth(), shapeCache.getHeight()),
                    dst, SamplingMode.LINEAR, cachePaint, true);
        }
    }

    private static Image renderIslandShape(int cw, int ch, float r, boolean expanded) {
        try (Surface surface = Surface.makeRasterN32Premul(cw, ch);
             Paint paint = new Paint().setAntiAlias(true)) {
            Canvas canvas = surface.getCanvas();
            float x = GLOW_PAD;
            float y = GLOW_PAD;
            float w = cw - GLOW_PAD * 2f;
            float h = ch - GLOW_PAD * 2f;
            drawIslandDirect(canvas, x, y, w, h, r);
            return surface.makeImageSnapshot();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** The actual island body drawing, used by the shape cache and the fallback. */
    private static void drawIslandDirect(Canvas canvas, float x, float y, float w, float h, float r) {
        SkijaUi.glowLayer(canvas, x, y, w, h, 7f, 5, () -> {
            SkijaUi.rounded(canvas, x - 1f, y - 1f, w + 2f, h + 2f, r + 1f, 0x263BD8FF);
        });
        SkijaUi.glowLayer(canvas, x, y, w, h, 3.5f, 4, () -> {
            SkijaUi.rounded(canvas, x, y, w, h, r, 0x184FE8FF);
        });

        SkijaUi.gradient(canvas, x, y, w, h,
                0xF806090D, 0xF80E141D, true, r);
        SkijaUi.outline(canvas, x + 0.5f, y + 0.5f, w - 1f, h - 1f,
                Math.max(1f, r - 0.5f), 1f, 0x8AF5F8FF);

        float glossInset = Math.max(8f, r * 0.65f);
        SkijaUi.rounded(canvas, x + glossInset, y + 2f,
                Math.max(1f, w - glossInset * 2f), 1f, 0.5f, 0x34FFFFFF);
        SkijaUi.rounded(canvas, x + 5f, y + h - 4f,
                Math.max(1f, w - 10f), 1f, 0.5f, 0x1200C8FF);
    }

    private void drawLogo(Canvas canvas, float x, float y, float size) {
        try {
            if (cachedLogo == null) cachedLogo = SkijaRenderer.borrowTexture(LOGO);
            if (cachedLogo == null) return;
            Image image = cachedLogo.image();
            Rect src = Rect.makeXYWH(0, 0, image.getWidth(), image.getHeight());
            Rect dst = Rect.makeXYWH(x, y, size, size);
            // The logo is tiny and static; LINEAR keeps the same appearance without
            // allocating a new Paint/texture bridge on every frame.
            try (Paint paint = new Paint().setAntiAlias(true)) {
                canvas.drawImageRect(image, src, dst, SamplingMode.LINEAR, paint, true);
            }
        } catch (Throwable ignored) {
            if (cachedLogo != null) {
                try { cachedLogo.close(); } catch (Throwable ignored2) {}
                cachedLogo = null;
            }
        }
    }

    private static List<PlayerInfo> tabPlayers(Minecraft mc) {
        if (mc.getConnection() == null) return List.of();
        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getListedOnlinePlayers());
        players.sort(Comparator.comparingInt(PlayerInfo::getTabListOrder)
                .thenComparing(i -> i.getProfile().name().toLowerCase()));
        return players;
    }

    private static int playerPing(Minecraft mc) {
        if (mc.player == null || mc.player.connection == null) return -1;
        if (mc.hasSingleplayerServer()) return 0;
        PlayerInfo info = mc.player.connection.getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private static String serverName(Minecraft mc) {
        if (mc.hasSingleplayerServer()) return "Singleplayer";
        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            return truncate(mc.getCurrentServer().ip, 30);
        }
        return "Main Menu";
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static float ease(float current, float target, float dt, float rate) {
        float factor = 1f - (float) Math.exp(-rate * dt);
        return current + (target - current) * factor;
    }
}
