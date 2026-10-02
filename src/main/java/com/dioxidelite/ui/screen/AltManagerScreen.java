package com.dioxidelite.ui.screen;

import com.dioxidelite.manager.AltManager;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.UiTheme;
import com.dioxidelite.ui.dr.DrFont;
import com.dioxidelite.ui.dr.DrPage;
import com.dioxidelite.ui.dr.DrPageRenderer;
import com.dioxidelite.ui.dr.DrSound;
import com.dioxidelite.ui.dr.DrTheme;
import com.dioxidelite.ui.dr.DrThemeState;
import com.dioxidelite.util.alt.Alt;
import com.dioxidelite.util.alt.MicrosoftAuthService;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Account picker: a scrollable account-card grid over a bottom action bar. */
public final class AltManagerScreen extends AbstractSkijaScreen {

    private static final float CARD_HEIGHT = 46.0F;
    private static final float CARD_GAP = 8.0F;

    private final Screen parent;
    private final UiControls.TextInput offlineName = new UiControls.TextInput(
            16,
            false,
            codePoint -> codePoint == '_' || codePoint >= '0' && codePoint <= '9'
                    || codePoint >= 'A' && codePoint <= 'Z'
                    || codePoint >= 'a' && codePoint <= 'z'
    );
    private final UiControls.TextInput minecraftToken = new UiControls.TextInput(4096, true);

    private List<Alt> accounts = List.of();
    private Alt selected;
    private int firstVisible;
    private boolean loginRunning;
    private String status = "Select an account";
    private boolean statusError;
    // DR 主题下的列表版式（320x240），非 DR 时完全不参与
    private final DrPageRenderer drPage = new DrPageRenderer(new DrPageSource());

