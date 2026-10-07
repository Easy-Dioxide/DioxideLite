package com.dioxidelite.ui.screen;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.i18n.UiText;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.UiTheme;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * Client-branded in-game pause menu.
 *
 * <p>Replaces the vanilla {@link PauseScreen} button grid with a single horizontal
 * rail of glass buttons centred over the animated backdrop the title screen uses, so
 * pausing feels like part of the client rather than a stock Minecraft dialog. The
 * rail is collapsed to the current selection by default and unfolds when the pointer
 * approaches it (or as soon as the keyboard is used), and the label of the selected
 * action floats above its button.</p>
 *
 * <p>Only presentation lives here. Every destination is the same vanilla screen the
 * stock pause menu opens, so gameplay behaviour is unchanged.</p>
 */
public final class GameMenuScreen extends AbstractSkijaScreen {

    // ---------------------------------------------------------------- timing

    private static final float OPEN_DURATION = 0.34F;
    private static final float CLOSE_DURATION = 0.16F;
    private static final float ENTRY_STAGGER = 0.035F;
    private static final float ITEM_STAGGER = 0.045F;
    private static final float RAIL_SPEED = 12.0F;
    private static final float ITEM_SPEED = 16.0F;
    private static final float PRESS_SPEED = 26.0F;
    private static final float SHINE_DURATION = 0.80F;
    private static final float CONFIRM_SPEED = 17.0F;
    /** The destructive choice stays inert until the dialog has finished appearing. */
    private static final float CONFIRM_COOLDOWN = 0.35F;
    private static final float FLASH_SPEED = 13.0F;
    /**
     * The rail starts collapsed, and Minecraft opens a pause screen with the cursor
     * sitting wherever it was — usually dead centre, which is exactly where the
     * collapsed selection lives. Waiting for real pointer travel instead of a timer
     * keeps "collapsed until the player reaches for it" true in that case.
     */
    private static final float POINTER_ARM_DISTANCE = 6.0F;
    private static final long HOVER_LOCK_NANOS = 380_000_000L;
    private static final float TOAST_LIFETIME = 2.40F;
    private static final float TOAST_FADE_IN = 0.18F;
    private static final float TOAST_FADE_OUT = 0.45F;

    // -------------------------------------------------------------- geometry
    // Numbers below are the web reference (72px button, 18px radius, 24px gap)
    // mapped into Minecraft GUI units at half scale, which keeps every ratio.

    private static final float BUTTON = 36.0F;
    private static final float BUTTON_RADIUS = 9.0F;
    private static final float RAIL_GAP = 12.0F;
    private static final float DANGER_GAP = 7.0F;
    private static final float HOT_MARGIN = 5.0F;
    /** Breathing room kept on both sides of the fully expanded rail. */
    private static final float RAIL_MARGIN = 14.0F;
    private static final float ICON = 14.0F;
    private static final float ICON_SPACE = 24.0F;
    private static final float ICON_STROKE = 1.8F;
    private static final float LABEL_MARGIN = 6.0F;
    private static final float LABEL_GAP = 1.6F;
    private static final float LABEL_MAIN_HEIGHT = 11.6F;
    private static final float LABEL_SUB_HEIGHT = 7.6F;
    private static final float LABEL_MAIN_SIZE = 8.4F;
    private static final float LABEL_SUB_SIZE = 5.8F;
    private static final float LABEL_MAIN_TRACKING = 0.50F;
    private static final float LABEL_SUB_TRACKING = 0.92F;
    private static final float CONFIRM_WIDTH = 208.0F;
    private static final float CONFIRM_HEIGHT = 86.0F;
    private static final float CONFIRM_RADIUS = 12.0F;
    private static final float CONFIRM_BUTTON_WIDTH = 78.0F;
    private static final float CONFIRM_BUTTON_HEIGHT = 22.0F;
    private static final float TOAST_BOTTOM = 28.0F;

    // ------------------------------------------------------------------ rows

    private static final int ROWS = 6;
    /** Width of the rail once every button is out, at full scale. */
    private static final float FULL_RAIL_WIDTH =
            ROWS * BUTTON + (ROWS - 1) * RAIL_GAP + DANGER_GAP;
    private static final int ACTION_BACK = 0;
    private static final int ACTION_PROGRESS = 1;
    private static final int ACTION_STATS = 2;
    private static final int ACTION_OPTIONS = 3;
    private static final int ACTION_LAN = 4;
    private static final int ACTION_QUIT = 5;
    private static final int CONFIRM_CANCEL = 0;
    private static final int CONFIRM_OK = 1;

    /**
     * Slugs are kept separate from their resolved text on purpose: {@code UiText.tr}
     * reads the live language, but anything cached into a {@code static} field freezes
     * at class-load, so switching language in-game would leave the menu in the old
     * locale forever. They are resolved per screen open instead (see {@link #resolveText()}).
     */
    private static final String[] LABEL_KEYS = {
            "pause_back_to_game", "pause_progress", "pause_stats",
            "pause_options", "pause_open_to_lan", "pause_quit",
    };
    private static final String[] LABEL_FALLBACKS = {
            "Back to Game", "Progress", "Statistics", "Options", "Open to LAN", "Save and Quit",
    };
    /** Uppercase mono-style caption that sits under the main label. */
    private static final String[] CAPTION_KEYS = {
            "pause_caption_back", "pause_caption_progress", "pause_caption_stats",
            "pause_caption_options", "pause_caption_lan", "pause_caption_quit",
    };
    private static final String[] CAPTION_FALLBACKS = {
            "RESUME SESSION", "ADVANCEMENTS", "STATISTICS", "SETTINGS", "LAN SESSION",
            "QUIT TO TITLE",
    };

    // ---------------------------------------------------------------- colour

    private static final int GLASS_ICON = rgba(255, 255, 255, 0.88F);
    private static final int DANGER_ICON = rgba(255, 130, 150, 0.80F);
    private static final int DANGER_ICON_HOT = rgba(255, 131, 153, 1.0F);
    private static final int LABEL_MAIN = rgba(255, 255, 255, 0.85F);
    private static final int LABEL_SUB = rgba(255, 255, 255, 0.32F);
    private static final int LABEL_DANGER = rgba(255, 130, 150, 0.80F);
    private static final int LABEL_DANGER_SUB = rgba(255, 130, 150, 0.40F);

