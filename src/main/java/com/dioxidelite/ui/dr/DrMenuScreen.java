package com.dioxidelite.ui.dr;

import com.dioxidelite.ui.screen.AbstractSkijaScreen;

import com.dioxidelite.ui.screen.AltManagerScreen;
import com.dioxidelite.ui.theme.ThemeRuntime;
import com.viaversion.dioxidelitevia.screen.impl.ProtocolSelectionScreen;
import io.github.humbleui.skija.Canvas;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * DR 主菜单宿主：持有 {@link DrMenu}，把输入转发进去，并接上演出、BGM 与各子页面。
 * Host 回调按 DioxideLite 现有能力映射：
 * SINGLE/MULTI/ALT → 三个子页面；OPTIONS → 原版设置；VIA → 内置协议设置；
 * MODS → 装了 ModMenu 才显示；UI_STYLE → 打开 ClickGUI 的主题页。
 */
public final class DrMenuScreen extends AbstractSkijaScreen implements DrMenu.Host {

    private final DrMenu menu = new DrMenu(this);
    private float menuAlpha;
    private boolean bgmStarted;

    public DrMenuScreen() {
        super(Component.literal("DELTARUNE"));
    }

    @Override
    protected void init() {
        menu.reset();
    }

    @Override
    protected void drawScreen(Canvas canvas) {
        // 演出未结束时整个屏幕让给演出，播完自动接管菜单
        if (DrIntroPlayer.isActive() && !DrIntroPlayer.isFinished()) {
            if (!DrIntroPlayer.render(canvas, width, height)) {
                canvas.drawColor(0xFF000000);
            }
            return;
        }
        if (DrIntroPlayer.isActive()) {
            DrIntroPlayer.close();
        }
        if (!bgmStarted) {
            bgmStarted = true;
            DrSound.startBgm();
        }
        menu.mouseMoved(mouseX, mouseY, width, height);
        menuAlpha = Math.min(1f, menuAlpha + 0.05f);
        canvas.drawColor(0xFF000000);
        menu.render(canvas, width, height, menuAlpha);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && DrIntroPlayer.isActive() && !DrIntroPlayer.isFinished()) {
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        return menu.mouseClicked(event.x(), event.y(), width, height, event.button())
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DrIntroPlayer.isActive() && !DrIntroPlayer.isFinished()) {
            return true;
        }
        if (menu.keyPressed(event.key())) {
            return true;
        }
        // 主菜单不吃 Esc，避免误退回原版标题屏
        if (event.isEscape()) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
        bgmStarted = false;
        DrSound.stopBgm();
        super.removed();
    }

    // DrMenu.Host

    @Override
    public void open(DrMenu.Entry entry) {
        switch (entry) {
            case SINGLE -> minecraft.setScreen(new DrSingleplayerScreen());
            case MULTI -> minecraft.setScreen(new DrMultiplayerScreen());
            case ALT -> minecraft.setScreen(new AltManagerScreen(this));
        }
    }

    @Override
    public void options() {
        minecraft.setScreen(new OptionsScreen(this, minecraft.options, false));
    }

    @Override
    public void via() {
        minecraft.setScreen(ProtocolSelectionScreen.INSTANCE.get(this));
    }

    @Override
    public void modMenu() {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
            menu.flash(DrThemeState.isChinese ? "未安装 ModMenu" : "ModMenu not installed");
            return;
        }
        // ModMenu 是 compileOnly：没装时连类都不能出现在常量池里，否则类验证就崩
        try {
            Class<?> screenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            minecraft.setScreen((Screen) screenClass.getConstructor(Screen.class).newInstance(this));
        } catch (Throwable t) {
            menu.flash(DrThemeState.isChinese ? "ModMenu 无法打开" : "ModMenu unavailable");
        }
    }

    @Override
    public void uiSettings() {
        ThemeRuntime.openThemeScreen(this);
    }

    @Override
    public void quit() {
        minecraft.stop();
    }

    @Override
    public void sound(DrMenu.Sfx sfx) {
        DrSound.play(switch (sfx) {
            case MOVE -> DrSound.Sfx.MOVE;
            case SELECT -> DrSound.Sfx.SELECT;
            case BACK -> DrSound.Sfx.BACK;
            case ERROR -> DrSound.Sfx.ERROR;
        });
    }
}