    public AltManagerScreen(Screen parent) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
        offlineName.setPlaceholder("Offline username");
        minecraftToken.setPlaceholder("Minecraft access token");
    }

    @Override
    protected void init() {
        AltManager.INSTANCE.load();
        refreshAccounts();
        if (DrTheme.active()) {
            drInputBounds();
        } else {
            updateInputBounds(layout());
        }
    }

    @Override
    protected void drawScreen(Canvas canvas) {
        if (DrTheme.active()) {
            drawDrScreen(canvas);
            return;
        }
        Layout layout = layout();
        updateInputBounds(layout);
        clampFirstVisible(layout);
        ScreenBackdrop.draw(canvas, width, height, 160);
        UiControls.panel(canvas, layout.workspace);
        drawTopBar(canvas, layout);
        drawGrid(canvas, layout);
        drawActionBar(canvas, layout);
        drawStatusBar(canvas, layout);
    }

    // DR 版式

    private static final String[] DR_ACTIONS_CN = {"登录", "添加", "微软", "删除", "返回"};
    private static final String[] DR_ACTIONS_EN = {"USE", "ADD", "MICROSOFT", "REMOVE", "BACK"};

    private void drawDrScreen(Canvas canvas) {
        drInputBounds();
        drPage.render(canvas, width, height, 255);
        float s = DrPage.scale(width, height);
        float ox = DrPage.originX(width, s);
        float oy = DrPage.originY(height, s);
        canvas.save();
        try {
            canvas.translate(ox, oy);
            canvas.scale(s, s);
            drawDrInput(canvas);
            drawDrStatus(canvas);
        } finally {
            canvas.restore();
        }
    }

    /** 离线名输入框落在第三行空位，登录流程需要它 */
    private void drInputBounds() {
        offlineName.setBounds(new UiControls.Box(DrPage.BOX_X, 156f, DrPage.XL, 16f));
    }

    private void drawDrInput(Canvas canvas) {
        String text = offlineName.text();
        int color = DrBackdropTextColor();
        if (text.isEmpty() && !offlineName.isFocused()) {
            DrFont.drawShadowed(canvas, offlinePlaceholder(), DrPage.BOX_X + 4f, 159f, DrPage.FONT,
                    DrBackdropArgba(color, 150));
        } else {
            offlineName.draw(canvas, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        if (offlineName.isFocused()) {
            float cursor = DrPage.BOX_X + 4f + DrFont.measureWidth(text, DrPage.FONT);
            DrFont.drawShadowed(canvas, "_", cursor, 159f, DrPage.FONT, DrBackdropArgba(color, 230));
        }
    }

    private void drawDrStatus(Canvas canvas) {
        String mode = loginRunning ? "WORKING" : statusError ? "FAILED" : "READY";
        String line = status == null ? "" : status;
        DrFont.drawShadowed(canvas, DrFont.truncate(line, DrPage.FONT, 200f, ".."), 8f, 22f, DrPage.FONT,
                DrBackdropArgba(DrBackdropTextColor(), 220));
        DrFont.drawShadowed(canvas, mode, 8f, 4f, 6f, DrBackdropArgba(DrBackdropTextColor(), 160));
    }

    private String offlinePlaceholder() {
        return DrThemeState.isChinese ? "输入离线用户名" : "Offline username";
    }

    private static int DrBackdropTextColor() {
        return DrPage.colA();
    }

    private static int DrBackdropArgba(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private List<DrPage.Action> drActions() {
        String[] labels = DrThemeState.isChinese ? DR_ACTIONS_CN : DR_ACTIONS_EN;
        return DrPage.layoutActions(labels);
    }

    private void drAction(int index) {
        switch (index) {
            case 0 -> {
                drPlay(DrSound.Sfx.SELECT);
                loginSelected();
            }
            case 1 -> {
                drPlay(DrSound.Sfx.SELECT);
                addOffline();
            }
            case 2 -> {
                drPlay(DrSound.Sfx.SELECT);
                startMicrosoftLogin();
            }
            case 3 -> {
                drPlay(DrSound.Sfx.SELECT);
                removeSelected();
            }
            default -> {
                drPlay(DrSound.Sfx.BACK);
                onClose();
            }
        }
    }

    private void drPlay(DrSound.Sfx sfx) {
        DrSound.play(sfx);
    }

    private int drSelectedIndex() {
        for (int i = 0; i < accounts.size(); i++) {
            if (sameAccount(accounts.get(i), selected)) {
                return i;
            }
        }
        return -1;
    }

    private boolean drMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float s = DrPage.scale(width, height);
        float lx = (float) ((event.x() - DrPage.originX(width, s)) / s);
        float ly = (float) ((event.y() - DrPage.originY(height, s)) / s);
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && offlineName.click(lx, ly, doubleClick)) {
            return true;
        }
        int action = drPage.actionAt(event.x(), event.y(), width, height);
        if (action >= 0) {
            drAction(action);
            return true;
        }
        int hit = drPage.mouseMoved(event.x(), event.y(), width, height);
        if (hit >= 0 && hit < accounts.size()) {
            if (sameAccount(accounts.get(hit), selected)) {
                drPlay(DrSound.Sfx.SELECT);
                loginSelected();
            } else {
                selected = accounts.get(hit);
                setStatus("Selected " + selected.getUsername(), false);
                drPlay(DrSound.Sfx.MOVE);
            }
        }
        return true;
    }

    /** DR 版式的列表数据源：账号名 + 类型，底栏换成本页操作 */
    private final class DrPageSource implements DrPageRenderer.Source {

        private List<DrPage.Action> cachedActions;

        @Override
        public List<DrPage.Row> rows() {
            List<DrPage.Row> list = new ArrayList<>(accounts.size());
            for (Alt alt : accounts) {
                list.add(new DrPage.Row(alt.getUsername(), "", accountType(alt)));
            }
            return list;
        }

        @Override
        public List<DrPage.Action> actions() {
            if (cachedActions == null) {
                cachedActions = drActions();
            }
            return cachedActions;
        }

        @Override
        public String title() {
            return DrThemeState.isChinese ? "账号管理" : "Accounts";
        }

        @Override
        public int selected() {
            return drSelectedIndex();
        }

        @Override
        public int version() {
            return accounts.size() * 31 + (selected == null ? 0 : selected.getUsername().hashCode());
        }
    }

    private void drawTopBar(Canvas canvas, Layout layout) {
        float titleX = layout.topBar.x() + 14.0F;
        UiControls.brand(canvas, "ALT MANAGER", titleX, layout.topBar.y() + 8.0F, 20.0F,
                UiControls.TEXT, 13.0F, 1.4F);
        float titleWidth = UiControls.brandWidth("ALT MANAGER", 13.0F, 1.4F);
        SkijaUi.text(canvas, accounts.size() + " accounts", titleX + titleWidth + 12.0F,
                layout.topBar.y() + 12.0F, 12.0F, UiControls.TEXT_FAINT, 7.0F);
        UiControls.button(canvas, layout.back, "Back", layout.back.contains(mouseX, mouseY), true,
                UiControls.Tone.NORMAL);
    }

    private void drawGrid(Canvas canvas, Layout layout) {
        UiControls.Box grid = layout.grid;
        SkijaUi.rounded(canvas, grid.x(), grid.y(), grid.width(), grid.height(),
                UiControls.RADIUS_SMALL, 0x33000000);

        if (accounts.isEmpty()) {
            UiControls.centeredText(canvas, "No saved accounts",
                    new UiControls.Box(grid.x() + 8, grid.y(), grid.width() - 16, grid.height()),
                    UiTheme.textFaint(), false);
            return;
        }

        Alt current = AltManager.INSTANCE.getLastAlt().orElse(null);
        int accent = UiTheme.accent();
        int visible = visibleCards(layout);
        for (int slot = 0; slot < visible && firstVisible + slot < accounts.size(); slot++) {
            Alt alt = accounts.get(firstVisible + slot);
            UiControls.Box card = cardBox(layout, slot);
            boolean isSelected = sameAccount(alt, selected);
            boolean isCurrent = sameAccount(alt, current)
                    || alt.getUsername().equalsIgnoreCase(AltManager.INSTANCE.getCurrentUsername());
            boolean hovered = card.contains(mouseX, mouseY);
            UiControls.card(canvas, card, hovered, isSelected);

            float discSize = 26.0F;
            float discX = card.x() + 8.0F;
            float discY = card.y() + (card.height() - discSize) * 0.5F;
            int discFill;
            int discText;
            if (isSelected) {
                discFill = accent;
                discText = UiControls.onAccent(accent);
            } else if (isCurrent) {
                discFill = UiTheme.withAlpha(accent, 150);
                discText = UiControls.TEXT;
            } else {
                discFill = UiControls.CARD_HOVER;
                discText = UiControls.TEXT_MUTED;
            }
            UiControls.disc(canvas, discX, discY, discSize, discFill);
            UiControls.centeredText(canvas, firstLetter(alt.getUsername()),
                    new UiControls.Box(discX, discY, discSize, discSize), discText, true);

            float textX = discX + discSize + 10.0F;
            float textWidth = card.x() + card.width() - textX - 10.0F;
            SkijaUi.boldText(canvas, UiControls.ellipsize(alt.getUsername(), textWidth, true), textX,
                    card.y() + 9, 15, UiControls.TEXT);
            SkijaUi.text(canvas, UiControls.ellipsize(accountType(alt), textWidth), textX, card.y() + 25,
                    12, isCurrent ? accent : UiControls.TEXT_FAINT);

            if (isCurrent) {
                UiControls.disc(canvas, card.x() + card.width() - 13.0F, card.y() + 8.0F, 5.0F, accent);
            }
        }
    }

    private void drawActionBar(Canvas canvas, Layout layout) {
        SkijaUi.boldText(canvas, "ADD OFFLINE", layout.offlineName.x(), layout.offlineName.y() - 13,
                11, UiControls.TEXT_FAINT, 7);
        offlineName.draw(canvas, mouseX, mouseY);
        UiControls.button(canvas, layout.add, "Add", layout.add.contains(mouseX, mouseY), !loginRunning,
                UiControls.Tone.NORMAL);

        SkijaUi.boldText(canvas, "DIRECT TOKEN", layout.minecraftToken.x(), layout.minecraftToken.y() - 13,
                11, UiControls.TEXT_FAINT, 7);
        minecraftToken.draw(canvas, mouseX, mouseY);
        UiControls.button(canvas, layout.tokenLogin, "Use", layout.tokenLogin.contains(mouseX, mouseY),
                !loginRunning, UiControls.Tone.NORMAL);

        UiControls.button(canvas, layout.microsoft,
                loginRunning ? "Microsoft login in progress..." : "Microsoft device login",
                layout.microsoft.contains(mouseX, mouseY), !loginRunning, UiControls.Tone.PRIMARY);
        UiControls.button(canvas, layout.login, "Use selected", layout.login.contains(mouseX, mouseY),
                selected != null && !loginRunning, UiControls.Tone.NORMAL);
        UiControls.button(canvas, layout.remove, "Remove", layout.remove.contains(mouseX, mouseY),
                selected != null && !loginRunning, UiControls.Tone.DANGER);
    }

    private void drawStatusBar(Canvas canvas, Layout layout) {
        SkijaUi.fill(canvas, layout.statusBar.x(), layout.statusBar.y(), layout.statusBar.width(),
                layout.statusBar.height(), UiTheme.argb(150, 8, 12, 14));
        int color = statusError ? UiTheme.danger() : loginRunning ? UiTheme.info() : UiTheme.textMuted();
        SkijaUi.rounded(canvas, layout.statusBar.x() + 10, layout.statusBar.y() + 9, 5, 5, 2.5F, color);
        SkijaUi.text(canvas, UiControls.ellipsize(status, layout.statusBar.width() - 80),
                layout.statusBar.x() + 21, layout.statusBar.y() + 4, layout.statusBar.height() - 4,
                color, 8);
        String mode = loginRunning ? "WORKING" : statusError ? "FAILED" : "READY";
        float modeWidth = SkijaUi.boldTextWidth(mode, 7);
        SkijaUi.boldText(canvas, mode, layout.statusBar.x() + layout.statusBar.width() - modeWidth - 10,
                layout.statusBar.y() + 4, layout.statusBar.height() - 4, color, 7);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (DrTheme.active()) {
            return drMouseClicked(event, doubleClick);
        }
        Layout layout = layout();
        updateInputBounds(layout);
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean nameHit = offlineName.click(event.x(), event.y(), doubleClick);
            boolean tokenHit = minecraftToken.click(event.x(), event.y(), doubleClick);
            if (nameHit || tokenHit) {
                return true;
            }
        }

        Alt clicked = accountAt(layout, event.x(), event.y());
        if (clicked != null) {
            selected = clicked;
            setStatus("Selected " + clicked.getUsername(), false);
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                removeSelected();
            } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && doubleClick) {
                loginSelected();
            }
            return true;
        }

        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (layout.back.contains(event.x(), event.y())) {
            onClose();
        } else if (layout.add.contains(event.x(), event.y()) && !loginRunning) {
            addOffline();
        } else if (layout.microsoft.contains(event.x(), event.y()) && !loginRunning) {
            startMicrosoftLogin();
        } else if (layout.tokenLogin.contains(event.x(), event.y()) && !loginRunning) {
            startTokenLogin();
        } else if (layout.login.contains(event.x(), event.y()) && selected != null && !loginRunning) {
            loginSelected();
        } else if (layout.remove.contains(event.x(), event.y()) && selected != null && !loginRunning) {
            removeSelected();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (DrTheme.active()) {
            drPage.mouseScrolled(scrollY);
            return true;
        }
        Layout layout = layout();
        if (!layout.grid.contains(mouseX, mouseY)) {
            return false;
        }
        int step = scrollY > 0 ? -layout.columns : scrollY < 0 ? layout.columns : 0;
        firstVisible += step;
        clampFirstVisible(layout);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DrTheme.active()) {
            return drKeyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_TAB) {
            if (offlineName.isFocused()) {
                offlineName.blur();
                minecraftToken.focus();
            } else {
                minecraftToken.blur();
                offlineName.focus();
            }
            return true;
        }
        if (offlineName.isFocused() && event.isConfirmation()) {
            addOffline();
            return true;
        }
        if (minecraftToken.isFocused() && event.isConfirmation()) {
            startTokenLogin();
            return true;
        }
        if (offlineName.keyPressed(event, minecraft) || minecraftToken.keyPressed(event, minecraft)) {
            return true;
        }
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (DrTheme.active()) {
            return offlineName.charTyped(event) || super.charTyped(event);
        }
        return offlineName.charTyped(event) || minecraftToken.charTyped(event) || super.charTyped(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (DrTheme.active()) {
            drPage.mouseMoved(mouseX, mouseY, width, height);
        }
    }

    private boolean drKeyPressed(KeyEvent event) {
        if (offlineName.isFocused()) {
            if (event.isEscape()) {
                offlineName.blur();
                return true;
            }
            if (event.isConfirmation()) {
                addOffline();
                return true;
            }
            if (offlineName.keyPressed(event, minecraft)) {
                return true;
            }
        }
        if (event.isEscape()) {
            drPlay(DrSound.Sfx.BACK);
            onClose();
            return true;
        }
        int confirm = drPage.confirmKey(event.key());
        if (confirm != -2) {
            if (confirm >= 0 && confirm < accounts.size()) {
                if (sameAccount(accounts.get(confirm), selected)) {
                    drPlay(DrSound.Sfx.SELECT);
                    loginSelected();
                } else {
                    selected = accounts.get(confirm);
                    setStatus("Selected " + selected.getUsername(), false);
                    drPlay(DrSound.Sfx.MOVE);
                }
            } else {
                drAction(drPage.cursorActionIndex());
            }
            return true;
        }
        drPage.keyPressed(event.key());
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void addOffline() {
        if (loginRunning) {
            return;
        }
        String name = offlineName.text().trim();
        if (!AltManager.INSTANCE.addOfflineAlt(name)) {
            setStatus("Invalid or duplicate offline username", true);
            return;
        }
        offlineName.clear();
        refreshAccounts();
        selected = accounts.stream().filter(alt -> alt.getUsername().equalsIgnoreCase(name)).findFirst().orElse(selected);
        setStatus("Added " + name, false);
    }

    private void startMicrosoftLogin() {
        loginRunning = true;
        setStatus("Requesting a Microsoft device code...", false);
        AltManager.INSTANCE.startMicrosoftDeviceLogin(new LoginCallback("Microsoft login"), false);
    }

    private void startTokenLogin() {
        if (loginRunning) {
            return;
        }
        String token = minecraftToken.text().trim();
        if (token.isEmpty()) {
            setStatus("Minecraft access token is empty", true);
            return;
        }
        loginRunning = true;
        setStatus("Checking Minecraft access token...", false);
        AltManager.INSTANCE.loginWithMinecraftToken(token, new LoginCallback("Token login"), false);
    }

    private void loginSelected() {
        Alt alt = selected;
        if (alt == null || loginRunning) {
            return;
        }
        if (!alt.isMicrosoft()) {
            AltManager.INSTANCE.login(alt);
            setStatus("Logged in as " + alt.getUsername(), false);
            return;
        }

        loginRunning = true;
        setStatus("Refreshing " + alt.getUsername() + "...", false);
        AltManager.INSTANCE.loginWithRefresh(alt, new LoginCallback("Logged in"), false);
    }

    private void removeSelected() {
        Alt removed = selected;
        if (removed == null || loginRunning) {
            return;
        }
        AltManager.INSTANCE.removeAlt(removed);
        selected = null;
        refreshAccounts();
        setStatus("Removed " + removed.getUsername(), false);
    }

    private void refreshAccounts() {
        String selectedName = selected == null ? null : selected.getUsername();
        accounts = AltManager.INSTANCE.getAlts();
        selected = selectedName == null
                ? AltManager.INSTANCE.getLastAlt().orElse(accounts.isEmpty() ? null : accounts.getFirst())
                : accounts.stream().filter(alt -> alt.getUsername().equalsIgnoreCase(selectedName)).findFirst()
                .orElse(accounts.isEmpty() ? null : accounts.getFirst());
    }

    private Alt accountAt(Layout layout, double mouseX, double mouseY) {
        if (!layout.grid.contains(mouseX, mouseY)) {
            return null;
        }
        int visible = visibleCards(layout);
        for (int slot = 0; slot < visible && firstVisible + slot < accounts.size(); slot++) {
            if (cardBox(layout, slot).contains(mouseX, mouseY)) {
                return accounts.get(firstVisible + slot);
            }
        }
        return null;
    }

    private int visibleRows(Layout layout) {
        return Math.max(1, (int) ((layout.grid.height() + CARD_GAP) / (CARD_HEIGHT + CARD_GAP)));
    }

    private int visibleCards(Layout layout) {
        return visibleRows(layout) * layout.columns;
    }

    private UiControls.Box cardBox(Layout layout, int slot) {
        int row = slot / layout.columns;
        int column = slot % layout.columns;
        float cardWidth = (layout.grid.width() - CARD_GAP * (layout.columns + 1)) / layout.columns;
        float cardX = layout.grid.x() + CARD_GAP + column * (cardWidth + CARD_GAP);
        float cardY = layout.grid.y() + CARD_GAP + row * (CARD_HEIGHT + CARD_GAP);
        return new UiControls.Box(cardX, cardY, cardWidth, CARD_HEIGHT);
    }

    private void clampFirstVisible(Layout layout) {
        int rows = Math.max(1, (int) Math.ceil(accounts.size() / (double) layout.columns));
        int maxRows = Math.max(0, rows - visibleRows(layout));
        int firstRow = Math.max(0, Math.min(firstVisible / layout.columns, maxRows));
        firstVisible = firstRow * layout.columns;
    }

    private void setStatus(String status, boolean error) {
        this.status = status == null ? "" : status;
        this.statusError = error;
    }

    private void updateInputBounds(Layout layout) {
        offlineName.setBounds(layout.offlineName);
        minecraftToken.setBounds(layout.minecraftToken);
    }

    private Layout layout() {
        float workspaceWidth = Math.min(720.0F, Math.max(320.0F, width - 24.0F));
        float workspaceHeight = Math.min(500.0F, Math.max(280.0F, height - 24.0F));
        workspaceWidth = Math.min(workspaceWidth, Math.max(0.0F, width - 8.0F));
        workspaceHeight = Math.min(workspaceHeight, Math.max(0.0F, height - 8.0F));
        float x = Math.max(4.0F, (width - workspaceWidth) * 0.5F);
        float y = Math.max(4.0F, (height - workspaceHeight) * 0.5F);
        UiControls.Box workspace = new UiControls.Box(x, y, workspaceWidth, workspaceHeight);
        UiControls.Box topBar = new UiControls.Box(x, y, workspaceWidth, 34.0F);
        UiControls.Box statusBar = new UiControls.Box(x, y + workspaceHeight - 21.0F,
                workspaceWidth, 21.0F);
        UiControls.Box back = new UiControls.Box(x + workspaceWidth - 64.0F, y + 6.0F, 54.0F, 22.0F);

        float padding = 14.0F;
        float contentX = x + padding;
        float contentWidth = workspaceWidth - padding * 2.0F;
        int columns = workspaceWidth >= 560 ? 3 : workspaceWidth >= 400 ? 2 : 1;

        // Bottom action bar: two input rows plus one button row, stacked upward
        // from the status bar.
        float rowHeight = 24.0F;
        float rowGap = 8.0F;
        float labelGap = 15.0F;
        float actionBarHeight = labelGap + rowHeight            // add offline
                + rowGap + labelGap + rowHeight                 // direct token
                + rowGap + rowHeight                            // microsoft
                + rowGap + rowHeight;                           // use selected / remove
        float actionBarTop = statusBar.y() - 8.0F - actionBarHeight;

        float gridTop = y + 42.0F;
        UiControls.Box grid = new UiControls.Box(x, gridTop, workspaceWidth,
                Math.max(CARD_HEIGHT + CARD_GAP * 2, actionBarTop - gridTop - 8.0F));

        float buttonWidth = Math.min(70.0F, Math.max(44.0F, contentWidth * 0.16F));
        float fieldWidth = contentWidth - buttonWidth - 8.0F;

        float nameY = actionBarTop + labelGap;
        UiControls.Box name = new UiControls.Box(contentX, nameY, fieldWidth, rowHeight);
        UiControls.Box add = new UiControls.Box(contentX + fieldWidth + 8.0F, nameY, buttonWidth, rowHeight);

        float tokenY = nameY + rowHeight + rowGap + labelGap;
        UiControls.Box token = new UiControls.Box(contentX, tokenY, fieldWidth, rowHeight);
        UiControls.Box tokenLogin = new UiControls.Box(contentX + fieldWidth + 8.0F, tokenY, buttonWidth, rowHeight);

        float actionsY = tokenY + rowHeight + rowGap;
        UiControls.Box microsoft = new UiControls.Box(contentX, actionsY, contentWidth, rowHeight);

        float useRemoveY = actionsY + rowHeight + rowGap;
        float half = (contentWidth - 8.0F) * 0.5F;
        UiControls.Box login = new UiControls.Box(contentX, useRemoveY, half, rowHeight);
        UiControls.Box remove = new UiControls.Box(contentX + half + 8.0F, useRemoveY, half, rowHeight);

        return new Layout(workspace, topBar, statusBar, back, grid, name, add, token, tokenLogin,
                microsoft, login, remove, columns);
    }

    private static boolean sameAccount(Alt first, Alt second) {
        return first != null && second != null && first.getUsername().equalsIgnoreCase(second.getUsername());
    }

    private static String firstLetter(String name) {
        return name == null || name.isBlank() ? "?" : name.substring(0, 1).toUpperCase();
    }

    private static String accountType(Alt alt) {
        if (!alt.isMicrosoft()) {
            return "Offline account";
        }
        if (alt.isExpired()) {
            return alt.canRefresh() ? "Microsoft - refresh required" : "Minecraft token expired";
        }
        long hours = Duration.ofSeconds(alt.getLeftExpiringTime()).toHours();
        return (alt.canRefresh() ? "Microsoft" : "Minecraft token") + " - " + hours + "h left";
    }

    private record Layout(
            UiControls.Box workspace,
            UiControls.Box topBar,
            UiControls.Box statusBar,
            UiControls.Box back,
            UiControls.Box grid,
            UiControls.Box offlineName,
            UiControls.Box add,
            UiControls.Box minecraftToken,
            UiControls.Box tokenLogin,
            UiControls.Box microsoft,
            UiControls.Box login,
            UiControls.Box remove,
            int columns
    ) {
    }

    private final class LoginCallback implements MicrosoftAuthService.LoginCallback {

        private final String successPrefix;

        private LoginCallback(String successPrefix) {
            this.successPrefix = successPrefix;
        }

        @Override
        public void setStatus(String message) {
            minecraft.execute(() -> AltManagerScreen.this.setStatus(message, false));
        }

        @Override
        public void onSucceed(Alt alt) {
            minecraft.execute(() -> {
                AltManager.INSTANCE.login(alt);
                loginRunning = false;
                refreshAccounts();
                selected = accounts.stream()
                        .filter(account -> account.getUsername().equalsIgnoreCase(alt.getUsername()))
                        .findFirst()
                        .orElse(alt);
                AltManagerScreen.this.setStatus(successPrefix + " as " + alt.getUsername(), false);
            });
        }

        @Override
        public void onFailed(Exception error) {
            minecraft.execute(() -> {
                loginRunning = false;
                String message = error == null ? "Unknown error" : error.getMessage();
                AltManagerScreen.this.setStatus("Microsoft login failed: " + message, true);
            });
        }
    }
}