    private static final Paint GRADIENT_PAINT = new Paint().setAntiAlias(true).setDither(true);
    private static final Paint ICON_PAINT = new Paint()
            .setAntiAlias(true)
            .setMode(PaintMode.STROKE)
            .setStrokeCap(PaintStrokeCap.ROUND)
            .setStrokeJoin(PaintStrokeJoin.ROUND);
    /** Built on first use; icon geometry never changes while the game runs. */
    private static final Path[] ICONS = new Path[ROWS];

    private final float[] presence = new float[ROWS];
    private final float[] delay = new float[ROWS];
    private final float[] highlight = new float[ROWS];
    private final float[] hover = new float[ROWS];
    private final float[] shine = new float[ROWS];
    private final float[] press = new float[ROWS];
    /** One-shot brightening fired the moment a button is committed. */
    private final float[] flash = new float[ROWS];
    private final boolean[] shown = new boolean[ROWS];
    private final String[] labels = new String[ROWS];
    private final String[] captions = new String[ROWS];

    private long openedAt = System.nanoTime();
    private long lastFrame = openedAt;
    private long closedAt = -1L;
    private long hoverLockUntil = 0L;
    private long confirmOpenedAt = -1L;
    private float railOpen;
    private float pointerTravel;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private boolean keyboardMode;
    private int selected = ACTION_BACK;
    private boolean confirmOpen;
    private int confirmFocus = CONFIRM_CANCEL;
    private float confirmReveal;
    private String toast;
    private long toastAt = -1L;
    private Runnable pendingAction;

    public GameMenuScreen() {
        super(Component.literal(DioxideLite.NAME));
    }

    @Override
    protected void init() {
        long now = System.nanoTime();
        openedAt = now;
        lastFrame = now;
        closedAt = -1L;
        hoverLockUntil = now + HOVER_LOCK_NANOS;
        confirmOpenedAt = -1L;
        railOpen = 0.0F;
        pointerTravel = 0.0F;
        lastMouseX = Integer.MIN_VALUE;
        lastMouseY = Integer.MIN_VALUE;
        keyboardMode = false;
        selected = ACTION_BACK;
        confirmOpen = false;
        confirmFocus = CONFIRM_CANCEL;
        confirmReveal = 0.0F;
        toast = null;
        toastAt = -1L;
        pendingAction = null;
        resolveText();
        java.util.Arrays.fill(presence, 0.0F);
        java.util.Arrays.fill(delay, 0.0F);
        java.util.Arrays.fill(highlight, 0.0F);
        java.util.Arrays.fill(hover, 0.0F);
        java.util.Arrays.fill(shine, 0.0F);
        java.util.Arrays.fill(press, 0.0F);
        java.util.Arrays.fill(flash, 0.0F);
        java.util.Arrays.fill(shown, false);
    }

