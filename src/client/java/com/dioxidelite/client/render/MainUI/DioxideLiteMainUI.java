package com.dioxidelite.client.render.MainUI;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.dioxidelite.Config;
import com.dioxidelite.client.Version;
import com.dioxidelite.client.render.font.FontRenderer;
import com.dioxidelite.client.render.skia.LiquidGlassRenderer;
import com.dioxidelite.client.render.skia.SkiaRenderer;
import com.dioxidelite.client.gui.clickgui.NewSettingsScreen;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Library;
import io.github.humbleui.types.RRect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class DioxideLiteMainUI extends Screen {
    private static final Identifier TEXT_TEXTURE_ID = Identifier.fromNamespaceAndPath("dioxide_lite", "mainui_text");
    private static final long HINT_DURATION_MS = 5000L;
    private static final long HINT_FADE_IN_MS = 400L;
    private static final long HINT_FADE_OUT_MS = 800L;
    private static final float SETTINGS_SIZE = 36f;
    private static final float SETTINGS_MARGIN = 24f;
    private static final float THEME_W = 132f;
    private static final float THEME_H = 30f;
    private static final float THEME_GAP = 10f;
    private static final Identifier BACKGROUND_TEXTURE_ID = Identifier.fromNamespaceAndPath("dioxide_lite", "mainui_custom_background");

    private MainUIShader shader;
    private final boolean showEntryHint;
    private final String fixedShaderPath;
    private final List<MenuButton> buttons = new ArrayList<>();
    private TitleHitBox titleHitBox = new TitleHitBox(0f, 0f, 0f, 0f);
    private Surface textSurface;
    private DynamicTexture textTexture;
    private DynamicTexture backgroundTexture;
    private int textX;
    private int textY;
    private int textW;
    private int textH;
    private int textPixelW = -1;
    private int textPixelH = -1;
    private int textGuiW = -1;
    private int textGuiH = -1;
    private boolean nativeLoaded;
    private int pressedIndex = -1;
    private boolean titlePressed;
    private long hintStartMs;
    private boolean settingsOpen;
    private boolean settingsHover;
    private float settingsPanelProgress;
    private float settingsHoverProgress;
    private float backgroundOffsetX;
    private float backgroundOffsetY;
    private int backgroundTextureW = -1;
    private int backgroundTextureH = -1;
    private int lastWindowPixelW = -1;
    private int lastWindowPixelH = -1;
    private String loadedBackground = "";
    private boolean lightSettingsTheme;
    private long lastRenderMs;
    private long introStartMs;
    // Minimal-inspired interaction model: two large destinations split by a central utility ring.
    private float centerOpen;
    private float singleHover;
    private float multiHover;
    private float utilityHover;
    private float utilityPress;
    private boolean entryGate;
    private static final float IDLE_RADIUS = 10f;
    private static final float OPEN_RADIUS = 48f;
    private static final float SWITCH_RADIUS = 7f;

    public DioxideLiteMainUI(Screen parent) {
        this(parent, false);
    }

    public DioxideLiteMainUI(Screen parent, boolean showEntryHint) {
        this(parent, showEntryHint, null);
    }

    private DioxideLiteMainUI(Screen parent, boolean showEntryHint, String fixedShaderPath) {
        super(Component.literal("DioxideLite"));
        this.showEntryHint = showEntryHint;
        this.fixedShaderPath = fixedShaderPath;
    }

    @Override
    protected void init() {
        if (shader != null) shader.close();
        shader = fixedShaderPath == null ? MainUIShader.random() : MainUIShader.named(fixedShaderPath);
        hintStartMs = showEntryHint ? System.currentTimeMillis() : 0L;
        introStartMs = System.currentTimeMillis();
        invalidateTextTexture();
        refreshThemeFromBackground();
        buttons.clear();
        buttons.add(new MenuButton("Singleplayer", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new SelectWorldScreen(returnParent()));
        }));
        buttons.add(new MenuButton("Multiplayer", () -> {
            if (this.minecraft == null) return;
            Screen parent = returnParent();
            Screen screen = this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(parent) : new SafetyScreen(parent);
            this.minecraft.setScreen(screen);
        }));
        buttons.add(new MenuButton("Options", () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new OptionsScreen(returnParent(), this.minecraft.options));
        }));
        buttons.add(new MenuButton("Exit", () -> {
            if (this.minecraft != null) this.minecraft.stop();
        }));
        centerOpen = 0f;
        singleHover = 0f;
        multiHover = 0f;
        utilityHover = 0f;
        utilityPress = 0f;
        entryGate = true;
        updateButtonPositions();
    }

    @Override
    protected void repositionElements() {
        updateButtonPositions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateSettingsPanel(mouseX, mouseY);
        renderMainBackground(graphics, mouseX, mouseY);

        Canvas canvas = SkiaRenderer.begin();
        if (canvas != null) {
            drawPremiumHome(canvas, this.width, this.height, mouseX, mouseY);
        }
        SkiaRenderer.end(graphics, this.width, this.height);

        float intro = Math.min(1f, Math.max(0f, (System.currentTimeMillis() - introStartMs) / 520f));
        float eased = 1f - (float) Math.pow(1f - intro, 3.0);
        if (eased < 1f) {
            int alpha = Math.round((1f - eased) * 150f);
            graphics.fill(0, 0, this.width, this.height, (alpha << 24));
        }
    }

    private void drawPremiumHome(Canvas c, int w, int h, int mouseX, int mouseY) {
        final float time = System.nanoTime() / 1_000_000_000f;
        final long nowMs = System.currentTimeMillis();
        final float dt = lastRenderMs <= 0L ? .016f : Math.min(.05f, Math.max(0f, (nowMs - lastRenderMs) / 1000f));
        lastRenderMs = nowMs;

        MenuLayout layout = menuLayout(w, h);
        float intro = Math.min(1f, Math.max(0f, (nowMs - introStartMs) / 850f));
        intro = 1f - (float) Math.pow(1f - intro, 3f);
        drawThemeSwitcher(c, intro);
        if (entryGate) {
            drawEntryGate(c, layout, intro);
            renderSettingsPlaceholder(c);
            if (settingsOpen) renderSettingsPanel(c);
            return;
        }
        float dx = mouseX - layout.cx;
        float dy = mouseY - layout.cy;
        float distance = (float) Math.hypot(dx, dy);
        boolean centerTarget = distance <= (centerOpen > .08f ? layout.openRadius + 16f : 30f);
        centerOpen = approach(centerOpen, centerTarget ? 1f : 0f, dt, 11f);

        boolean utilityActive = centerOpen > .18f && distance <= layout.openRadius + 15f;
        int side = mouseX < layout.cx ? -1 : 1;
        singleHover = approach(singleHover, !utilityActive && side < 0 ? 1f : 0f, dt, 9f);
        multiHover = approach(multiHover, !utilityActive && side > 0 ? 1f : 0f, dt, 9f);
        utilityHover = approach(utilityHover, utilityActive ? 1f : 0f, dt, 13f);
        utilityPress = approach(utilityPress, 0f, dt, 20f);

        // The backdrop is deliberately kept separate from the UI. This prevents the
        // Skia layer from painting an opaque panel over the single/multi-player scene.
        drawAdaptiveFields(c, layout, intro, time);

        drawDestination(c, "SINGLE PLAYER", layout.leftX, layout.cy, -1f,
                singleHover, intro, 0xFF78CFFF, layout);
        drawDestination(c, "MULTI PLAYER", layout.rightX, layout.cy, 1f,
                multiHover, intro, 0xFFF1A45D, layout);

        float progress = smooth(centerOpen) * intro;
        float radius = lerp(layout.idleRadius, layout.openRadius, progress);
        Paint dot = new Paint().setAntiAlias(true);
        float dotRadius = lerp(layout.idleRadius * .78f, layout.idleRadius * 1.05f, progress);
        dot.setColor(withAlpha(0xFFFFFFFF, .96f * intro));
        c.drawCircle(layout.cx, layout.cy, dotRadius, dot);
        dot.close();

        Paint ring = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE);
        ring.setStrokeWidth(Math.max(.8f, 1.15f * layout.scale));
        ring.setColor(withAlpha(0xFFEFF9FF, .82f * progress));
        if (progress > .01f) c.drawCircle(layout.cx, layout.cy, radius, ring);
        ring.setStrokeWidth(.65f * layout.scale);
        ring.setColor(withAlpha(0xFF78CFFF, .24f * progress));
        if (progress > .01f) c.drawCircle(layout.cx, layout.cy, radius + 7f * layout.scale, ring);
        ring.close();

        if (progress > .01f) {
            drawUtility(c, "OPTIONS", layout.cx + radius * .46f, layout.cy + radius * .30f,
                    utilityHover, 0xFF78CFFF, progress, layout.scale);
            drawUtility(c, "UI", layout.cx, layout.cy - radius * .54f,
                    utilityHover, 0xFFE8F2FF, progress, layout.scale);
            drawUtility(c, "EXIT", layout.cx - radius * .46f, layout.cy + radius * .30f,
                    utilityHover, 0xFFE26964, progress, layout.scale);
            FontRenderer.drawText(c, "↔", layout.cx - layout.scale * 3f,
                    layout.cy + layout.scale * 2.7f, 7f * layout.scale,
                    withAlpha(0xFF10161D, progress));
        }

        // Minimal branding: no competing client names, no permanent HUD panels.
        FontRenderer.drawText(c, "DioxideLite", layout.brandX, layout.brandY,
                layout.brandSize, withAlpha(0xFFFFFFFF, intro));
        FontRenderer.drawText(c, Version.displayName(), layout.brandX,
                layout.brandY + layout.brandSize * .9f, Math.max(6f, layout.brandSize * .48f),
                withAlpha(0xFF9AA7B5, intro));
        FontRenderer.drawText(c, Version.displayName(), layout.footerX, layout.footerY,
                Math.max(6f, layout.brandSize * .52f), withAlpha(0xFF8C98A5, intro * .82f));

        // Keep the original background/settings surface available in the Setsuna-style menu.
        renderSettingsPlaceholder(c);
        if (settingsOpen) renderSettingsPanel(c);
    }

    private void drawThemeSwitcher(Canvas c, float intro) {
        float x = getThemeX();
        float y = SETTINGS_MARGIN + 3f;
        boolean light = isLightTheme();
        int base = light ? 0x111111 : 0xFFFFFF;
        int accent = light ? 0xD17600 : 0x78CFFF;
        try (Paint bg = new Paint().setAntiAlias(true)) {
            bg.setColor(withAlpha(light ? 0xF5F5F5 : 0x081019, .64f * intro));
            c.drawRRect(RRect.makeXYWH(x, y, THEME_W, THEME_H, 10f), bg);
            bg.setMode(PaintMode.STROKE);
            bg.setStrokeWidth(.8f);
            bg.setColor(withAlpha(accent, .55f * intro));
            c.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, THEME_W - 1f, THEME_H - 1f, 10f), bg);
        }
        FontRenderer.drawText(c, Config.isChinese ? "主界面" : "MAIN MENU", x + 12f, y + 12f, 6.5f, withAlpha(light ? 0x4D5559 : 0x9AA7B5, intro));
        FontRenderer.drawText(c, "SETSUNA", x + 12f, y + 23f, 8.5f, withAlpha(base, intro));
        FontRenderer.drawText(c, "↔", x + THEME_W - 22f, y + 20f, 10f, withAlpha(accent, intro));
    }

    private void drawEntryGate(Canvas c, MenuLayout layout, float intro) {
        float pulse = .72f + .18f * (float)Math.sin(System.nanoTime() / 1_000_000_000.0 * 1.8);
        Paint p = new Paint().setAntiAlias(true);
        p.setColor(withAlpha(0xFFFFFFFF, .95f * intro));
        c.drawCircle(layout.cx, layout.cy, 7.5f * layout.scale, p);
        p.setMode(PaintMode.STROKE);
        p.setStrokeWidth(Math.max(.8f, layout.scale));
        p.setColor(withAlpha(0xFF78CFFF, .35f * intro));
        c.drawCircle(layout.cx, layout.cy, 18f * layout.scale, p);
        p.close();
        float tw = FontRenderer.measureTextWidth("CLICK TO START", 11f * layout.scale);
        FontRenderer.drawText(c, "CLICK TO START", layout.cx - tw / 2f, layout.cy + 36f * layout.scale,
                11f * layout.scale, withAlpha(0xFFFFFFFF, pulse * intro));
        FontRenderer.drawText(c, "DioxideLite", layout.brandX, layout.brandY, layout.brandSize, withAlpha(0xFFFFFFFF, intro));
        FontRenderer.drawText(c, Version.displayName(), layout.brandX, layout.brandY + layout.brandSize * .9f,
                Math.max(6f, layout.brandSize * .48f), withAlpha(0xFF9AA7B5, intro));
    }

    private void drawAdaptiveFields(Canvas c, MenuLayout layout, float intro, float time) {
        float extent = layout.extent;
        float pulse = .5f + .5f * (float) Math.sin(time * .55f);
        c.save();
        c.translate(layout.cx, layout.cy);
        c.rotate(8f);
        Paint p = new Paint().setAntiAlias(false);
        p.setColor(withAlpha(0xFF78CFFF, (.075f + singleHover * .055f) * intro));
        c.drawRect(io.github.humbleui.types.Rect.makeXYWH(-extent, -extent, extent, extent * 2f), p);
        p.setColor(withAlpha(0xFFF1A45D, (.055f + multiHover * .045f) * intro));
        c.drawRect(io.github.humbleui.types.Rect.makeXYWH(0, -extent, extent, extent * 2f), p);
        c.restore();
        p.close();

        Paint line = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE);
        line.setStrokeWidth(Math.max(.7f, layout.scale));
        line.setColor(withAlpha(0xFFFFFFFF, (.16f + pulse * .035f) * intro));
        c.drawLine(layout.cx, 0, layout.cx + hShift(layout, .14f), layout.cy * 2f, line);
        line.setColor(withAlpha(0xFF78CFFF, (.08f + singleHover * .10f) * intro));
        c.drawLine(0, layout.cy, layout.cx - layout.openRadius, layout.cy, line);
        line.setColor(withAlpha(0xFFF1A45D, (.07f + multiHover * .09f) * intro));
        c.drawLine(layout.cx + layout.openRadius, layout.cy, layout.width, layout.cy, line);
        line.close();
    }

    private float hShift(MenuLayout layout, float factor) {
        return layout.height * factor;
    }

    private void drawDestination(Canvas c, String label, float anchorX, float centerY,
                                 float direction, float hover, float intro, int accent,
                                 MenuLayout layout) {
        float fontSize = Math.max(11f, 17f * layout.scale);
        float labelWidth = FontRenderer.measureTextWidth(label, fontSize);
        float x = anchorX - labelWidth * .5f + direction * hover * 5f * layout.scale;
        float y = centerY - fontSize * .55f;
        float panelProgress = smooth(hover) * intro;
        float panelWidth = (labelWidth + 44f * layout.scale) * panelProgress;
        float panelHeight = 38f * layout.scale;
        if (panelWidth > .5f) {
            Paint panel = new Paint().setAntiAlias(true).setColor(withAlpha(0xFF02060A, .42f * panelProgress));
            c.drawRRect(RRect.makeXYWH(anchorX - panelWidth * .5f, centerY - panelHeight * .5f,
                    panelWidth, panelHeight, 3f * layout.scale), panel);
            panel.close();
            Paint edge = new Paint().setAntiAlias(false).setColor(withAlpha(accent,
                    panelProgress * (.24f + .24f * hover)));
            c.drawRect(io.github.humbleui.types.Rect.makeXYWH(anchorX - panelWidth * .5f, centerY - panelHeight * .5f,
                    panelWidth, Math.max(1f, layout.scale)), edge);
            c.drawRect(io.github.humbleui.types.Rect.makeXYWH(anchorX - panelWidth * .5f, centerY + panelHeight * .5f - Math.max(1f, layout.scale),
                    panelWidth, Math.max(1f, layout.scale)), edge);
            edge.close();
        }
        FontRenderer.drawText(c, label, x, y, fontSize,
                withAlpha(mixText(0xFFC6CECC, 0xFFFFFFFF, hover), intro * (.72f + .28f * hover)));
        float lineWidth = (34f + hover * 36f) * layout.scale;
        Paint line = new Paint().setAntiAlias(true).setColor(withAlpha(accent,
                intro * (.26f + hover * .60f)));
        c.drawRRect(RRect.makeXYWH(anchorX - lineWidth * .5f, y + fontSize + 8f * layout.scale,
                lineWidth, Math.max(1f, layout.scale), .5f * layout.scale), line);
        line.close();
    }

    private void drawUtility(Canvas c, String label, float x, float y, float hover,
                             int accent, float reveal, float scale) {
        float size = Math.max(5.5f, 6.3f * scale);
        float tw = FontRenderer.measureTextWidth(label, size);
        FontRenderer.drawText(c, label, x - tw * .5f, y, size,
                withAlpha(0xFFFFFFFF, reveal * (.62f + .38f * hover)));
        Paint p = new Paint().setAntiAlias(true).setColor(withAlpha(accent,
                reveal * (.26f + .66f * hover)));
        float width = (8f + hover * 9f) * scale;
        c.drawRRect(RRect.makeXYWH(x - width * .5f, y + 8f * scale,
                width, Math.max(1f, scale), .5f * scale), p);
        p.close();
    }

    private static int mixText(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int) (((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private MenuLayout menuLayout(int w, int h) {
        float scale = clamp(Math.min(w / 1280f, h / 720f), .68f, 1.25f);
        float cx = w * .5f;
        float cy = h * .5f + Math.max(-18f, Math.min(22f, h * .015f));
        float sideOffset = clamp(w * .235f, 150f * scale, 285f * scale);
        return new MenuLayout(w, h, scale, cx, cy, cx - sideOffset, cx + sideOffset,
                11f * scale, 47f * scale, Math.max(w, h) * .9f,
                22f * scale, 17f * scale, 22f * scale, h - 22f * scale, h - 12f * scale);
    }

    private record MenuLayout(int width, int height, float scale, float cx, float cy,
                              float leftX, float rightX, float idleRadius, float openRadius,
                              float extent, float brandX, float brandY, float brandSize,
                              float footerX, float footerY) {}

    private void drawMinimalDestination(Canvas c, String label, float anchorX, float centerY,
                                        float direction, float hover, float intro, int accent, float flash) {
        float size = width < 520 ? 12f : 16f;
        float textW = FontRenderer.measureTextWidth(label, size);
        float x = anchorX - textW * .5f + direction * hover * 5f;
        float y = centerY - 8f;
        if (hover > .01f) {
            float pw = textW + 44f;
            Paint panel = new Paint().setAntiAlias(true).setColor(withAlpha(0xFF05080B, .42f * hover * intro));
            c.drawRRect(RRect.makeXYWH(anchorX - pw/2f, centerY - 20f, pw, 40f, 3f), panel);
            panel.close();
        }
        FontRenderer.drawText(c, label, x, y, size, withAlpha(0xFFFFFFFF, intro * (.68f + .32f * hover)));
        Paint line = new Paint().setAntiAlias(true).setColor(withAlpha(accent, intro * (.25f + hover * (.55f + .15f * flash))));
        float lw = 30f + hover * 40f;
        c.drawRect(io.github.humbleui.types.Rect.makeXYWH(anchorX - lw/2f, centerY + 13f, lw, 1.2f), line);
        line.close();
    }

    private void drawUtility(Canvas c, String label, float x, float y, float hover, int accent, float reveal) {
        float tw = FontRenderer.measureTextWidth(label, 6.5f);
        FontRenderer.drawText(c, label, x - tw/2f, y, 6.5f, withAlpha(0xFFFFFFFF, reveal * (.65f + .35f * hover)));
        Paint p = new Paint().setAntiAlias(true).setColor(withAlpha(accent, reveal * (.28f + .65f * hover)));
        c.drawRect(io.github.humbleui.types.Rect.makeXYWH(x - 4f - hover * 4f, y + 8f, 8f + hover * 8f, 1.2f), p);
        p.close();
    }

    private void drawHomeAction(Canvas c, String label, float centerX, float centerY, int dir, int mx, int my, float intro, int accent) {
        float tw=FontRenderer.measureTextWidth(label, 14f);
        float x=centerX-tw/2f; float y=centerY-7;
        boolean hover=Math.abs(mx-centerX)<(tw/2f+28) && Math.abs(my-centerY)<22;
        if(hover) {
            LiquidGlassRenderer.drawSurface(c, Minecraft.getInstance(), centerX-(tw/2f+28), centerY-22, tw+56, 44, 10f, .58f*intro);
        }
        FontRenderer.drawText(c,label,x+dir*(hover?4:0),y,14,hover?0xFFFFFFFF:0xFFD1D9E5);
        Paint p=new Paint().setAntiAlias(true).setColor(withAlpha(accent, intro*(hover?.9f:.35f)));
        c.drawRect(io.github.humbleui.types.Rect.makeXYWH(centerX-tw/2f,centerY+14,tw*(hover?1f:.62f),1.5f),p); p.close();
    }

    private void drawUtility(Canvas c, String label, float x, float y, int mx, int my, Runnable action) {
        float tw=FontRenderer.measureTextWidth(label,7f);
        boolean h=Math.abs(mx-x)<tw/2f+12 && Math.abs(my-y)<10;
        FontRenderer.drawText(c,label,x-tw/2f,y+3,7,h?0xFF78CFFF:0xFF9BA7B7);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) return false;
        float mx = (float) event.x();
        float my = (float) event.y();

        // Explicit main-menu theme switch. The vanilla screen gets the same control
        // from MainUIScreenManager, so the user can move between both themes at any time.
        if (isInsideThemeSwitcher(mx, my)) {
            Config.useMainUI = false;
            Config.save();
            playClickSound();
            if (minecraft != null) minecraft.setScreen(new TitleScreen());
            return true;
        }

        if (isInsideSettings(mx, my)) {
            settingsOpen = !settingsOpen;
            playClickSound();
            return true;
        }
        if (settingsOpen && isInsideSettingsArea(mx, my)) {
            if (isInsideBackgroundModeBuiltin(mx, my)) {
                Config.mainUICustomBackground = false;
                Config.save();
                destroyBackgroundTexture();
                refreshThemeFromBackground();
                playClickSound();
                return true;
            }
            if (isInsideBackgroundModeCustom(mx, my)) {
                Config.mainUICustomBackground = true;
                Config.save();
                ensureBackgroundTexture();
                refreshThemeFromBackground();
                playClickSound();
                return true;
            }
            if (Config.mainUICustomBackground && isInsideOpenBackgroundFolder(mx, my)) {
                MainUIBackgrounds.openFolder();
                playClickSound();
                return true;
            }
            if (Config.mainUICustomBackground && isInsideBackgroundImageSelect(mx, my)) {
                cycleBackgroundImage();
                playClickSound();
                return true;
            }
            if (Config.mainUICustomBackground && isInsideMouseEffectToggle(mx, my)) {
                Config.mainUIMouseEffect = !Config.mainUIMouseEffect;
                Config.save();
                playClickSound();
                return true;
            }
            return true;
        }

        MenuLayout layout = menuLayout(width, height);
        if (entryGate) {
            entryGate = false;
            introStartMs = System.currentTimeMillis();
            playClickSound();
            return true;
        }
        float dx = mx - layout.cx;
        float dy = my - layout.cy;
        float distance = (float)Math.hypot(dx, dy);

        if (centerOpen > .35f && distance <= layout.openRadius + 10f) {
            utilityPress = 1f;
            if (distance <= layout.idleRadius + 5f) {
                Config.useMainUI = false;
                Config.save();
                playClickSound();
                if (minecraft != null) minecraft.setScreen(new TitleScreen());
                return true;
            }
            float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0f) angle += 360f;
            playClickSound();
            if (angle >= 315f || angle < 45f) {
                if (minecraft != null) minecraft.setScreen(new OptionsScreen(returnParent(), minecraft.options));
            } else if (angle >= 120f && angle < 240f) {
                if (minecraft != null) minecraft.stop();
            } else {
                if (minecraft != null) minecraft.setScreen(new NewSettingsScreen(returnParent()));
            }
            return true;
        }

        float hitHalfW = Math.max(120f, 155f * layout.scale);
        float hitHalfH = Math.max(24f, 30f * layout.scale);
        boolean hitLeft = Math.abs(mx - layout.leftX) <= hitHalfW && Math.abs(my - layout.cy) <= hitHalfH;
        boolean hitRight = Math.abs(mx - layout.rightX) <= hitHalfW && Math.abs(my - layout.cy) <= hitHalfH;
        if (hitLeft || hitRight) {
            playClickSound();
            if (hitLeft) {
                if (minecraft != null) minecraft.setScreen(new SelectWorldScreen(returnParent()));
            } else {
                if (minecraft != null) minecraft.setScreen(minecraft.options.skipMultiplayerWarning
                        ? new JoinMultiplayerScreen(returnParent())
                        : new SafetyScreen(returnParent()));
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return event.button() == 0;
    }

    private void updateButtonPositions() {
        float titleSize = titleSize();
        float titleX = 24f;
        float titleY = 18f;
        float titleW = FontRenderer.measureTextWidth("DioxideLite", titleSize);
        float titleH = FontRenderer.getLineHeight(titleSize);
        titleHitBox = new TitleHitBox(titleX, titleY, titleW, titleH);
        updateTextRegion();
        invalidateTextTexture();
    }

    private float titleSize() {
        float scale = Math.max(0.5f, Math.min(1.0f, (this.width * 2f + this.height) / 6000f + 0.1f));
        return 48f * scale;
    }

    private void renderText(GuiGraphics graphics) {
        ensureTextTexture();
        if (textTexture == null) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXT_TEXTURE_ID, textX, textY, 0f, 0f, textW, textH, textPixelW, textPixelH, textPixelW, textPixelH);
    }

    private void renderMainBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!Config.mainUICustomBackground) {
            shader.render(graphics, mouseX, mouseY);
            return;
        }
        ensureBackgroundTexture();
        if (backgroundTexture == null || backgroundTextureW <= 0 || backgroundTextureH <= 0) {
            shader.render(graphics, mouseX, mouseY);
            return;
        }

        float coverScale = Math.max(this.width / (float) backgroundTextureW, this.height / (float) backgroundTextureH);
        if (Config.mainUIMouseEffect) {
            coverScale *= 1.18f;
            float minW = this.width * 1.16f;
            float minH = this.height * 1.16f;
            coverScale = Math.max(coverScale, minW / backgroundTextureW);
            coverScale = Math.max(coverScale, minH / backgroundTextureH);
        } else {
            coverScale *= 1.08f;
        }
        float drawW = backgroundTextureW * coverScale;
        float drawH = backgroundTextureH * coverScale;
        float targetOffsetX = 0f;
        float targetOffsetY = 0f;
        float maxOffsetX = Math.max(0f, (drawW - this.width) * 0.5f);
        float maxOffsetY = Math.max(0f, (drawH - this.height) * 0.5f);
        if (Config.mainUIMouseEffect) {
            float overflowX = Math.max(0f, drawW - this.width);
            float overflowY = Math.max(0f, drawH - this.height);
            float dragX = Math.max(overflowX * 0.62f, this.width * 0.06f);
            float dragY = Math.max(overflowY * 0.62f, this.height * 0.06f);
            targetOffsetX = ((mouseX / Math.max(1f, (float) this.width)) - 0.5f) * -dragX;
            targetOffsetY = ((mouseY / Math.max(1f, (float) this.height)) - 0.5f) * -dragY;
        }
        targetOffsetX = clamp(targetOffsetX, -maxOffsetX, maxOffsetX);
        targetOffsetY = clamp(targetOffsetY, -maxOffsetY, maxOffsetY);
        backgroundOffsetX += (targetOffsetX - backgroundOffsetX) * 0.08f;
        backgroundOffsetY += (targetOffsetY - backgroundOffsetY) * 0.08f;
        backgroundOffsetX = clamp(backgroundOffsetX, -maxOffsetX, maxOffsetX);
        backgroundOffsetY = clamp(backgroundOffsetY, -maxOffsetY, maxOffsetY);

        int x = Math.round((this.width - drawW) * 0.5f + backgroundOffsetX);
        int y = Math.round((this.height - drawH) * 0.5f + backgroundOffsetY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE_ID, x, y, 0f, 0f, Math.round(drawW), Math.round(drawH), backgroundTextureW, backgroundTextureH, backgroundTextureW, backgroundTextureH);
    }

    private void ensureBackgroundTexture() {
        Minecraft client = Minecraft.getInstance();
        int windowPixelW = client.getWindow().getWidth();
        int windowPixelH = client.getWindow().getHeight();
        if (windowPixelW <= 0 || windowPixelH <= 0) return;
        if (lastWindowPixelW != -1 && (lastWindowPixelW != windowPixelW || lastWindowPixelH != windowPixelH)) {
            destroyBackgroundTexture();
            backgroundOffsetX = 0f;
            backgroundOffsetY = 0f;
        }
        lastWindowPixelW = windowPixelW;
        lastWindowPixelH = windowPixelH;

        String selected = Config.mainUIBackgroundImage == null || Config.mainUIBackgroundImage.isBlank() ? "1.png" : Config.mainUIBackgroundImage;
        if (backgroundTexture != null && selected.equals(loadedBackground)) return;
        destroyBackgroundTexture();

        Path path = MainUIBackgrounds.resolve(selected);
        if (!Files.exists(path)) {
            List<String> files = MainUIBackgrounds.listPngs();
            selected = files.isEmpty() ? "1.png" : files.get(0);
            Config.mainUIBackgroundImage = selected;
            Config.save();
            path = MainUIBackgrounds.resolve(selected);
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) return;
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int argb = image.getRGB(px, py);
                    buffer.put((byte) ((argb >> 16) & 255));
                    buffer.put((byte) ((argb >> 8) & 255));
                    buffer.put((byte) (argb & 255));
                    buffer.put((byte) ((argb >>> 24) & 255));
                }
            }
            buffer.flip();
            backgroundTexture = new DynamicTexture("dioxide_lite:mainui_custom_background", width, height, false);
            client.getTextureManager().register(BACKGROUND_TEXTURE_ID, backgroundTexture);
            GpuTexture gpuTexture = backgroundTexture.getTexture();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(gpuTexture, buffer, NativeImage.Format.RGBA, 0, 0, 0, 0, width, height);
            MemoryUtil.memFree(buffer);
            backgroundTextureW = width;
            backgroundTextureH = height;
            loadedBackground = selected;
            refreshThemeFromImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderEntryHint(GuiGraphics graphics) {
        if (hintStartMs <= 0L) return;
        long elapsed = System.currentTimeMillis() - hintStartMs;
        if (elapsed >= HINT_DURATION_MS) return;

        float alpha;
        if (elapsed < HINT_FADE_IN_MS) {
            alpha = elapsed / (float) HINT_FADE_IN_MS;
        } else if (elapsed > HINT_DURATION_MS - HINT_FADE_OUT_MS) {
            alpha = (HINT_DURATION_MS - elapsed) / (float) HINT_FADE_OUT_MS;
        } else {
            alpha = 1f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));
        int a = Math.round(alpha * 255f);
        if (a <= 0) return;

        String text = Config.isChinese
                ? "点击右上角“SETSUNA”可切换回原版主界面；齿轮仍保留原来的背景切换设置。"
                : "Click the \"DioxideLite\" title in the top-left to return to the vanilla UI. Right-click it to switch styles.";
        int textW = this.font.width(text);
        int x = (this.width - textW) / 2;
        int y = this.height / 2;
        int bgW = textW + 28;
        int bgH = 28;
        int bgX = (this.width - bgW) / 2;
        int bgY = y - 15;
        graphics.fill(bgX, bgY, bgX + bgW, bgY + bgH, (Math.round(alpha * 150f) << 24));
        graphics.drawString(this.font, text, x, y - 4, (a << 24) | 0xFFFFFF, false);
    }

    private void ensureTextTexture() {
        Minecraft client = Minecraft.getInstance();
        float scale = (float) client.getWindow().getGuiScale();
        int targetW = Math.max(1, (int) Math.ceil(textW * scale));
        int targetH = Math.max(1, (int) Math.ceil(textH * scale));
        if (textTexture != null && textPixelW == targetW && textPixelH == targetH && textGuiW == this.width && textGuiH == this.height) return;

        ensureNativeLoaded();
        destroyTextTexture();
        SurfaceProps props = new SurfaceProps(false, PixelGeometry.RGB_H);
        textSurface = Surface.makeRaster(new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, null), targetW, targetH), 0, props);
        textTexture = new DynamicTexture("dioxide_lite:mainui_text", targetW, targetH, false);
        client.getTextureManager().register(TEXT_TEXTURE_ID, textTexture);
        textPixelW = targetW;
        textPixelH = targetH;
        textGuiW = this.width;
        textGuiH = this.height;

        Canvas c = textSurface.getCanvas();
        c.restoreToCount(1);
        c.resetMatrix();
        c.clear(0x00000000);
        c.save();
        c.scale(scale, scale);
        c.translate(-textX, -textY);
        FontRenderer.drawText(c, "DioxideLite", titleHitBox.x, titleHitBox.y + titleHitBox.h * 0.82f, titleSize(), mainTextColor(255));
        renderSettingsPlaceholder(c);
        renderSettingsPanel(c);
        for (MenuButton button : buttons) {
            button.renderText(c);
        }
        renderVersionText(c);
        c.restore();

        Pixmap pixmap = new Pixmap();
        if (!textSurface.peekPixels(pixmap)) {
            pixmap.close();
            return;
        }
        long addr = pixmap.getAddr();
        int byteSize = textPixelH * pixmap.getRowBytes();
        GpuTexture gpuTexture = textTexture.getTexture();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(gpuTexture, MemoryUtil.memByteBuffer(addr, byteSize), NativeImage.Format.RGBA, 0, 0, 0, 0, textPixelW, textPixelH);
        pixmap.close();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new TitleScreen());
        }
    }

    @Override
    public void removed() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        destroyTextTexture();
        destroyBackgroundTexture();
        lastWindowPixelW = -1;
        lastWindowPixelH = -1;
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private void refreshShader() {
        if (shader != null) shader.close();
        shader = MainUIShader.random();
    }

    private DioxideLiteMainUI returnParent() {
        return new DioxideLiteMainUI(null, false, shader == null ? null : shader.fragmentPath());
    }

    private void cycleBackgroundImage() {
        List<String> files = MainUIBackgrounds.listPngs();
        if (files.isEmpty()) return;
        int index = files.indexOf(Config.mainUIBackgroundImage);
        Config.mainUIBackgroundImage = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        destroyBackgroundTexture();
        refreshThemeFromBackground();
    }

    private void refreshThemeFromBackground() {
        lightSettingsTheme = true;
        if (!Config.mainUICustomBackground) return;
        String selected = Config.mainUIBackgroundImage == null || Config.mainUIBackgroundImage.isBlank() ? "1.png" : Config.mainUIBackgroundImage;
        Path path = MainUIBackgrounds.resolve(selected);
        if (!Files.exists(path)) return;
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image != null) refreshThemeFromImage(image);
        } catch (IOException ignored) {
        }
    }

    private void refreshThemeFromImage(BufferedImage image) {
        long total = 0L;
        int samples = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int stepX = Math.max(1, width / 96);
        int stepY = Math.max(1, height / 96);
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                total += (r * 299L + g * 587L + b * 114L) / 1000L;
                samples++;
            }
        }
        if (samples > 0) {
            lightSettingsTheme = total / (float) samples >= 140f;
        }
    }

    private void ensureNativeLoaded() {
        if (nativeLoaded) return;
        Library.load();
        nativeLoaded = true;
    }

    private void invalidateTextTexture() {
        textGuiW = -1;
        textGuiH = -1;
    }

    private void updateTextRegion() {
        float minX = titleHitBox.x;
        float minY = titleHitBox.y;
        float maxX = titleHitBox.x + titleHitBox.w;
        float maxY = titleHitBox.y + titleHitBox.h;
        float settingsX = getSettingsX();
        float settingsY = getSettingsY();
        float panelW = getSettingsPanelWidth();
        float panelH = getSettingsPanelMaxHeight();
        minX = Math.min(minX, settingsX);
        minY = Math.min(minY, settingsY);
        maxX = Math.max(maxX, settingsX + SETTINGS_SIZE);
        maxY = Math.max(maxY, settingsY + SETTINGS_SIZE);
        minX = Math.min(minX, settingsX + SETTINGS_SIZE - panelW);
        maxX = Math.max(maxX, settingsX + SETTINGS_SIZE);
        maxY = Math.max(maxY, settingsY + SETTINGS_SIZE + panelH);
        for (MenuButton button : buttons) {
            minX = Math.min(minX, button.x - 4f);
            minY = Math.min(minY, button.y);
            maxX = Math.max(maxX, button.x + button.w + 4f);
            maxY = Math.max(maxY, button.y + button.h + 10f);
        }
        String version = Version.displayName();
        if (Version.DEBUG) {
            minY = Math.min(minY, this.height - 12f - 11f - 10f);
            maxX = Math.max(maxX, 12f + FontRenderer.measureTextWidth("DEBUG", 11f));
        }
        minX = Math.min(minX, 12f);
        maxX = Math.max(maxX, 12f + FontRenderer.measureTextWidth(version, 11f));
        maxY = Math.max(maxY, this.height - 12f + FontRenderer.getLineHeight(11f));
        textX = Math.max(0, (int) Math.floor(minX - 6f));
        textY = Math.max(0, (int) Math.floor(minY - 6f));
        int right = Math.min(this.width, (int) Math.ceil(maxX + 6f));
        int bottom = Math.min(this.height, (int) Math.ceil(maxY + 6f));
        textW = Math.max(1, right - textX);
        textH = Math.max(1, bottom - textY);
    }

    private float getThemeX() {
        return this.width - SETTINGS_MARGIN - SETTINGS_SIZE - THEME_GAP - THEME_W;
    }

    private boolean isInsideThemeSwitcher(float mx, float my) {
        float x = getThemeX();
        float y = SETTINGS_MARGIN + 3f;
        return mx >= x && mx <= x + THEME_W && my >= y && my <= y + THEME_H;
    }

    private float getSettingsX() {
        return this.width - SETTINGS_MARGIN - SETTINGS_SIZE;
    }

    private float getSettingsY() {
        return SETTINGS_MARGIN;
    }

    private void renderSettingsPlaceholder(Canvas canvas) {
        float x = getSettingsX();
        float y = getSettingsY();
        float fade = 1f - easeOutCubic(settingsPanelProgress);
        if (fade <= 0.01f) return;
        int alpha = Math.round((Config.mainUICustomBackground ? 47f : 190f) + 40f * settingsHoverProgress);
        boolean lightTheme = isLightTheme();
        int baseColor = lightTheme ? 0x111111 : 0xFFFFFF;
        int accentColor = lightTheme ? 0xD17600 : 0xFFD176;
        int bgColor = (Math.round(alpha * fade) << 24) | (lightTheme ? 0xF7F7F7 : 0xFFFFFF);
        int iconColor = (Math.round((230f + 25f * settingsHoverProgress) * fade) << 24) | lerpRgb(baseColor, accentColor, settingsHoverProgress);
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(bgColor);
            canvas.drawRRect(RRect.makeXYWH(x, y, SETTINGS_SIZE, SETTINGS_SIZE, 10f), bg);
        }
        String icon = "\uE8B8";
        float size = 22f;
        float iconW = FontRenderer.measureTextWidth(icon, size, FontRenderer.MATERIAL_SYMBOLS);
        float iconH = FontRenderer.getLineHeight(size, FontRenderer.MATERIAL_SYMBOLS);
        FontRenderer.drawText(canvas, icon, x + (SETTINGS_SIZE - iconW) * 0.5f, y + (SETTINGS_SIZE + iconH) * 0.5f - 2f, size, iconColor, FontRenderer.MATERIAL_SYMBOLS);
    }

    private void renderSettingsPanel(Canvas canvas) {
        if (settingsPanelProgress <= 0.001f) return;
        float t = easeOutCubic(settingsPanelProgress);
        float fullW = getSettingsPanelWidth();
        float fullH = getSettingsPanelHeight();
        float w = fullW * t;
        float h = fullH * t;
        float x = getSettingsX() + SETTINGS_SIZE - w;
        float y = getSettingsY();
        boolean lightTheme = isLightTheme();
        int alpha = Math.round((!Config.mainUICustomBackground ? 196f : (lightTheme ? 118f : 70f)) * t);
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((alpha << 24) | (lightTheme ? 0xF7F7F7 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 16f), bg);
        }
        if (t <= 0.45f) return;

        int textAlpha = Math.round(255f * Math.min(1f, (t - 0.45f) / 0.55f));
        int primary = (textAlpha << 24) | (lightTheme ? 0x111111 : 0xFFFFFF);
        int secondary = (Math.round(textAlpha * 0.72f) << 24) | (lightTheme ? 0x444444 : 0xFFFFFF);
        float contentX = x + 22f;
        float contentY = y + 34f;
        FontRenderer.drawText(canvas, Config.isChinese ? "UI \u8bbe\u7f6e" : "UI Settings", contentX, contentY, 18f, primary);

        float rowY = y + 72f;
        FontRenderer.drawText(canvas, Config.isChinese ? "\u5f53\u524d\u80cc\u666f\u6a21\u5f0f" : "Background Mode", contentX, rowY, 12f, secondary);
        renderChoice(canvas, contentX, rowY + 18f, 112f, Config.isChinese ? "\u5185\u7f6eGLSL" : "Built-in GLSL", !Config.mainUICustomBackground, textAlpha);
        renderChoice(canvas, contentX + 122f, rowY + 18f, 148f, Config.isChinese ? "\u81ea\u5b9a\u4e49\u80cc\u666f\u56fe" : "Custom Image", Config.mainUICustomBackground, textAlpha);

        if (Config.mainUICustomBackground) {
            float folderY = y + 148f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u6253\u5f00\u76ee\u5f55" : "Open Folder", contentX, folderY, 12f, secondary);
            renderButton(canvas, contentX + 198f, folderY - 16f, 72f, Config.isChinese ? "\u6253\u5f00" : "Open", textAlpha);

            float imageY = y + 184f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u9009\u62e9\u56fe\u7247" : "Select Image", contentX, imageY, 12f, secondary);
            renderButton(canvas, contentX + 112f, imageY - 16f, 158f, Config.mainUIBackgroundImage, textAlpha);

            float effectY = y + 220f;
            FontRenderer.drawText(canvas, Config.isChinese ? "\u80cc\u666f\u6548\u679c" : "Background Effects", contentX, effectY, 12f, secondary);
            renderToggle(canvas, contentX, effectY + 18f, Config.isChinese ? "\u9f20\u6807\u4ea4\u4e92\u6548\u679c" : "Mouse Interaction", Config.mainUIMouseEffect, textAlpha);
        }
    }

    private void renderVersionText(Canvas canvas) {
        float versionX = 12f;
        float versionY = this.height - 12f;
        if (Version.DEBUG) {
            float debugY = versionY - FontRenderer.getLineHeight(11f) - 4f;
            FontRenderer.drawText(canvas, "DEBUG", versionX, debugY, 11f, 0xFFFFD34D);
        }
        drawVersionText(canvas, versionX, versionY, 11f, 0xE6FFFFFF);
    }

    private void drawVersionText(Canvas canvas, float x, float y, float size, int baseColor) {
        String version = Version.displayName();
        String type = Version.typeName();
        if (type.isEmpty()) {
            FontRenderer.drawText(canvas, version, x, y, size, baseColor);
            return;
        }

        String marker = "-" + type;
        int typeStart = version.indexOf(marker);
        if (typeStart < 0) {
            FontRenderer.drawText(canvas, version, x, y, size, baseColor);
            return;
        }

        typeStart += 1;
        int typeEnd = typeStart + type.length();
        String before = version.substring(0, typeStart);
        String typed = version.substring(typeStart, typeEnd);
        String after = version.substring(typeEnd);
        int typeColor = Version.TYPE == 1 ? 0xFFFF4444 : 0xFFFFD34D;
        FontRenderer.drawText(canvas, before, x, y, size, baseColor);
        float tx = x + FontRenderer.measureTextWidth(before, size);
        FontRenderer.drawText(canvas, typed, tx, y, size, typeColor);
        FontRenderer.drawText(canvas, after, tx + FontRenderer.measureTextWidth(typed, size), y, size, baseColor);
    }

    private void renderChoice(Canvas canvas, float x, float y, float w, String text, boolean selected, int alpha) {
        boolean lightTheme = isLightTheme();
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(((selected ? Math.round(alpha * 0.28f) : Math.round(alpha * 0.12f)) << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, 28f, 9f), bg);
        }
        int color = (alpha << 24) | (selected ? 0xFFD176 : (lightTheme ? 0x111111 : 0xFFFFFF));
        float tw = FontRenderer.measureTextWidth(text, 11f);
        FontRenderer.drawText(canvas, text, x + (w - tw) * 0.5f, y + 18f, 11f, color);
    }

    private void renderButton(Canvas canvas, float x, float y, float w, String text, int alpha) {
        boolean lightTheme = isLightTheme();
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * 0.14f) << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(x, y, w, 28f, 9f), bg);
        }
        String label = text == null ? "" : text;
        if (FontRenderer.measureTextWidth(label, 11f) > w - 16f) {
            while (label.length() > 1 && FontRenderer.measureTextWidth(label + "...", 11f) > w - 16f) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }
        float tw = FontRenderer.measureTextWidth(label, 11f);
        FontRenderer.drawText(canvas, label, x + (w - tw) * 0.5f, y + 18f, 11f, (alpha << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
    }

    private void renderToggle(Canvas canvas, float x, float y, String text, boolean selected, int alpha) {
        boolean lightTheme = isLightTheme();
        FontRenderer.drawText(canvas, text, x, y + 18f, 12f, (alpha << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
        float tx = x + 218f;
        try (Paint track = new Paint()) {
            track.setAntiAlias(true);
            track.setColor((Math.round(alpha * 0.22f) << 24) | (lightTheme ? 0x111111 : 0xFFFFFF));
            canvas.drawRRect(RRect.makeXYWH(tx, y + 3f, 44f, 24f, 12f), track);
        }
        try (Paint knob = new Paint()) {
            knob.setAntiAlias(true);
            knob.setColor((alpha << 24) | (selected ? 0xFFD176 : (lightTheme ? 0x111111 : 0xFFFFFF)));
            canvas.drawCircle(tx + (selected ? 32f : 12f), y + 15f, 8f, knob);
        }
    }

    private void updateSettingsPanel(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        if (lastRenderMs <= 0L) lastRenderMs = now;
        float dt = Math.min(0.05f, (now - lastRenderMs) / 1000f);
        lastRenderMs = now;

        boolean oldHover = settingsHover;
        settingsHover = isInsideSettings(mouseX, mouseY);
        if (settingsOpen && !isInsideSettingsArea(mouseX, mouseY)) {
            settingsOpen = false;
        }

        float target = settingsOpen ? 1f : 0f;
        float hoverTarget = settingsHover ? 1f : 0f;
        float oldProgress = settingsPanelProgress;
        float oldHoverProgress = settingsHoverProgress;
        settingsPanelProgress += (target - settingsPanelProgress) * Math.min(1f, dt * 10f);
        settingsHoverProgress += (hoverTarget - settingsHoverProgress) * Math.min(1f, dt * 12f);
        if (Math.abs(settingsPanelProgress - target) < 0.002f) settingsPanelProgress = target;
        if (Math.abs(settingsHoverProgress - hoverTarget) < 0.002f) settingsHoverProgress = hoverTarget;
        if (oldHover != settingsHover || Math.abs(oldProgress - settingsPanelProgress) > 0.0005f || Math.abs(oldHoverProgress - settingsHoverProgress) > 0.0005f) {
            invalidateTextTexture();
        }
    }

    private boolean isInsideSettings(float mx, float my) {
        float x = getSettingsX();
        float y = getSettingsY();
        return mx >= x && mx <= x + SETTINGS_SIZE && my >= y && my <= y + SETTINGS_SIZE;
    }

    private boolean isInsideSettingsArea(float mx, float my) {
        if (isInsideSettings(mx, my)) return true;
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth();
        float y = getSettingsY();
        return mx >= x && mx <= x + getSettingsPanelWidth() && my >= y && my <= y + getSettingsPanelHeight();
    }

    private boolean isInsideBackgroundModeBuiltin(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 22f;
        float y = getSettingsY() + 90f;
        return mx >= x && mx <= x + 112f && my >= y && my <= y + 28f;
    }

    private boolean isInsideBackgroundModeCustom(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 144f;
        float y = getSettingsY() + 90f;
        return mx >= x && mx <= x + 148f && my >= y && my <= y + 28f;
    }

    private boolean isInsideOpenBackgroundFolder(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 198f;
        float y = getSettingsY() + 132f;
        return mx >= x && mx <= x + 72f && my >= y && my <= y + 28f;
    }

    private boolean isInsideBackgroundImageSelect(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 112f;
        float y = getSettingsY() + 168f;
        return mx >= x && mx <= x + 158f && my >= y && my <= y + 28f;
    }

    private boolean isInsideMouseEffectToggle(float mx, float my) {
        float x = getSettingsX() + SETTINGS_SIZE - getSettingsPanelWidth() + 22f;
        float y = getSettingsY() + 238f;
        return mx >= x && mx <= x + 270f && my >= y && my <= y + 30f;
    }

    private float getSettingsPanelWidth() {
        return 340f;
    }

    private float getSettingsPanelHeight() {
        return Config.mainUICustomBackground ? 276f : 146f;
    }

    private float getSettingsPanelMaxHeight() {
        return 276f;
    }

    private float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private void destroyTextTexture() {
        if (textSurface != null) {
            textSurface.close();
            textSurface = null;
        }
        if (textTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXT_TEXTURE_ID);
            textTexture = null;
        }
        textPixelW = -1;
        textPixelH = -1;
    }

    private void destroyBackgroundTexture() {
        if (backgroundTexture != null) {
            Minecraft.getInstance().getTextureManager().release(BACKGROUND_TEXTURE_ID);
            backgroundTexture = null;
        }
        backgroundTextureW = -1;
        backgroundTextureH = -1;
        loadedBackground = "";
    }

    private class MenuButton {
        private final String text;
        private final Runnable action;
        private float x;
        private float y;
        private float w;
        private float h;
        private float hover;

        private MenuButton(String text, Runnable action) {
            this.text = text;
            this.action = action;
        }

        private void setBounds(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY, boolean pressed) {
            hover += ((contains(mouseX, mouseY) ? 1f : 0f) - hover) * 0.16f;
            int fill = (Math.round(24f + hover * 34f) << 24) | 0x09131D;
            graphics.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), fill);
            int line = lerpColor(0xA06F8296, pressed ? 0xFFD7F3FF : 0xFF78CFFF, hover);
            graphics.renderOutline(Math.round(x), Math.round(y), Math.round(w), Math.round(h), line);
            if (hover > 0.02f) {
                graphics.fill(Math.round(x + 1f), Math.round(y + h - 2f), Math.round(x + w - 1f), Math.round(y + h - 1f),
                        (Math.round(90f * hover) << 24) | 0x78CFFF);
            }
        }

        private void renderText(Canvas canvas) {
            String first = text.substring(0, 1);
            String rest = text.length() > 1 ? text.substring(1) : "";
            float textScale = Math.min((DioxideLiteMainUI.this.width * 2f + DioxideLiteMainUI.this.height) / 5000f + 1.25f, 3f);
            float size = 10f * textScale;
            float textY = y + 20f;
            FontRenderer.drawText(canvas, first, x, textY, size, mainTextColor(255));
            FontRenderer.drawText(canvas, rest, x + FontRenderer.measureTextWidth(first, size), textY, size, mainTextColor(255));
        }
    }

    private record TitleHitBox(float x, float y, float w, float h) {
        private boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = from >>> 24;
        int rr = (from >> 16) & 255;
        int gr = (from >> 8) & 255;
        int br = from & 255;
        int at = to >>> 24;
        int rt = (to >> 16) & 255;
        int gt = (to >> 8) & 255;
        int bt = to & 255;
        return ((int) (ar + (at - ar) * t) << 24)
                | ((int) (rr + (rt - rr) * t) << 16)
                | ((int) (gr + (gt - gr) * t) << 8)
                | (int) (br + (bt - br) * t);
    }

    private static int lerpRgb(int from, int to, float t) {
        return lerpColor(0xFF000000 | from, 0xFF000000 | to, t) & 0xFFFFFF;
    }

    private int themedTextColor(int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (isLightTheme() ? 0x111111 : 0xFFFFFF);
    }

    private int mainTextColor(int alpha) {
        boolean darkText = Config.mainUICustomBackground && lightSettingsTheme;
        return (Math.max(0, Math.min(255, alpha)) << 24) | (darkText ? 0x111111 : 0xFFFFFF);
    }

    private boolean isLightTheme() {
        return !Config.mainUICustomBackground || lightSettingsTheme;
    }

    private static float approach(float current, float target, float delta, float speed) {
        return current + (target - current) * (1f - (float)Math.exp(-speed * delta));
    }

    private static float smooth(float value) {
        float t = Math.max(0f, Math.min(1f, value));
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0f, Math.min(1f, t));
    }

    private static int withAlpha(int color, float opacity) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * Math.max(0f, Math.min(1f, opacity)))));
        return (a << 24) | (color & 0xFFFFFF);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
