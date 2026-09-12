package com.dioxidelite.ui.dioxide;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.render.SkijaRenderer;
import com.dioxidelite.render.SkijaUi;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
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
    private float time;

    private DioxideDynamicIsland() {}

    public static DioxideDynamicIsland getInstance() {
        return INSTANCE;
    }

    public void render(Canvas canvas, float screenW, float screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (canvas == null || mc == null || mc.player == null || mc.screen != null) return;

        List<PlayerInfo> players = tabPlayers(mc);
        boolean expanded = mc.options != null && mc.options.keyPlayerList.isDown();

        String server = serverName(mc);
        String fps = mc.getFps() + " FPS";
        int ping = playerPing(mc);

        float targetW = expanded
                ? Math.min(screenW - 24f, Math.max(340f, Math.min(470f, 300f + players.size() * 2f)))
                : Math.min(screenW - 24f, Math.max(176f,
                        SkijaUi.textWidth(DioxideLite.NAME + " " + DioxideLite.VERSION, 8.5f) + 82f));
        float targetH = expanded
                ? Math.min(screenH - 24f, 72f + Math.max(0, ((players.size() + 5) / 6) - 1) * 14f)
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

        drawIsland(canvas, x, y, width, height, radius);

        if (expanded) {
            drawExpanded(canvas, mc, players, server, ping, x, y, width, height);
        } else {
            drawCompact(canvas, mc, server, fps, ping, x, y, width, height);
        }
    }

    private void drawCompact(Canvas canvas, Minecraft mc, String server,
                             String fps, int ping, float x, float y, float w, float h) {
        float iconSize = 20f;
        drawLogo(canvas, x + 8f, y + (h - iconSize) * 0.5f, iconSize);

        String title = DioxideLite.NAME;
        String version = "v" + DioxideLite.VERSION;
        SkijaUi.boldText(canvas, title, x + 34f, y + 8f, 10f, 0xFFF5F8FF, 8.5f);
        SkijaUi.text(canvas, version, x + 34f + SkijaUi.textWidth(title, 8.5f) + 5f,
                y + 8f, 10f, 0xFF91A0B4, 7.5f);

        String right = ping >= 0 ? fps + "  " + ping + "ms" : fps;
        float rw = SkijaUi.textWidth(right, 7.5f);
        SkijaUi.text(canvas, right, x + w - rw - 10f, y + 9f, 9f, 0xFFD7E4F2, 7.5f);
    }

    private void drawExpanded(Canvas canvas, Minecraft mc, List<PlayerInfo> players,
                              String server, int ping, float x, float y, float w, float h) {
        float iconSize = 24f;
        drawLogo(canvas, x + 10f, y + 9f, iconSize);

        SkijaUi.boldText(canvas, DioxideLite.NAME, x + 43f, y + 8f, 10f,
                0xFFF7FAFF, 9f);
        String build = "v" + DioxideLite.VERSION;
        SkijaUi.text(canvas, build, x + 43f + SkijaUi.textWidth(DioxideLite.NAME, 9f) + 6f,
                y + 9f, 9f, 0xFF9AA9BE, 7.5f);

        String status = server;
        if (ping >= 0) status += "  ·  " + ping + "ms";
        SkijaUi.text(canvas, truncate(status, 46), x + 43f, y + 20f, 9f,
                0xFFB9C7D8, 7.2f);

        String fps = mc.getFps() + " FPS";
        float fpsW = SkijaUi.textWidth(fps, 7.5f);
        SkijaUi.text(canvas, fps, x + w - fpsW - 11f, y + 10f, 9f,
                0xFF8BD7FF, 7.5f);

        if (players.isEmpty()) {
            SkijaUi.text(canvas, "NO PLAYERS LISTED", x + 43f, y + 42f, 9f,
                    0xFF69788A, 6.8f);
            return;
        }

        int cols = Math.min(6, Math.max(1, (players.size() + 4) / 5));
        int rows = Math.min(3, (players.size() + cols - 1) / cols);
        float gridTop = y + 36f;
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
            SkijaUi.text(canvas, name, tx, ty, 8f, color, 6.8f);
        }
    }

    /** OPAI-like glass pill: glow, deep-black body, inner white edge, top gloss. */
    private void drawIsland(Canvas canvas, float x, float y, float w, float h, float r) {
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
        try (SkijaRenderer.BorrowedImage borrowed = SkijaRenderer.borrowTexture(LOGO)) {
            if (borrowed == null) return;
            Image image = borrowed.image();
            Rect src = Rect.makeXYWH(0, 0, image.getWidth(), image.getHeight());
            Rect dst = Rect.makeXYWH(x, y, size, size);
            try (Paint paint = new Paint().setAntiAlias(true)) {
                canvas.drawImageRect(image, src, dst, SamplingMode.LINEAR, paint, true);
            }
        } catch (Throwable ignored) {
            // Decorative only; the island must remain usable if the texture fails.
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