    /** Re-reads every label from the live language so switching locale takes effect at once. */
    private void resolveText() {
        for (int index = 0; index < ROWS; index++) {
            labels[index] = UiText.tr(LABEL_KEYS[index], LABEL_FALLBACKS[index]);
            captions[index] = UiText.tr(CAPTION_KEYS[index], CAPTION_FALLBACKS[index]);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    /** Transparent: closing dissolves the rail straight back into the world. */
    @Override
    protected int backdropColor() {
        return 0x00000000;
    }

    @Override
    protected void drawScreen(Canvas canvas) {
        long now = System.nanoTime();

        // Fire the queued action as soon as the fade-out finishes. This has to
        // run before the zero-reveal early return below, otherwise the action
        // would never fire and the menu would sit on a black screen until a
        // second ESC fell through to Screen#onClose.
        if (closedAt >= 0L && pendingAction != null
                && elapsed(now, closedAt) >= CLOSE_DURATION) {
            Runnable action = pendingAction;
            pendingAction = null;
            action.run();
            return;
        }

        float delta = frameDelta(now);
        updateMotion(delta, now);

        float open = openProgress(now);
        float close = closeProgress(now);
        float backdropReveal = open * (1.0F - close);
        float railReveal = open * (1.0F - Math.min(1.0F, close * 1.4F));
        confirmReveal = approach(confirmReveal, confirmOpen ? 1.0F : 0.0F, delta, CONFIRM_SPEED);
        if (backdropReveal <= 0.001F && railReveal <= 0.001F) {
            return;
        }

        // The rail dissolves first, leaving the branded backdrop to fade out on
        // its own, so the whole thing melts back into the world instead of
        // cutting through black.
        int backdropLayer = canvas.saveLayerAlpha(null, Math.round(255.0F * backdropReveal));
        try {
            drawBackdrop(canvas, now);
        } finally {
            canvas.restoreToCount(backdropLayer);
        }

        int railLayer = canvas.saveLayerAlpha(null, Math.round(255.0F * railReveal));
        try {
            Rail rail = rail();
            for (int index = 0; index < ROWS; index++) {
                drawButton(canvas, rail, index, railReveal);
            }
            drawSelectionLabel(canvas, rail, railReveal);
            drawToast(canvas, now);
        } finally {
            canvas.restoreToCount(railLayer);
        }

        if (confirmReveal > 0.001F) {
            drawConfirm(canvas);
        }
    }

    /** Shared branded backdrop, matching the title screen. */
    private void drawBackdrop(Canvas canvas, long now) {
        ScreenBackdrop.drawMainMenu(canvas, width, height,
                (now & 0x1FFFFFFFFFFFFFL) / 1_000_000_000.0F,
                mouseX, mouseY, 150);
        SkijaUi.gradientDiagonal(canvas, 0, 0, width, height,
                UiTheme.argb(150, 6, 9, 12), UiTheme.argb(120, 3, 5, 7), 0.0F);
    }

    // ------------------------------------------------------------ glass button

    private void drawButton(Canvas canvas, Rail rail, int index, float reveal) {
        float appear = rowReveal(reveal, index);
        float open = presence[index];
        if (appear <= 0.001F || open <= 0.02F) {
            return;
        }

        UiControls.Box box = rail.box(index);
        float scale = railScale();
        float alpha = appear * Math.min(1.0F, open);
        float selectedness = highlight[index];
        float hovered = hover[index];
        float emphasis = Math.max(selectedness, hovered);
        boolean danger = index == ACTION_QUIT;

        float x = box.x();
        float y = box.y() + press[index] * 1.2F * scale;
        float w = box.width();
        float h = box.height();
        float radius = Math.min(BUTTON_RADIUS * scale, Math.min(w, h) * 0.5F);

        // 0 8px 32px rgba(0,0,0,0.45) — laid down before the clip so it can spill.
        SkijaUi.dropShadowRounded(canvas, x, y, w, h, radius, 4.0F, 16.0F,
                fade(rgba(0, 0, 0, 0.45F + 0.05F * emphasis), alpha));

        // 0 0 60px rgba(255,255,255,0.06) / 0 0 70px rgba(255,255,255,0.08)
        if (emphasis > 0.01F) {
            int halo = danger
                    ? rgba(255, 80, 100, 0.12F * emphasis)
                    : rgba(255, 255, 255, 0.06F * emphasis + 0.02F * selectedness);
            for (int ring = 3; ring >= 1; ring--) {
                float grow = ring * 2.2F;
                SkijaUi.rounded(canvas, x - grow, y - grow, w + grow * 2.0F, h + grow * 2.0F,
                        radius + grow, fade(halo, alpha / 3.0F));
            }
        }

        int clip = canvas.save();
        canvas.clipRRect(RRect.makeXYWH(x, y, w, h, radius), ClipMode.INTERSECT, true);
        try {
            // rgba(255,255,255,0.045) base, 0.09 hover, 0.12 selected.
            float glass = 0.045F + 0.045F * emphasis + 0.03F * selectedness;
            SkijaUi.rounded(canvas, x, y, w, h, radius, fade(rgba(255, 255, 255, glass), alpha));

            drawRefraction(canvas, x, y, w, h, alpha);
            drawBottomGlow(canvas, x, y, w, h, alpha);
            drawTopHighlight(canvas, x, y, w, h, alpha);
            drawEdgeLights(canvas, x, y, w, h, alpha, selectedness, hovered);

            if (selectedness > 0.01F) {
                // inset 0 0 24px rgba(255,255,255,0.08)
                SkijaUi.outline(canvas, x + 1.2F, y + 1.2F, w - 2.4F, h - 2.4F,
                        Math.max(0.0F, radius - 1.2F), 2.0F,
                        fade(rgba(255, 255, 255, 0.07F), alpha * selectedness));
                SkijaUi.outline(canvas, x + 3.0F, y + 3.0F, w - 6.0F, h - 6.0F,
                        Math.max(0.0F, radius - 3.0F), 3.0F,
                        fade(rgba(255, 255, 255, 0.04F), alpha * selectedness));
            }

            if (shine[index] > 0.001F) {
                drawShine(canvas, x, y, w, h, shine[index], alpha);
            }

            drawIcon(canvas, index, x, y, w, h, alpha, emphasis, selectedness, hovered, danger);

            // Commit flash: the fade-out takes 0.16s, so without this the menu looks
            // like it ignored the click for the whole transition.
            if (flash[index] > 0.002F) {
                SkijaUi.rounded(canvas, x, y, w, h, radius,
                        fade(rgba(255, 255, 255, 0.34F), alpha * flash[index]));
            }
        } finally {
            canvas.restoreToCount(clip);
        }

        // 1px rgba(255,255,255,0.14) → 0.22 hover → 0.30 selected; the dangerous
        // entry swaps the same hairline for the red family.
        int border = danger
                ? rgba(255, 107, 131, 0.22F + 0.18F * emphasis)
                : rgba(255, 255, 255, 0.14F + 0.08F * emphasis + 0.08F * selectedness);
        SkijaUi.outline(canvas, x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F,
                Math.max(0.0F, radius - 0.5F), 1.0F, fade(border, alpha));

        if (index == ACTION_LAN && lanOpen()) {
            float dot = 3.2F * scale;
            float dotX = x + w - dot - 3.4F * scale;
            float dotY = y + 3.4F * scale;
            int accent = UiTheme.accent();
            SkijaUi.rounded(canvas, dotX - 1.0F, dotY - 1.0F, dot + 2.0F, dot + 2.0F,
                    (dot + 2.0F) * 0.5F, fade(UiTheme.withAlpha(accent, 60), alpha));
            SkijaUi.rounded(canvas, dotX, dotY, dot, dot, dot * 0.5F, fade(accent, alpha));
        }
    }

    /** 180deg rgba(255,255,255,0.52) → 0.24 → transparent over the top 44%. */
    private void drawTopHighlight(Canvas canvas, float x, float y, float w, float h, float alpha) {
        int[] colors = {
                fade(rgba(255, 255, 255, 0.52F), alpha),
                fade(rgba(255, 255, 255, 0.24F), alpha),
                fade(rgba(255, 255, 255, 0.0F), alpha),
        };
        float[] stops = {0.0F, 0.20F, 0.45F};
        verticalGradient(canvas, x + w * 0.06F, y, w * 0.88F, h * 0.44F, colors, stops);
    }

    /** 3px white→pale-blue rail on the left edge, only lit while emphasised. */
    private void drawEdgeLights(Canvas canvas, float x, float y, float w, float h,
                                float alpha, float selectedness, float hovered) {
        float lit = Math.max(selectedness, hovered);
        int[] left = {
                fade(rgba(255, 255, 255, 0.0F), alpha * lit),
                fade(rgba(255, 255, 255, 0.50F), alpha * lit),
                fade(rgba(200, 220, 255, 0.32F), alpha * lit),
                fade(rgba(255, 255, 255, 0.0F), alpha * lit),
        };
        float[] leftStops = {0.0F, 0.33F, 0.66F, 1.0F};
        verticalGradient(canvas, x, y + h * 0.10F, 1.6F, h * 0.80F, left, leftStops);

        int[] right = {
                fade(rgba(255, 255, 255, 0.0F), alpha),
                fade(rgba(255, 255, 255, 0.40F), alpha),
                fade(rgba(255, 255, 255, 0.24F), alpha),
                fade(rgba(255, 255, 255, 0.0F), alpha),
        };
        verticalGradient(canvas, x + w - 1.0F, y + h * 0.14F, 1.0F, h * 0.72F, right, leftStops);
    }

    /** 8px tall blurred ellipse of white light hugging the bottom edge. */
    private void drawBottomGlow(Canvas canvas, float x, float y, float w, float h, float alpha) {
        int[] colors = {
                fade(rgba(255, 255, 255, 0.0F), alpha),
                fade(rgba(255, 255, 255, 0.22F), alpha),
                fade(rgba(255, 255, 255, 0.0F), alpha),
        };
        float[] stops = {0.0F, 0.5F, 1.0F};
        float barHeight = Math.max(1.5F, h * 0.11F);
        horizontalGradient(canvas, x + w * 0.12F, y + h - barHeight - h * 0.04F,
                w * 0.76F, barHeight, colors, stops, barHeight * 0.5F);
        horizontalGradient(canvas, x + w * 0.22F, y + h - barHeight * 0.5F - h * 0.04F,
                w * 0.56F, barHeight * 0.5F, colors, stops, barHeight * 0.25F);
    }

    /** 18%-wide sheet of light leaning across the glass. */
    private void drawRefraction(Canvas canvas, float x, float y, float w, float h, float alpha) {
        float bandWidth = w * 0.20F;
        int[] colors = {
                fade(rgba(255, 255, 255, 0.0F), alpha),
                fade(rgba(255, 255, 255, 0.14F), alpha),
                fade(rgba(200, 220, 255, 0.08F), alpha),
                fade(rgba(255, 255, 255, 0.0F), alpha),
        };
        float[] stops = {0.0F, 0.33F, 0.66F, 1.0F};
        int save = canvas.save();
        try {
            canvas.translate(x + w * 0.20F + bandWidth * 0.5F, y + h * 0.5F);
            canvas.skew((float) Math.tan(Math.toRadians(-16.0)), 0.0F);
            horizontalGradient(canvas, -bandWidth * 0.5F, -h * 0.5F, bandWidth, h,
                    colors, stops, 0.0F);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    /** Highlight that sweeps left→right once while the button is hovered. */
    private void drawShine(Canvas canvas, float x, float y, float w, float h, float progress,
                           float alpha) {
        float bandWidth = w * 0.35F;
        float travel = lerp(-0.70F * w, 1.30F * w, easeInOutCubic(progress));
        int[] colors = {
                fade(rgba(255, 255, 255, 0.0F), alpha),
                fade(rgba(255, 255, 255, 0.55F), alpha),
                fade(rgba(255, 255, 255, 0.32F), alpha),
                fade(rgba(255, 255, 255, 0.0F), alpha),
        };
        float[] stops = {0.0F, 0.33F, 0.66F, 1.0F};
        int save = canvas.save();
        try {
            canvas.translate(x + travel + bandWidth * 0.5F, y + h * 0.5F);
            canvas.skew((float) Math.tan(Math.toRadians(-20.0)), 0.0F);
            horizontalGradient(canvas, -bandWidth * 0.5F, -h * 0.5F, bandWidth, h,
                    colors, stops, 0.0F);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private void drawIcon(Canvas canvas, int index, float x, float y, float w, float h,
                          float alpha, float emphasis, float selectedness, float hovered,
                          boolean danger) {
        Path icon = icon(index);
        if (icon == null) {
            return;
        }
        int color;
        if (danger) {
            color = selectedness > 0.5F
                    ? rgba(255, 255, 255, 1.0F)
                    : lerpColor(DANGER_ICON, DANGER_ICON_HOT, hovered);
        } else {
            color = lerpColor(GLASS_ICON, rgba(255, 255, 255, 1.0F), emphasis);
        }

        float size = ICON * railScale();
        float scale = (size / ICON_SPACE) * (1.0F + 0.06F * emphasis);
        float centerX = x + w * 0.5F;
        float centerY = y + h * 0.5F;
        int save = canvas.save();
        try {
            canvas.translate(centerX - size * 0.5F * (1.0F + 0.06F * emphasis),
                    centerY - size * 0.5F * (1.0F + 0.06F * emphasis));
            canvas.scale(scale, scale);
            ICON_PAINT.setColor(fade(color, alpha)).setStrokeWidth(ICON_STROKE);
            canvas.drawPath(icon, ICON_PAINT);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    // ------------------------------------------------------------ selection text

    private void drawSelectionLabel(Canvas canvas, Rail rail, float reveal) {
        if (confirmOpen) {
            return;
        }
        float selectedness = highlight[selected];
        float appear = rowReveal(reveal, selected);
        float alpha = Math.min(selectedness, appear);
        UiControls.Box box = rail.box(selected);
        if (alpha <= 0.01F || box.width() < 1.0F) {
            return;
        }

        boolean danger = selected == ACTION_QUIT;
        float scale = railScale();
        float mainSize = LABEL_MAIN_SIZE * scale;
        float subSize = LABEL_SUB_SIZE * scale;
        float mainHeight = LABEL_MAIN_HEIGHT * scale;
        float subHeight = LABEL_SUB_HEIGHT * scale;
        float centerX = box.x() + box.width() * 0.5F;
        float subTop = box.y() - (LABEL_MARGIN + LABEL_SUB_HEIGHT) * scale;
        float mainTop = subTop - (LABEL_GAP * scale + mainHeight);

        String caption = caption(selected);
        float captionWidth = UiControls.brandWidth(caption, subSize, LABEL_SUB_TRACKING * scale);
        UiControls.brand(canvas, caption, centerX - captionWidth * 0.5F, subTop,
                subHeight, fade(danger ? LABEL_DANGER_SUB : LABEL_SUB, alpha),
                subSize, LABEL_SUB_TRACKING * scale);

        String label = labels[selected];
        float labelWidth = UiControls.brandWidth(label, mainSize, LABEL_MAIN_TRACKING * scale);
        UiControls.brand(canvas, label, centerX - labelWidth * 0.5F, mainTop,
                mainHeight, fade(danger ? LABEL_DANGER : LABEL_MAIN, alpha),
                mainSize, LABEL_MAIN_TRACKING * scale);
    }

    /** Caption under the selected label; the LAN entry reports its live state. */
    private String caption(int index) {
        if (index == ACTION_LAN) {
            String state = lanOpen()
                    ? UiText.tr("pause_lan_on", "开启")
                    : UiText.tr("pause_lan_off", "关闭");
            return captions[index] + " · " + state;
        }
        return captions[index];
    }

    // ---------------------------------------------------------------- confirm

    private void drawConfirm(Canvas canvas) {
        float reveal = easeOutCubic(confirmReveal);
        SkijaUi.fill(canvas, 0.0F, 0.0F, width, height,
                rgba(0, 0, 0, 0.62F * reveal));

        ConfirmLayout layout = confirmLayout();
        float px = layout.panelX();
        float py = layout.panelY();
        float pw = layout.panelWidth();
        float ph = layout.panelHeight();

        SkijaUi.dropShadowRounded(canvas, px, py, pw, ph, CONFIRM_RADIUS, 6.0F, 24.0F,
                fade(rgba(0, 0, 0, 0.55F), reveal));
        SkijaUi.rounded(canvas, px, py, pw, ph, CONFIRM_RADIUS,
                fade(rgba(12, 14, 18, 0.94F), reveal));
        SkijaUi.outline(canvas, px + 0.5F, py + 0.5F, pw - 1.0F, ph - 1.0F,
                CONFIRM_RADIUS - 0.5F, 1.0F, fade(rgba(255, 255, 255, 0.16F), reveal));
        SkijaUi.rounded(canvas, px + 7.0F, py + 1.0F, Math.max(0.0F, pw - 14.0F), 1.0F, 0.5F,
                fade(rgba(255, 255, 255, 0.14F), reveal));

        String title = UiText.tr("pause_confirm_title", "确定要退出吗？");
        SkijaUi.boldText(canvas, title, px + 16.0F, py + 14.0F, 12.0F,
                fade(rgba(255, 255, 255, 0.92F), reveal), 8.4F);
        String body = UiText.tr("pause_confirm_body", "保存进度后返回标题界面。");
        SkijaUi.text(canvas, UiControls.ellipsize(body, pw - 32.0F), px + 16.0F, py + 30.0F,
                10.0F, fade(rgba(255, 255, 255, 0.48F), reveal), 6.2F);

        drawConfirmButton(canvas, layout.cancelX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight(), CONFIRM_CANCEL, reveal);
        drawConfirmButton(canvas, layout.confirmX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight(), CONFIRM_OK, reveal);
    }

    private void drawConfirmButton(Canvas canvas, float x, float y, float w, float h,
                                   int slot, float alpha) {
        boolean destructive = slot == CONFIRM_OK;
        // The red choice reads as "not yet" for the first moments of the dialog, so a
        // double-click aimed at the button underneath cannot quit the world outright.
        boolean armed = !destructive || confirmArmed();
        boolean focused = confirmFocus == slot && armed;
        float lift = focused ? 1.0F : 0.0F;
        if (!armed) {
            alpha *= 0.45F;
        }

        if (focused) {
            SkijaUi.dropShadowRounded(canvas, x, y, w, h, 9.0F, 4.0F, 14.0F,
                    fade(rgba(0, 0, 0, 0.45F), alpha));
        }
        int danger = UiTheme.DANGER;
        int fill = destructive
                ? fade(danger, 0.90F + 0.10F * lift)
                : rgba(255, 255, 255, 0.05F + 0.05F * lift);
        int border = destructive
                ? fade(danger, 0.55F + 0.45F * lift)
                : rgba(255, 255, 255, 0.16F + 0.16F * lift);
        int ink = destructive
                ? rgba(255, 255, 255, 0.96F)
                : rgba(255, 255, 255, 0.70F + 0.24F * lift);

        SkijaUi.rounded(canvas, x, y, w, h, 9.0F, fade(fill, alpha));
        SkijaUi.outline(canvas, x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, 8.5F, 1.0F,
                fade(border, alpha));

        String label = destructive
                ? UiText.tr("pause_confirm_ok", "确认退出")
                : UiText.tr("pause_confirm_cancel", "取消");
        float textWidth = SkijaUi.boldTextWidth(label, 7.2F);
        SkijaUi.boldText(canvas, label, x + (w - textWidth) * 0.5F, y, h, fade(ink, alpha), 7.2F);
    }

    private ConfirmLayout confirmLayout() {
        float pw = Math.min(CONFIRM_WIDTH, width - 24.0F);
        float ph = CONFIRM_HEIGHT;
        float px = (width - pw) * 0.5F;
        float py = (height - ph) * 0.5F;
        float by = py + ph - 16.0F - CONFIRM_BUTTON_HEIGHT;
        float confirmX = px + pw - 16.0F - CONFIRM_BUTTON_WIDTH;
        float cancelX = confirmX - 8.0F - CONFIRM_BUTTON_WIDTH;
        return new ConfirmLayout(px, py, pw, ph, cancelX, confirmX, by,
                CONFIRM_BUTTON_WIDTH, CONFIRM_BUTTON_HEIGHT);
    }

    // ------------------------------------------------------------------ toast

    private void showToast(String message) {
        toast = message;
        toastAt = System.nanoTime();
    }

    private void drawToast(Canvas canvas, long now) {
        if (toast == null || toastAt < 0L) {
            return;
        }
        float age = elapsed(now, toastAt);
        if (age >= TOAST_LIFETIME) {
            toast = null;
            toastAt = -1L;
            return;
        }
        float alpha = Math.min(1.0F, Math.min(age / TOAST_FADE_IN,
                (TOAST_LIFETIME - age) / TOAST_FADE_OUT));
        float textWidth = SkijaUi.textWidth(toast, 6.6F);
        float padding = 9.0F;
        float boxWidth = textWidth + padding * 2.0F;
        float boxHeight = 16.0F;
        float x = (width - boxWidth) * 0.5F;
        float y = height - TOAST_BOTTOM - boxHeight;

        SkijaUi.rounded(canvas, x, y, boxWidth, boxHeight, boxHeight * 0.5F,
                fade(rgba(0, 0, 0, 0.55F), alpha));
        SkijaUi.outline(canvas, x + 0.5F, y + 0.5F, boxWidth - 1.0F, boxHeight - 1.0F,
                boxHeight * 0.5F, 1.0F, fade(rgba(255, 255, 255, 0.12F), alpha));
        SkijaUi.text(canvas, toast, x + padding, y, boxHeight,
                fade(rgba(255, 255, 255, 0.82F), alpha), 6.6F);
    }

    // ---------------------------------------------------------------- geometry

    private Rail rail() {
        float scale = railScale();
        float buttonHeight = BUTTON * scale;
        float[] widths = new float[ROWS];
        float[] gaps = new float[ROWS];
        float total = 0.0F;
        for (int index = 0; index < ROWS; index++) {
            widths[index] = BUTTON * scale * presence[index];
            gaps[index] = index == 0 ? 0.0F
                    : (index == ACTION_QUIT ? RAIL_GAP + DANGER_GAP : RAIL_GAP)
                            * scale * presence[index];
            total += widths[index] + gaps[index];
        }

        // Rounded origins keep every 1px hairline on a whole screen unit, which is
        // what the stroke needs to land on a physical pixel instead of a half one.
        float cursor = Math.round((width - total) * 0.5F);
        float top = Math.round((height - buttonHeight) * 0.5F);
        UiControls.Box[] boxes = new UiControls.Box[ROWS];
        for (int index = 0; index < ROWS; index++) {
            cursor += gaps[index];
            boxes[index] = new UiControls.Box(cursor, top, widths[index], buttonHeight);
            cursor += widths[index];
        }

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (UiControls.Box box : boxes) {
            if (box.width() < 0.5F) {
                continue;
            }
            minX = Math.min(minX, box.x());
            maxX = Math.max(maxX, box.x() + box.width());
        }
        float margin = HOT_MARGIN * scale;
        UiControls.Box hotZone = minX > maxX
                ? new UiControls.Box(0.0F, 0.0F, 0.0F, 0.0F)
                : new UiControls.Box(minX - margin, top - margin,
                        maxX - minX + margin * 2.0F, buttonHeight + margin * 2.0F);
        return new Rail(boxes, hotZone);
    }

    /**
     * One design unit in screen units. The rail is a fixed-width row, so on the rare
     * combination of a small window and a high GUI scale it would otherwise run off
     * both edges; shrinking the whole thing keeps it whole instead.
     */
    private float railScale() {
        return clamp((width - RAIL_MARGIN * 2.0F) / FULL_RAIL_WIDTH, 0.62F, 1.0F);
    }

    private record Rail(UiControls.Box[] boxes, UiControls.Box hotZone) {

        private UiControls.Box box(int index) {
            return boxes[index];
        }

        private int hit(double mouseX, double mouseY) {
            for (int index = 0; index < boxes.length; index++) {
                if (boxes[index].width() > 0.5F && boxes[index].contains(mouseX, mouseY)) {
                    return index;
                }
            }
            return -1;
        }
    }

    private record ConfirmLayout(float panelX, float panelY, float panelWidth, float panelHeight,
                                 float cancelX, float confirmX, float buttonY,
                                 float buttonWidth, float buttonHeight) {
    }

    // ------------------------------------------------------------------ motion

    private void updateMotion(float delta, long now) {
        Rail rail = rail();
        if (lastMouseX != Integer.MIN_VALUE) {
            pointerTravel += Math.abs(mouseX - lastMouseX) + Math.abs(mouseY - lastMouseY);
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        boolean armed = pointerTravel >= POINTER_ARM_DISTANCE || keyboardMode;
        boolean interactive = armed && !confirmOpen && !isTransitioning();

        int hovered = -1;
        if (interactive) {
            for (int index = 0; index < ROWS; index++) {
                if (rail.box(index).contains(mouseX, mouseY)) {
                    hovered = index;
                }
            }
        }
        boolean wantOpen = interactive && (keyboardMode || rail.hotZone().contains(mouseX, mouseY));
        float before = railOpen;
        railOpen = approach(railOpen, wantOpen ? 1.0F : 0.0F, delta, RAIL_SPEED);
        if (before <= 0.35F && railOpen > 0.35F) {
            hoverLockUntil = now + HOVER_LOCK_NANOS;
        }
        if (hovered >= 0 && railOpen > 0.35F && selected != hovered
                && now >= hoverLockUntil && !keyboardMode) {
            selected = hovered;
        }

        for (int index = 0; index < ROWS; index++) {
            boolean visible = index == selected || railOpen > 0.35F;
            float target = visible ? 1.0F : 0.0F;
            if (target > 0.0F && !shown[index]) {
                // Staggered entrance, mirroring the reference's per-item delay.
                delay[index] = index * ITEM_STAGGER;
                shown[index] = true;
            } else if (target <= 0.0F) {
                shown[index] = false;
                delay[index] = 0.0F;
            }
            if (delay[index] > 0.0F) {
                delay[index] = Math.max(0.0F, delay[index] - delta);
            } else {
                presence[index] = approach(presence[index], target, delta,
                        target > presence[index] ? RAIL_SPEED : ITEM_SPEED * 1.4F);
            }

            highlight[index] = approach(highlight[index], index == selected ? 1.0F : 0.0F,
                    delta, ITEM_SPEED);
            hover[index] = approach(hover[index], index == hovered ? 1.0F : 0.0F,
                    delta, ITEM_SPEED);
            press[index] = approach(press[index], 0.0F, delta, PRESS_SPEED);
            flash[index] = approach(flash[index], 0.0F, delta, FLASH_SPEED);

            float shineTarget = index == hovered && !confirmOpen ? 1.0F : 0.0F;
            float step = delta / SHINE_DURATION;
            if (shine[index] < shineTarget) {
                shine[index] = Math.min(shineTarget, shine[index] + step);
            } else if (shine[index] > shineTarget) {
                shine[index] = Math.max(shineTarget, shine[index] - step);
            }
        }

        if (confirmOpen) {
            ConfirmLayout layout = confirmLayout();
            UiControls.Box cancel = new UiControls.Box(layout.cancelX(), layout.buttonY(),
                    layout.buttonWidth(), layout.buttonHeight());
            UiControls.Box ok = new UiControls.Box(layout.confirmX(), layout.buttonY(),
                    layout.buttonWidth(), layout.buttonHeight());
            if (cancel.contains(mouseX, mouseY)) {
                confirmFocus = CONFIRM_CANCEL;
            } else if (ok.contains(mouseX, mouseY)) {
                confirmFocus = CONFIRM_OK;
            }
        }
    }

    /** 0 while opening from transparent, 1 once the rail is fully shown. */
    private float openProgress(long now) {
        return easeOutCubic(Math.min(1.0F, elapsed(now, openedAt) / OPEN_DURATION));
    }

    /** 0 while idle, 1 once the close fade has fully completed. */
    private float closeProgress(long now) {
        if (closedAt < 0L) {
            return 0.0F;
        }
        return easeInOutCubic(Math.min(1.0F, elapsed(now, closedAt) / CLOSE_DURATION));
    }

    private float rowReveal(float reveal, int index) {
        float offset = index * ENTRY_STAGGER;
        return Math.max(0.0F, Math.min(1.0F,
                (reveal - offset) / (1.0F - offset + 0.0001F)));
    }

    private float frameDelta(long now) {
        float value = Math.max(0.0F, Math.min(0.05F, (now - lastFrame) / 1_000_000_000.0F));
        lastFrame = now;
        return value;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || isTransitioning()) {
            return false;
        }
        if (confirmOpen) {
            ConfirmLayout layout = confirmLayout();
            UiControls.Box cancel = new UiControls.Box(layout.cancelX(), layout.buttonY(),
                    layout.buttonWidth(), layout.buttonHeight());
            UiControls.Box ok = new UiControls.Box(layout.confirmX(), layout.buttonY(),
                    layout.buttonWidth(), layout.buttonHeight());
            if (cancel.contains(event.x(), event.y())) {
                closeConfirm();
            } else if (ok.contains(event.x(), event.y()) && confirmArmed()) {
                confirmQuit();
            }
            return true;
        }

        int hit = rail().hit(event.x(), event.y());
        if (hit >= 0) {
            selected = hit;
            press[hit] = 1.0F;
            flash[hit] = 1.0F;
            playClick();
            activate(hit);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (confirmOpen) {
                closeConfirm();
            } else if (!isTransitioning()) {
                beginClose(() -> minecraft.setScreen(null));
            }
            return true;
        }
        if (isTransitioning()) {
            return true;
        }
        if (confirmOpen) {
            switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A ->
                        moveConfirmFocus(CONFIRM_CANCEL);
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D ->
                        moveConfirmFocus(CONFIRM_OK);
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                    if (confirmFocus == CONFIRM_OK) {
                        if (confirmArmed()) {
                            confirmQuit();
                        }
                    } else {
                        closeConfirm();
                    }
                }
                default -> {
                }
            }
            return true;
        }
        switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> moveSelection(-1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> moveSelection(1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                flash[selected] = 1.0F;
                playClick();
                activate(selected);
            }
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    private void moveSelection(int step) {
        keyboardMode = true;
        selected = (selected + step + ROWS) % ROWS;
        playClick();
    }

    /**
     * The rail is hand-drawn, so it never goes through {@code AbstractWidget}, which
     * means Minecraft's own UI click never fires. Playing it explicitly keeps the
     * menu from feeling dead compared to every stock button in the game.
     */
    private void playClick() {
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** Blocks the destructive choice while the dialog is still fading in. */
    private boolean confirmArmed() {
        return confirmOpenedAt >= 0L
                && elapsed(System.nanoTime(), confirmOpenedAt) >= CONFIRM_COOLDOWN;
    }

    private void moveConfirmFocus(int slot) {
        confirmFocus = slot;
    }

    private boolean isTransitioning() {
        return pendingAction != null || closedAt >= 0L;
    }

    /** Starts the fade-out; the action fires once the rail is fully gone. */
    private void beginClose(Runnable action) {
        pendingAction = action;
        closedAt = System.nanoTime();
    }

    // ----------------------------------------------------------------- actions

    private void activate(int index) {
        switch (index) {
            case ACTION_BACK -> beginClose(() -> minecraft.setScreen(null));
            case ACTION_PROGRESS -> {
                if (minecraft.getConnection() != null) {
                    beginClose(() -> minecraft.setScreen(new AdvancementsScreen(
                            minecraft.getConnection().getAdvancements(), this)));
                }
            }
            case ACTION_STATS -> {
                if (minecraft.player != null) {
                    beginClose(() -> minecraft.setScreen(new StatsScreen(
                            this, minecraft.player.getStats())));
                }
            }
            case ACTION_OPTIONS -> beginClose(() ->
                    minecraft.setScreen(new OptionsScreen(this, minecraft.options, false)));
            case ACTION_LAN -> activateLan();
            case ACTION_QUIT -> openConfirm();
            default -> beginClose(() -> minecraft.setScreen(null));
        }
    }

    /**
     * Minecraft can publish a single-player world to the LAN but offers no way to
     * retract it, so this reads the real state instead of pretending to be a
     * two-way switch: opening the world still routes through the vanilla screen,
     * and once it is open the button reports the live port.
     */
    private void activateLan() {
        if (lanOpen()) {
            showToast(UiText.tr("pause_lan_already", "本局已在局域网开放") + " · " + lanPort());
            return;
        }
        beginClose(() -> minecraft.setScreen(new ShareToLanScreen(this)));
    }

    private void openConfirm() {
        confirmOpen = true;
        confirmFocus = CONFIRM_CANCEL;
        confirmOpenedAt = System.nanoTime();
    }

    private void closeConfirm() {
        confirmOpen = false;
        confirmOpenedAt = -1L;
        playClick();
    }

    private void confirmQuit() {
        confirmOpen = false;
        confirmOpenedAt = -1L;
        playClick();
        beginClose(() -> minecraft.disconnectFromWorld(Component.translatable("menu.quitting")));
    }

    private boolean lanOpen() {
        IntegratedServer server = server();
        return server != null && server.isPublished();
    }

    private int lanPort() {
        IntegratedServer server = server();
        return server == null ? 0 : server.getPort();
    }

    private IntegratedServer server() {
        return minecraft == null ? null : minecraft.getSingleplayerServer();
    }

    // ----------------------------------------------------------------- drawing

    private static void verticalGradient(Canvas canvas, float x, float y, float width,
                                         float height, int[] colors, float[] stops) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        try (Shader shader = Shader.makeLinearGradient(x, y, x, y + height, colors, stops)) {
            GRADIENT_PAINT.setShader(shader).setColor(0xFFFFFFFF).setAlpha(255);
            canvas.drawRect(Rect.makeXYWH(x, y, width, height), GRADIENT_PAINT);
        } finally {
            GRADIENT_PAINT.setShader(null).setAlpha(255);
        }
    }

    private static void horizontalGradient(Canvas canvas, float x, float y, float width,
                                           float height, int[] colors, float[] stops,
                                           float radius) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        try (Shader shader = Shader.makeLinearGradient(x, y, x + width, y, colors, stops)) {
            GRADIENT_PAINT.setShader(shader).setColor(0xFFFFFFFF).setAlpha(255);
            if (radius > 0.0F) {
                canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), GRADIENT_PAINT);
            } else {
                canvas.drawRect(Rect.makeXYWH(x, y, width, height), GRADIENT_PAINT);
            }
        } finally {
            GRADIENT_PAINT.setShader(null).setAlpha(255);
        }
    }

    // -------------------------------------------------------------- icon paths

    /** Line icons on a 24x24 grid, stroke 1.8, round caps — same art as the reference. */
    private static Path icon(int index) {
        Path cached = ICONS[index];
        if (cached != null) {
            return cached;
        }
        try (PathBuilder builder = new PathBuilder()) {
            switch (index) {
                case ACTION_BACK -> builder
                        .moveTo(8.5F, 5.2F)
                        .lineTo(19.0F, 12.0F)
                        .lineTo(8.5F, 18.8F)
                        .closePath();
                case ACTION_PROGRESS -> builder
                        .moveTo(7.0F, 4.0F)
                        .lineTo(17.0F, 4.0F)
                        .lineTo(17.0F, 20.0F)
                        .lineTo(12.0F, 16.1F)
                        .lineTo(7.0F, 20.0F)
                        .closePath();
                case ACTION_STATS -> builder
                        .moveTo(6.0F, 20.0F).lineTo(6.0F, 13.0F)
                        .moveTo(12.0F, 20.0F).lineTo(12.0F, 4.5F)
                        .moveTo(18.0F, 20.0F).lineTo(18.0F, 11.0F);
                case ACTION_OPTIONS -> {
                    builder.addCircle(12.0F, 12.0F, 3.6F);
                    for (int spoke = 0; spoke < 8; spoke++) {
                        double angle = Math.toRadians(spoke * 45.0);
                        float cos = (float) Math.cos(angle);
                        float sin = (float) Math.sin(angle);
                        builder.moveTo(12.0F + 6.4F * cos, 12.0F + 6.4F * sin);
                        builder.lineTo(12.0F + 9.35F * cos, 12.0F + 9.35F * sin);
                    }
                }
                case ACTION_LAN -> {
                    builder.addArc(oval(12.0F, 18.6F, 11.2F), 225.7F, 88.6F);
                    builder.addArc(oval(12.0F, 18.6F, 6.2F), 225.7F, 88.6F);
                    builder.addCircle(12.0F, 18.5F, 0.85F);
                }
                case ACTION_QUIT -> builder
                        .moveTo(12.0F, 2.8F)
                        .lineTo(12.0F, 11.2F)
                        // Open power ring: the gap sits at the top, under the stem.
                        .addArc(oval(12.0F, 12.0F, 7.4F), -42.2F, 264.4F);
                default -> {
                }
            }
            Path built = builder.detach();
            ICONS[index] = built;
            return built;
        }
    }

    private static Rect oval(float centerX, float centerY, float radius) {
        return Rect.makeLTRB(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);
    }

    // ------------------------------------------------------------------- maths

    private static float elapsed(long now, long since) {
        return (now - since) / 1_000_000_000.0F;
    }

    private static float easeOutCubic(float value) {
        float remaining = 1.0F - clamp(value, 0.0F, 1.0F);
        return 1.0F - remaining * remaining * remaining;
    }

    private static float easeInOutCubic(float value) {
        float clamped = clamp(value, 0.0F, 1.0F);
        return clamped < 0.5F
                ? 4.0F * clamped * clamped * clamped
                : 1.0F - (float) Math.pow(-2.0F * clamped + 2.0F, 3.0F) / 2.0F;
    }

    private static float approach(float current, float target, float delta, float speed) {
        return current + (target - current) * (1.0F - (float) Math.exp(-speed * delta));
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int rgba(int red, int green, int blue, float alpha) {
        return (Math.round(clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24)
                | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /** Scales the alpha channel of {@code color} by {@code amount}. */
    private static int fade(int color, float amount) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * clamp(amount, 0.0F, 1.0F));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float amount) {
        float value = clamp(amount, 0.0F, 1.0F);
        int alpha = Math.round(((from >>> 24) & 0xFF)
                + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * value);
        int red = Math.round(((from >> 16) & 0xFF)
                + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * value);
        int green = Math.round(((from >> 8) & 0xFF)
                + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * value);
        int blue = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * value);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
