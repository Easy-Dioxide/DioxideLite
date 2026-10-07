package com.dioxidelite.ui.screen;

import com.dioxidelite.render.SkijaRenderer;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.WinScreen;

/** Visual skin for the real vanilla world, server, and options screens. */
public final class VanillaScreenTheme {

    private static Screen transitionScreen;
    private static long transitionStartedAt;
    private static volatile Screen lastRenderedScreen;
    private static volatile long lastRenderedAt;

    private VanillaScreenTheme() {
    }

    public static boolean applies(Screen screen) {
        if (screen == null) {
            return false;
        }
        // The in-game pause menu and the surrounding client-menu navigation
        // screens live in the base "screens" package, so they are matched by
        // class rather than by package prefix.
        if (screen instanceof PauseScreen
                || screen instanceof ShareToLanScreen
                || screen instanceof DirectJoinServerScreen
                || screen instanceof ManageServerScreen
                || screen instanceof ConnectScreen
                || screen instanceof DisconnectedScreen
                || screen instanceof WinScreen
                || screen instanceof CreditsAndAttributionScreen
                || screen instanceof CreateFlatWorldScreen
                || screen instanceof CreateBuffetWorldScreen
                || screen instanceof PresetFlatWorldScreen
                || isFeedbackSubScreen(screen)) {
            return true;
        }
        String packageName = screen.getClass().getPackageName();
        return packageName.startsWith("net.minecraft.client.gui.screens.worldselection")
                || packageName.startsWith("net.minecraft.client.gui.screens.multiplayer")
                || packageName.startsWith("net.minecraft.client.gui.screens.options")
                || packageName.startsWith("net.minecraft.client.gui.screens.packs")
                || packageName.startsWith("net.minecraft.client.gui.screens.social")
                || packageName.startsWith("net.minecraft.client.gui.screens.advancements")
                || packageName.startsWith("net.minecraft.client.gui.screens.achievement");
    }

    /** Feedback/bug-report submenu reachable from the pause menu. */
    public static boolean isFeedbackSubScreen(Screen screen) {
        return screen != null && screen.getClass().getSimpleName().equals("FeedbackSubScreen");
    }

    public static void beginFrame(Screen screen) {
        if (transitionScreen != screen) {
            transitionScreen = screen;
            transitionStartedAt = System.nanoTime();
        }
    }

    public static void drawSkijaBackdrop(Canvas canvas, float width, float height,
                                         float mouseX, float mouseY) {
        lastRenderedScreen = Minecraft.getInstance().screen;
        lastRenderedAt = System.nanoTime();
        ScreenBackdrop.drawMainMenu(canvas, width, height,
                (System.nanoTime() & 0x1FFFFFFFFFFFFFL) / 1_000_000_000.0F,
                mouseX, mouseY, 48);
        VanillaButtonOverlay.INSTANCE.renderSkija(canvas);
    }

    public static boolean canReplaceVanillaButtons(Screen screen) {
        return screen != null && !SkijaRenderer.hasFailed() && lastRenderedScreen == screen
                && System.nanoTime() - lastRenderedAt < 1_000_000_000L;
    }

    public static void drawFallback(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF07090A);
    }

    public static void drawTransition(GuiGraphicsExtractor graphics) {
        int alpha = PageTransition.overlayAlpha(transitionStartedAt);
        if (alpha > 0) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
        }
    }
}
