package com.dioxidelite.client.render.MainUI;

import com.dioxidelite.Config;
import com.dioxidelite.client.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * DioxideLite main menu. The composition and motion language follows the
 * supplied Setsuna reference, while the renderer/assets remain DioxideLite-owned.
 * It deliberately uses GuiGraphics only so the title screen never needs a
 * Skia/FBO/CPU readback path.
 */
public final class DioxideLiteMainUI extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("dioxide_lite", "mainui_background");

    private final Screen parent;
    private DynamicTexture backgroundTexture;
    private int backgroundW = -1, backgroundH = -1;
    private String loadedBackground = "";
    private float bgX, bgY;

    private boolean entryGate = true;
    private boolean settingsOpen;
    private long openedAt;
    private long lastFrame;
    private float transition;
    private float singleHover, multiHover, centerOpen;

    public DioxideLiteMainUI(Screen parent) {
        super(Component.literal("DioxideLite"));
        this.parent = parent;
    }

    public DioxideLiteMainUI(Screen parent, boolean ignored) { this(parent); }

    @Override
    protected void init() {
        long now = System.nanoTime();
        openedAt = now;
        lastFrame = now;
        transition = 0.0f;
        entryGate = true;
        settingsOpen = false;
        singleHover = multiHover = centerOpen = 0.0f;
        ensureBackgroundTexture();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0.0f, (now - lastFrame) / 1_000_000_000f));
        lastFrame = now;
        float elapsed = (now - openedAt) / 1_000_000_000f;

        // Setsuna-like staged entrance: background -> center point -> menu lines/text.
        float backgroundIntro = ease(clamp(elapsed / 0.70f));
        if (!entryGate) transition = Math.min(1.0f, transition + dt / 0.62f);
        float content = ease(clamp((elapsed - (entryGate ? 0.0f : 0.0f)) / 0.42f));

        drawBackground(g, mouseX, mouseY, backgroundIntro);
        if (entryGate) {
            drawEntry(g, backgroundIntro, elapsed);
        } else {
            updateHover(mouseX, mouseY, dt);
            drawMenu(g, ease(transition), elapsed);
            drawTopBar(g, ease(transition), mouseX, mouseY);
        }
        if (settingsOpen) drawBackgroundPanel(g, mouseX, mouseY, ease(transition));
    }

    private void drawBackground(GuiGraphics g, int mouseX, int mouseY, float intro) {
        if (Config.mainUICustomBackground && ensureBackgroundTexture()) {
            float scale = Math.max(width / (float) backgroundW, height / (float) backgroundH) * 1.04f;
            float dw = backgroundW * scale, dh = backgroundH * scale;
            float tx = Config.mainUIMouseEffect
                    ? ((mouseX / (float) Math.max(1, width)) - .5f) * -Math.max(8f, (dw - width) * .22f) : 0f;
            float ty = Config.mainUIMouseEffect
                    ? ((mouseY / (float) Math.max(1, height)) - .5f) * -Math.max(8f, (dh - height) * .22f) : 0f;
            bgX += (tx - bgX) * .08f;
            bgY += (ty - bgY) * .08f;
            int alpha = Math.round(255f * intro);
            g.fill(0, 0, width, height, 0xFF000000);
            g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE,
                    Math.round((width - dw) / 2 + bgX), Math.round((height - dh) / 2 + bgY),
                    0, 0, Math.round(dw), Math.round(dh), backgroundW, backgroundH, backgroundW, backgroundH);
            g.fill(0, 0, width, height, ((Math.round(82f * intro) & 255) << 24) | 0x020509);
            return;
        }

        // Original, lightweight recreation of the reference's dark architectural backdrop.
        g.fill(0, 0, width, height, 0xFF000000);
        if (intro <= 0) return;
        int alpha = Math.round(255f * intro);
        int line = ((Math.min(255, Math.round(20f * intro)) & 255) << 24) | 0xD8E6E3;
        int faint = ((Math.min(255, Math.round(10f * intro)) & 255) << 24) | 0x9BB8B2;
        int cx = width / 2;
        int horizon = Math.round(height * 0.56f);

        // Perspective plane / scan geometry, kept intentionally cheap.
        for (int i = 0; i < 9; i++) {
            int y = horizon + i * Math.max(18, height / 24);
            g.fill(0, y, width, y + 1, faint);
        }
        for (int i = -10; i <= 10; i++) {
            int x = cx + i * Math.max(34, width / 18);
            g.fill(x, horizon, x + 1, height, faint);
        }
        g.fill(cx, 0, cx + 1, height, line);
        g.fill(0, horizon, width, horizon + 1, line);

        // Two very subtle architectural light fields.
        int glowA = ((Math.min(255, Math.round(10f * intro)) & 255) << 24) | 0x58DDBE;
        int glowB = ((Math.min(255, Math.round(7f * intro)) & 255) << 24) | 0x8BD8FF;
        g.fill(0, Math.round(height * .18f), Math.round(width * .38f), Math.round(height * .82f), glowA);
        g.fill(Math.round(width * .62f), Math.round(height * .14f), width, Math.round(height * .86f), glowB);

        // Vignette-like edge bands without shaders.
        g.fill(0, 0, width, 22, ((Math.min(255, Math.round(44f * intro)) & 255) << 24));
        g.fill(0, height - 28, width, height, ((Math.min(255, Math.round(58f * intro)) & 255) << 24));
        if (alpha < 255) g.fill(0, 0, width, height, ((255 - alpha) << 24));
    }

    private void drawEntry(GuiGraphics g, float intro, float elapsed) {
        int cx = width / 2, cy = height / 2;
        float pulse = .72f + .28f * (float) Math.sin(elapsed * 5.2f);
        int white = (Math.round(255f * intro) << 24) | 0xF3F7F6;
        int accent = (Math.round(150f * intro) << 24) | 0x58DDBE;

        g.renderOutline(cx - 10, cy - 10, 20, 20, white);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, white);
        g.drawCenteredString(font, "CLICK TO START", cx, cy + 38,
                (Math.round(255f * intro * pulse) << 24) | 0xF2F7F6);
        g.drawCenteredString(font, "DIOXIDELITE", cx, cy - 62, white);
        g.drawCenteredString(font, "SETSUNA UI", cx, cy - 48,
                (Math.round(135f * intro) << 24) | 0x96A39F);
        g.fill(cx - 48, cy + 59, cx + 48, cy + 60, accent);
    }

    private void drawMenu(GuiGraphics g, float t, float elapsed) {
        int cx = width / 2, cy = height / 2;
        float side = Math.max(160f, Math.min(280f, width * .24f));
        float open = ease(centerOpen);
        int accentLeft = 0x58DDBE, accentRight = 0x8BD8FF;

        // Central divider grows out from the center exactly as the menu opens.
        int dividerAlpha = Math.round(220f * t);
        int gap = Math.round(12f + open * 34f);
        int top = Math.round(lerp(cy - gap, 0, t));
        int bottom = Math.round(lerp(cy + gap, height, t));
        g.fill(cx, top, cx + 1, cy - gap, (dividerAlpha << 24) | 0xFFFFFF);
        g.fill(cx, cy + gap, cx + 1, bottom, (dividerAlpha << 24) | 0xFFFFFF);

        float pulse = .5f + .5f * (float) Math.sin(elapsed * 6.0f);
        drawDestination(g, "SINGLE PLAYER", Math.round(cx - side), cy, -1, singleHover, t, accentLeft, pulse);
        drawDestination(g, "MULTI PLAYER", Math.round(cx + side), cy, 1, multiHover, t, accentRight, pulse);

        float r = lerp(7f, 38f, open);
        g.renderOutline(Math.round(cx - r), Math.round(cy - r), Math.round(r * 2), Math.round(r * 2),
                (Math.round(235f * t) << 24) | 0xFFFFFF);
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, (Math.round(255f * (1f - open) * t) << 24) | 0xFFFFFF);

        g.drawCenteredString(font, "DIOXIDELITE", cx, cy - 58,
                (Math.round(255f * t) << 24) | 0xF2F7F6);
        g.drawCenteredString(font, Version.displayName(), cx, cy - 44,
                (Math.round(125f * t) << 24) | 0x96A39F);
        g.drawCenteredString(font, "OPTIONS", cx, cy + 72,
                (Math.round(180f * t) << 24) | 0xC9D3D0);
        g.drawCenteredString(font, "ESC  EXIT", cx, height - 22,
                (Math.round(100f * t) << 24) | 0x73807D);
    }

    private void drawDestination(GuiGraphics g, String text, int cx, int cy, int dir,
                                 float hover, float intro, int accent, float pulse) {
        int tw = font.width(text);
        float x = cx + dir * (hover * 5f - 2f);
        int textAlpha = Math.round(255f * intro * (.72f + hover * .28f));
        int panelAlpha = Math.round((76f + pulse * 24f) * hover * intro);
        if (panelAlpha > 0) {
            g.fill(Math.round(cx - tw / 2f - 23), cy - 20,
                    Math.round(cx + tw / 2f + 23), cy + 19,
                    (panelAlpha << 24) | 0x000000);
        }
        int edge = Math.round((45f + hover * 135f) * intro);
        g.fill(Math.round(cx - tw / 2f - 23), cy + 18,
                Math.round(cx + tw / 2f + 23), cy + 19,
                (edge << 24) | accent);
        g.drawCenteredString(font, text, Math.round(x), cy - 5,
                (textAlpha << 24) | 0xF2F7F6);
    }

    private void drawTopBar(GuiGraphics g, float a, int mx, int my) {
        int alpha = Math.round(255f * a);
        g.drawString(font, "DIOXIDELITE", 22, 18, (alpha << 24) | 0xF2F7F6, false);
        g.drawString(font, "SETSUNA", 22, 31, (Math.round(135f * a) << 24) | 0x58DDBE, false);
        int themeW = 94, bgW = 112;
        int themeX = width - themeW - 24, bgX = themeX - bgW - 10;
        button(g, bgX, 16, bgW, 20,
                Config.mainUICustomBackground ? "BACKGROUND" : "BACKGROUND: SETSUNA", mx, my, a);
        button(g, themeX, 16, themeW, 20, "VANILLA", mx, my, a);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String text, int mx, int my, float a) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int fill = Math.round(255f * a * (hover ? .18f : .08f));
        g.fill(x, y, x + w, y + h, (fill << 24) | 0xFFFFFF);
        g.renderOutline(x, y, w, h, (Math.round(255f * a * (hover ? .65f : .22f)) << 24) | 0x8EA7A2);
        g.drawCenteredString(font, text, x + w / 2, y + 6,
                (Math.round(255f * a * (hover ? 1f : .62f)) << 24) | 0xF2F7F6);
    }

    private void drawBackgroundPanel(GuiGraphics g, int mx, int my, float a) {
        int w = 340, h = 142, x = width - w - 24, y = 48;
        g.fill(x, y, x + w, y + h, 0xE80A1014);
        g.renderOutline(x, y, w, h, (Math.round(255f * a * .42f) << 24) | 0x71807C);
        g.drawString(font, "BACKGROUND", x + 16, y + 16, (Math.round(255f * a) << 24) | 0xF2F7F6, false);
        g.drawString(font, "PVPUtils-style image background", x + 16, y + 31,
                (Math.round(255f * a * .52f) << 24) | 0x96A39F, false);
        button(g, x + 16, y + 48, 145, 22, "IMAGE: " + shortName(Config.mainUIBackgroundImage), mx, my, a);
        button(g, x + 171, y + 48, 145, 22, "OPEN FOLDER", mx, my, a);
        button(g, x + 16, y + 78, 145, 22,
                Config.mainUIMouseEffect ? "MOUSE PARALLAX: ON" : "MOUSE PARALLAX: OFF", mx, my, a);
        button(g, x + 171, y + 78, 145, 22, "CLOSE", mx, my, a);
    }

    private void updateHover(int mx, int my, float dt) {
        int cx = width / 2, cy = height / 2;
        float side = Math.max(160f, Math.min(280f, width * .24f));
        boolean sh = Math.abs(mx - (cx - side)) < font.width("SINGLE PLAYER") / 2f + 42 && Math.abs(my - cy) < 30;
        boolean mh = Math.abs(mx - (cx + side)) < font.width("MULTI PLAYER") / 2f + 42 && Math.abs(my - cy) < 30;
        singleHover = approach(singleHover, sh ? 1f : 0f, dt * 9f);
        multiHover = approach(multiHover, mh ? 1f : 0f, dt * 9f);
        boolean center = Math.abs(mx - cx) < 46 && Math.abs(my - cy) < 46;
        centerOpen = approach(centerOpen, center ? 1f : 0f, dt * 7f);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean consumed) {
        if (e.button() != 0) return false;
        float mx = (float) e.x(), my = (float) e.y();

        int themeX = width - 94 - 24, bgX = themeX - 112 - 10;
        if (!entryGate) {
            if (mx >= themeX && mx <= themeX + 94 && my >= 16 && my <= 36) {
                Config.useMainUI = false;
                Config.save();
                if (minecraft != null) minecraft.setScreen(new TitleScreen());
                return true;
            }
            if (mx >= bgX && mx <= bgX + 112 && my >= 16 && my <= 36) {
                settingsOpen = !settingsOpen;
                return true;
            }
        }

        if (settingsOpen) {
            int x = width - 340 - 24, y = 48;
            if (hit(x + 16, y + 48, 145, 22, mx, my)) { cycleBackgroundImage(); return true; }
            if (hit(x + 171, y + 48, 145, 22, mx, my)) { MainUIBackgrounds.openFolder(); return true; }
            if (hit(x + 16, y + 78, 145, 22, mx, my)) {
                Config.mainUIMouseEffect = !Config.mainUIMouseEffect; Config.save(); return true;
            }
            if (hit(x + 171, y + 78, 145, 22, mx, my)) { settingsOpen = false; return true; }
        }

        if (entryGate) {
            entryGate = false;
            transition = 0f;
            openedAt = System.nanoTime();
            lastFrame = openedAt;
            playClick();
            return true;
        }

        int cx = width / 2, cy = height / 2;
        float side = Math.max(160f, Math.min(280f, width * .24f));
        if (Math.abs(mx - (cx - side)) < font.width("SINGLE PLAYER") / 2f + 42 && Math.abs(my - cy) < 32) {
            if (minecraft != null) minecraft.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (Math.abs(mx - (cx + side)) < font.width("MULTI PLAYER") / 2f + 42 && Math.abs(my - cy) < 32) {
            if (minecraft != null) minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this));
            return true;
        }
        if (Math.abs(mx - cx) < 80 && Math.abs(my - (cy + 72)) < 24) {
            if (minecraft != null) minecraft.setScreen(new OptionsScreen(this, minecraft.options));
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsOpen) { settingsOpen = false; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent != null ? parent : new TitleScreen());
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void removed() { destroyBackground(); }

    private boolean hit(int x, int y, int w, int h, float mx, float my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void playClick() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private void cycleBackgroundImage() {
        List<String> files = MainUIBackgrounds.listPngs();
        if (files.isEmpty()) return;
        int i = files.indexOf(Config.mainUIBackgroundImage);
        Config.mainUIBackgroundImage = files.get((i + 1 + files.size()) % files.size());
        Config.mainUICustomBackground = true;
        Config.save();
        destroyBackground();
        ensureBackgroundTexture();
    }

    private boolean ensureBackgroundTexture() {
        if (!Config.mainUICustomBackground) return false;
        String selected = Config.mainUIBackgroundImage == null || Config.mainUIBackgroundImage.isBlank() ? "1.png" : Config.mainUIBackgroundImage;
        if (backgroundTexture != null && selected.equals(loadedBackground)) return true;
        destroyBackground();
        Path path = MainUIBackgrounds.resolve(selected);
        if (!Files.exists(path)) return false;
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) return false;
            int w = image.getWidth(), h = image.getHeight();
            ByteBuffer b = MemoryUtil.memAlloc(w * h * 4);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int c = image.getRGB(x, y);
                b.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) (c >> 24));
            }
            b.flip();
            backgroundTexture = new DynamicTexture("dioxide_lite:mainui_background", w, h, false);
            Minecraft.getInstance().getTextureManager().register(BACKGROUND_TEXTURE, backgroundTexture);
            GpuTexture tex = backgroundTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(tex, b, NativeImage.Format.RGBA, 0, 0, 0, 0, w, h);
            MemoryUtil.memFree(b);
            backgroundW = w; backgroundH = h; loadedBackground = selected;
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void destroyBackground() {
        if (backgroundTexture != null) {
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            manager.release(BACKGROUND_TEXTURE);
            backgroundTexture = null;
        }
        backgroundW = backgroundH = -1;
        loadedBackground = "";
        bgX = bgY = 0;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float ease(float v) { v = clamp(v); return v * v * (3f - 2f * v); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float approach(float value, float target, float amount) {
        if (value < target) return Math.min(target, value + amount);
        return Math.max(target, value - amount);
    }
    private static String shortName(String s) { if (s == null) return "1.png"; return s.length() > 19 ? s.substring(0, 16) + "..." : s; }
}
