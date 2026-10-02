package com.dioxidelite.ui.theme;

import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.ui.UiTheme;
import com.dioxidelite.ui.clickgui.GuiPalette;
import com.google.gson.JsonPrimitive;
import io.github.humbleui.skija.Canvas;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI 内的主题面板：卡片列表 + 就地编辑器。
 *
 * <p>Pop 与 Window 两个模式共用这一个视图，宿主只负责给它一块矩形区域（内容坐标系）
 * 和转发鼠标/键盘。列表左键选主题、右键自定义主题进入编辑、内置主题右键复制为自定义；
 * 编辑器里的改动即时保存并同步运行时。</p>
 */
public final class ThemePanelView {

    // ---- 列表布局 ----
    private static final float CARD_HEIGHT = 42.0F;
    private static final float CARD_GAP = 6.0F;
    private static final float FOOTER_HEIGHT = 28.0F;
    private static final float PAD = 8.0F;

    // ---- 编辑器布局 ----
    private static final float TOP_BAR_HEIGHT = 30.0F;
    private static final float PREVIEW_HEIGHT = 76.0F;
    private static final float GROUP_HEADER_HEIGHT = 20.0F;
    private static final float SLOT_ROW_HEIGHT = 20.0F;
    private static final float CHANNEL_HEIGHT = 15.0F;
    private static final float GRADIENT_ROW_HEIGHT = 46.0F;
    private static final float ADDON_ROW_HEIGHT = 20.0F;

    private static final String[] CHANNEL_LABELS = {"R", "G", "B", "A"};

    private enum Page { LIST, EDITOR }

    private enum RowType {
        LIST_ADD, LIST_CARD, LIST_FOOTER,
        TOP_BAR, PREVIEW, GROUP_HEADER, SLOT, GRADIENT, ADDON_INTRO, ADDON_UI, SPACER
    }

    private static final class Row {
        final RowType type;
        final float y;
        final float height;
        final Object payloadA;
        final Object payloadB;

        Row(RowType type, float y, float height, Object payloadA, Object payloadB) {
            this.type = type;
            this.y = y;
            this.height = height;
            this.payloadA = payloadA;
            this.payloadB = payloadB;
        }
    }

    private Page page = Page.LIST;
    private EditableTheme editing;

    private Slot openSlot;
    private GradientSpec.Kind openGradient;
    private boolean openGradientStart;
    private int dragChannel = -1;
    private float dragCursorX;
    private float dragTrackX;
    private float dragTrackWidth;

    private boolean focusName;
    private boolean focusHex;
    private String textBuffer = "";
    private int textCursor;
    private boolean selectAll;

    private String pendingDelete;
    private String toast;
    private long toastUntilMs;

    private float listScroll;
    private float editorScroll;

    public ThemePanelView() {
    }

    // ------------------------------------------------------------------
    // 宿主接口
    // ------------------------------------------------------------------

    public void openList() {
        page = Page.LIST;
        editing = null;
        openSlot = null;
        openGradient = null;
        focusName = false;
        focusHex = false;
    }

    public void openEditor(ThemeEntry entry) {
        if (entry == null) {
            return;
        }
        page = Page.EDITOR;
        editing = EditableTheme.from(entry);
        openSlot = null;
        openGradient = null;
        focusName = false;
        focusHex = false;
        editorScroll = 0.0F;
        textBuffer = "";
    }

    public float contentHeight(float width) {
        return contentHeight(buildRows(width));
    }

    private static float contentHeight(List<Row> rows) {
        if (rows.isEmpty()) {
            return 0.0F;
        }
        Row last = rows.get(rows.size() - 1);
        return last.y + last.height + PAD;
    }

    public void render(Canvas canvas, float x, float top, float width, float mouseX, float contentMouseY) {
        List<Row> rows = buildRows(width);
        float contentHeight = contentHeight(rows);
        for (Row row : rows) {
            float rowY = top + row.y;
            switch (row.type) {
                case LIST_ADD -> drawAddCard(canvas, x, rowY, width, mouseX, contentMouseY);
                case LIST_CARD -> drawThemeCard(canvas, x, rowY, width, mouseX, contentMouseY,
                        (ThemeEntry) row.payloadA);
                case LIST_FOOTER -> drawFooter(canvas, x, rowY, width, mouseX, contentMouseY);
                case TOP_BAR -> drawTopBar(canvas, x, rowY, width, mouseX, contentMouseY);
                case PREVIEW -> drawPreview(canvas, x, rowY, width);
                case GROUP_HEADER -> drawGroupHeader(canvas, x, rowY, width, row.payloadA);
                case SLOT -> drawSlotRow(canvas, x, rowY, width, mouseX, contentMouseY,
                        (Slot) row.payloadA, (Boolean) row.payloadB);
                case GRADIENT -> drawGradientRow(canvas, x, rowY, width, mouseX, contentMouseY,
                        (GradientSpec.Kind) row.payloadA);
                case ADDON_INTRO -> drawIntroAddon(canvas, x, rowY, width, mouseX, contentMouseY);
                case ADDON_UI -> drawUiAddon(canvas, x, rowY, width, mouseX, contentMouseY);
                default -> {
                }
            }
        }
        drawToast(canvas, x, top + contentHeight, width);
    }

    // ------------------------------------------------------------------
    // 布局
    // ------------------------------------------------------------------

    private List<Row> buildRows(float width) {
        return page == Page.LIST ? buildListRows(width) : buildEditorRows(width);
    }

    private List<Row> buildListRows(float width) {
        List<Row> rows = new ArrayList<>();
        float y = PAD;
        rows.add(new Row(RowType.LIST_ADD, y, FOOTER_HEIGHT, null, null));
        y += FOOTER_HEIGHT + CARD_GAP;
        for (ThemeEntry entry : Themes.themes()) {
            rows.add(new Row(RowType.LIST_CARD, y, CARD_HEIGHT, entry, null));
            y += CARD_HEIGHT + CARD_GAP;
        }
        y += 4.0F;
        rows.add(new Row(RowType.LIST_FOOTER, y, FOOTER_HEIGHT, null, null));
        return rows;
    }

    private List<Row> buildEditorRows(float width) {
        List<Row> rows = new ArrayList<>();
        if (editing == null) {
            return rows;
        }
        float y = PAD;
        rows.add(new Row(RowType.TOP_BAR, y, TOP_BAR_HEIGHT, null, null));
        y += TOP_BAR_HEIGHT + 6.0F;
        rows.add(new Row(RowType.PREVIEW, y, PREVIEW_HEIGHT, null, null));
        y += PREVIEW_HEIGHT + 8.0F;

        for (Slot.Group group : Slot.Group.values()) {
            rows.add(new Row(RowType.GROUP_HEADER, y, GROUP_HEADER_HEIGHT, group, null));
            y += GROUP_HEADER_HEIGHT;
            for (Slot slot : Slot.of(group)) {
                float height = SLOT_ROW_HEIGHT;
                if (slot == openSlot) {
                    height = SLOT_ROW_HEIGHT + 4.0F * CHANNEL_HEIGHT + 10.0F;
                }
                rows.add(new Row(RowType.SLOT, y, height, slot, slot == openSlot));
                y += height;
            }
            y += 4.0F;
        }

        rows.add(new Row(RowType.GROUP_HEADER, y, GROUP_HEADER_HEIGHT, "Gradients", null));
        y += GROUP_HEADER_HEIGHT;
        for (GradientSpec.Kind kind : GradientSpec.Kind.values()) {
            float height = GRADIENT_ROW_HEIGHT;
            if (kind == openGradient) {
                height += 4.0F * CHANNEL_HEIGHT + 12.0F;
            }
            rows.add(new Row(RowType.GRADIENT, y, height, kind, null));
            y += height;
        }
        y += 6.0F;

        rows.add(new Row(RowType.GROUP_HEADER, y, GROUP_HEADER_HEIGHT, "Startup Intro", null));
        y += GROUP_HEADER_HEIGHT;
        rows.add(new Row(RowType.ADDON_INTRO, y, ADDON_ROW_HEIGHT * 5.0F + 8.0F, null, null));
        y += ADDON_ROW_HEIGHT * 5.0F + 14.0F;

        rows.add(new Row(RowType.GROUP_HEADER, y, GROUP_HEADER_HEIGHT, "Custom UI", null));
        y += GROUP_HEADER_HEIGHT;
        rows.add(new Row(RowType.ADDON_UI, y, ADDON_ROW_HEIGHT * 4.0F + 8.0F, null, null));
        y += ADDON_ROW_HEIGHT * 4.0F + 8.0F;
        return rows;
    }

    // ------------------------------------------------------------------
    // 列表页绘制
    // ------------------------------------------------------------------

    private void drawAddCard(Canvas canvas, float x, float y, float width, float mouseX, float mouseY) {
        boolean hovered = inside(mouseX, mouseY, x + PAD, y, width - PAD * 2.0F, FOOTER_HEIGHT);
        int fill = hovered ? GuiPalette.section() : GuiPalette.panelInner();
        SkijaUi.rounded(canvas, x + PAD, y, width - PAD * 2.0F, FOOTER_HEIGHT, GuiPalette.CHIP_RADIUS, fill);
        SkijaUi.outline(canvas, x + PAD, y, width - PAD * 2.0F, FOOTER_HEIGHT, GuiPalette.CHIP_RADIUS,
                1.0F, GuiPalette.outlineVariant());
        String label = "+  New from current";
        float textWidth = SkijaUi.textWidth(label, 7.0F);
        SkijaUi.text(canvas, label, x + width * 0.5F - textWidth * 0.5F, y, FOOTER_HEIGHT,
                hovered ? GuiPalette.primary() : GuiPalette.textDim(), 7.0F);
    }

    private void drawThemeCard(Canvas canvas, float x, float y, float width, float mouseX, float mouseY,
                               ThemeEntry entry) {
        float cardX = x + PAD;
        float cardWidth = width - PAD * 2.0F;
        boolean hovered = inside(mouseX, mouseY, cardX, y, cardWidth, CARD_HEIGHT);
        boolean selected = entry.id().equals(Themes.currentId());
        int fill = selected ? GuiPalette.active() : hovered ? GuiPalette.section() : GuiPalette.panelInner();
        SkijaUi.rounded(canvas, cardX, y, cardWidth, CARD_HEIGHT, GuiPalette.CARD_RADIUS, fill);
        SkijaUi.outline(canvas, cardX, y, cardWidth, CARD_HEIGHT, GuiPalette.CARD_RADIUS, 1.0F,
                selected ? GuiPalette.primary() : GuiPalette.outlineVariant());

        SkijaUi.boldText(canvas, fit(entry.name(), cardWidth - 96.0F, 7.6F), cardX + 9.0F, y,
                CARD_HEIGHT * 0.55F, selected ? GuiPalette.text() : GuiPalette.textDim(), 7.6F);
        String tag = entry.builtin() ? "built-in" : "custom";
        SkijaUi.text(canvas, tag, cardX + 9.0F, y + CARD_HEIGHT * 0.5F, CARD_HEIGHT * 0.4F,
                GuiPalette.textFaint(), 6.0F);

        // 色板 swatch：accent / window / panel / text / card（自定义卡片给删除按钮让位）
        float swatchX = cardX + cardWidth - (entry.builtin() ? 74.0F : 94.0F);
        float swatchY = y + CARD_HEIGHT * 0.5F - 6.0F;
        for (Slot slot : new Slot[]{Slot.ACCENT, Slot.WINDOW_BG, Slot.PANEL, Slot.TEXT_PRIMARY, Slot.CARD_ENABLED}) {
            SkijaUi.rounded(canvas, swatchX, swatchY, 11.0F, 12.0F, 3.0F, pick(entry, slot));
            swatchX += 14.0F;
        }

        if (!entry.builtin()) {
            boolean deleting = entry.id().equals(pendingDelete);
            float deleteX = x + width - PAD - 16.0F;
            SkijaUi.rounded(canvas, deleteX, y + CARD_HEIGHT * 0.5F - 6.0F, 12.0F, 12.0F, 3.0F,
                    deleting ? UiTheme.danger() : GuiPalette.hover());
            SkijaUi.boldText(canvas, "x", deleteX + 3.5F, y + CARD_HEIGHT * 0.5F - 6.0F, 12.0F,
                    deleting ? GuiPalette.text() : GuiPalette.textDim(), 6.5F);
        }
    }

    private void drawFooter(Canvas canvas, float x, float y, float width, float mouseX, float mouseY) {
        float buttonWidth = (width - PAD * 2.0F - 6.0F) * 0.5F;
        drawSmallButton(canvas, "Import", x + PAD, y, buttonWidth, FOOTER_HEIGHT - 4.0F,
                inside(mouseX, mouseY, x + PAD, y, buttonWidth, FOOTER_HEIGHT - 4.0F));
        drawSmallButton(canvas, "Export", x + PAD + buttonWidth + 6.0F, y, buttonWidth, FOOTER_HEIGHT - 4.0F,
                inside(mouseX, mouseY, x + PAD + buttonWidth + 6.0F, y, buttonWidth, FOOTER_HEIGHT - 4.0F));
    }

    private void drawSmallButton(Canvas canvas, String label, float x, float y, float width, float height,
                                 boolean hovered) {
        SkijaUi.rounded(canvas, x, y, width, height, GuiPalette.CHIP_RADIUS,
                hovered ? GuiPalette.section() : GuiPalette.panelInner());
        SkijaUi.outline(canvas, x, y, width, height, GuiPalette.CHIP_RADIUS, 1.0F,
                hovered ? GuiPalette.outline() : GuiPalette.outlineVariant());
        float textWidth = SkijaUi.textWidth(label, 6.8F);
        SkijaUi.text(canvas, label, x + width * 0.5F - textWidth * 0.5F, y, height,
                hovered ? GuiPalette.text() : GuiPalette.textDim(), 6.8F);
    }

    // ------------------------------------------------------------------
    // 编辑页绘制
    // ------------------------------------------------------------------

    private void drawTopBar(Canvas canvas, float x, float y, float width, float mouseX, float mouseY) {
        float backWidth = 24.0F;
        boolean backHover = inside(mouseX, mouseY, x + PAD, y, backWidth, TOP_BAR_HEIGHT);
        SkijaUi.rounded(canvas, x + PAD, y, backWidth, TOP_BAR_HEIGHT, GuiPalette.CHIP_RADIUS,
                backHover ? GuiPalette.section() : GuiPalette.panelInner());
        SkijaUi.boldText(canvas, "<", x + PAD + 8.0F, y, TOP_BAR_HEIGHT,
                backHover ? GuiPalette.primary() : GuiPalette.textDim(), 9.0F);

        float exportWidth = 52.0F;
        float exportX = x + width - PAD - exportWidth;
        boolean exportHover = inside(mouseX, mouseY, exportX, y, exportWidth, TOP_BAR_HEIGHT);
        drawSmallButton(canvas, "Export", exportX, y, exportWidth, TOP_BAR_HEIGHT, exportHover);

        float nameX = x + PAD + backWidth + 6.0F;
        float nameWidth = Math.max(40.0F, exportX - nameX - 6.0F);
        SkijaUi.rounded(canvas, nameX, y, nameWidth, TOP_BAR_HEIGHT, GuiPalette.CHIP_RADIUS,
                GuiPalette.panelInner());
        SkijaUi.outline(canvas, nameX, y, nameWidth, TOP_BAR_HEIGHT, GuiPalette.CHIP_RADIUS, 1.0F,
                focusName ? GuiPalette.primary() : GuiPalette.outlineVariant());
        String shown = focusName ? textBuffer : editing.name();
        SkijaUi.boldText(canvas, fit(shown + (focusName && blink() ? "|" : ""), nameWidth - 14.0F, 7.4F),
                nameX + 7.0F, y, TOP_BAR_HEIGHT, GuiPalette.text(), 7.4F);
    }

    private void drawPreview(Canvas canvas, float x, float y, float width) {
        float boxX = x + PAD;
        float boxWidth = width - PAD * 2.0F;
        SkijaUi.rounded(canvas, boxX, y, boxWidth, PREVIEW_HEIGHT, GuiPalette.CARD_RADIUS, pick(Slot.WINDOW_BG));
        SkijaUi.outline(canvas, boxX, y, boxWidth, PREVIEW_HEIGHT, GuiPalette.CARD_RADIUS, 1.0F, pick(Slot.OUTLINE));

        float headerHeight = 18.0F;
        int headerColor = pick(Slot.HEADER);
        Integer headerEnd = gradientEndForPreview(GradientSpec.Kind.HEADER);
        if (headerEnd != null) {
            SkijaUi.gradient(canvas, boxX, y, boxWidth, headerHeight, headerColor, headerEnd,
                    true, GuiPalette.CARD_RADIUS);
        } else {
            SkijaUi.rounded(canvas, boxX, y, boxWidth, headerHeight, GuiPalette.CARD_RADIUS, headerColor);
        }
        SkijaUi.rounded(canvas, boxX, y + headerHeight - 6.0F, boxWidth, 6.0F, GuiPalette.CARD_RADIUS, headerColor);
        SkijaUi.boldText(canvas, "Preview", boxX + 8.0F, y, headerHeight, pick(Slot.TEXT_PRIMARY), 7.0F);
        SkijaUi.rounded(canvas, boxX + boxWidth - 18.0F, y + 5.0F, 9.0F, 9.0F, 4.5F, pick(Slot.ACCENT));

        float rowY = y + headerHeight + 6.0F;
        float rowWidth = boxWidth - 16.0F;
        SkijaUi.rounded(canvas, boxX + 8.0F, rowY, rowWidth, 16.0F, GuiPalette.CHIP_RADIUS,
                pick(Slot.CARD_ENABLED));
        SkijaUi.text(canvas, "Enabled module", boxX + 13.0F, rowY, 16.0F, pick(Slot.TEXT_PRIMARY), 6.4F);
        SkijaUi.rounded(canvas, boxX + 8.0F, rowY + 20.0F, rowWidth, 16.0F, GuiPalette.CHIP_RADIUS,
                pick(Slot.CARD_DISABLED));
        SkijaUi.text(canvas, "Disabled module", boxX + 13.0F, rowY + 20.0F, 16.0F, pick(Slot.TEXT_MUTED), 6.4F);
    }

    private Integer gradientEndForPreview(GradientSpec.Kind kind) {
        GradientSpec spec = editing.gradient(kind);
        return spec.enabled() ? spec.end() : null;
    }

    private void drawGroupHeader(Canvas canvas, float x, float y, float width, Object payload) {
        String title = payload instanceof Slot.Group group ? group.title() : String.valueOf(payload);
        SkijaUi.boldText(canvas, title, x + PAD, y, GROUP_HEADER_HEIGHT, GuiPalette.primary(), 7.2F);
        float titleWidth = SkijaUi.boldTextWidth(title, 7.2F);
        SkijaUi.fill(canvas, x + PAD + titleWidth + 6.0F, y + GROUP_HEADER_HEIGHT * 0.5F,
                Math.max(0.0F, width - PAD * 2.0F - titleWidth - 6.0F), 1.0F, GuiPalette.outlineVariant());
    }

    private void drawSlotRow(Canvas canvas, float x, float y, float width, float mouseX, float mouseY,
                             Slot slot, boolean open) {
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        boolean hovered = inside(mouseX, mouseY, rowX, y, rowWidth, SLOT_ROW_HEIGHT);
        boolean explicit = editing.color(slot) != null;
        SkijaUi.rounded(canvas, rowX, y, rowWidth, SLOT_ROW_HEIGHT, GuiPalette.CHIP_RADIUS,
                hovered || open ? GuiPalette.section() : GuiPalette.panelInner());
        SkijaUi.text(canvas, slot.key(), rowX + 7.0F, y, SLOT_ROW_HEIGHT,
                explicit ? GuiPalette.text() : GuiPalette.textDim(), 6.6F);

        int color = pick(slot);
        String hex = String.format("#%08X", color);
        float hexWidth = SkijaUi.textWidth(hex, 6.0F);
        SkijaUi.text(canvas, hex, rowX + rowWidth - 44.0F - hexWidth, y, SLOT_ROW_HEIGHT,
                GuiPalette.textFaint(), 6.0F);
        SkijaUi.rounded(canvas, rowX + rowWidth - 38.0F, y + 4.0F, 26.0F, 12.0F, 3.0F, color);
        SkijaUi.outline(canvas, rowX + rowWidth - 38.0F, y + 4.0F, 26.0F, 12.0F, 3.0F, 1.0F,
                explicit ? GuiPalette.primary() : GuiPalette.outlineVariant());

        if (!open) {
            return;
        }
        float channelY = y + SLOT_ROW_HEIGHT + 2.0F;
        float sliderX = rowX + 24.0F;
        float sliderWidth = rowWidth - 70.0F;
        SkijaUi.rounded(canvas, rowX + 4.0F, channelY, rowWidth - 8.0F,
                4.0F * CHANNEL_HEIGHT, GuiPalette.CHIP_RADIUS, GuiPalette.panel());
        for (int channel = 0; channel < 4; channel++) {
            float valueY = channelY + channel * CHANNEL_HEIGHT;
            SkijaUi.text(canvas, CHANNEL_LABELS[channel], rowX + 10.0F, valueY, CHANNEL_HEIGHT,
                    GuiPalette.textDim(), 6.0F);
            drawSlider(canvas, sliderX, valueY + 5.0F, sliderWidth, channelValue(color, channel) / 255.0F,
                    channelTint(color, channel));
        }
        boolean resetHover = inside(mouseX, mouseY, rowX + 10.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F,
                54.0F, 12.0F);
        SkijaUi.rounded(canvas, rowX + 10.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 54.0F, 12.0F, 3.0F,
                resetHover ? GuiPalette.section() : GuiPalette.panelInner());
        SkijaUi.text(canvas, "Reset", rowX + 19.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 12.0F,
                resetHover ? GuiPalette.primary() : GuiPalette.textDim(), 6.2F);
        drawHexField(canvas, rowX + 70.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 72.0F, 12.0F, focusHex);
    }

    private void drawHexField(Canvas canvas, float x, float y, float width, float height, boolean focused) {
        SkijaUi.rounded(canvas, x, y, width, height, 3.0F,
                focused ? GuiPalette.section() : GuiPalette.panelInner());
        SkijaUi.outline(canvas, x, y, width, height, 3.0F, 1.0F,
                focused ? GuiPalette.primary() : GuiPalette.outlineVariant());
        String shown = focused ? textBuffer + (blink() ? "|" : "") : "hex: #AARRGGBB";
        SkijaUi.text(canvas, shown, x + 4.0F, y, height,
                focused ? GuiPalette.text() : GuiPalette.textFaint(), 6.0F);
    }

    private void drawSlider(Canvas canvas, float x, float y, float width, float fraction, int fillColor) {
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        SkijaUi.rounded(canvas, x, y, width, 3.0F, 1.5F, GuiPalette.track());
        if (clamped > 0.001F) {
            SkijaUi.rounded(canvas, x, y, Math.max(3.0F, width * clamped), 3.0F, 1.5F, fillColor);
        }
        float handle = 8.0F;
        SkijaUi.rounded(canvas, x + width * clamped - handle * 0.5F, y + 1.5F - handle * 0.5F,
                handle, handle, handle * 0.5F, fillColor);
    }

    private void drawGradientRow(Canvas canvas, float x, float y, float width, float mouseX, float mouseY,
                                 GradientSpec.Kind kind) {
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        GradientSpec spec = editing.gradient(kind);
        boolean open = kind == openGradient;
        SkijaUi.rounded(canvas, rowX, y, rowWidth, GRADIENT_ROW_HEIGHT, GuiPalette.CHIP_RADIUS,
                open ? GuiPalette.section() : GuiPalette.panelInner());
        SkijaUi.text(canvas, kind.key(), rowX + 7.0F, y, 16.0F,
                spec.enabled() ? GuiPalette.text() : GuiPalette.textDim(), 6.6F);
        drawToggle(canvas, rowX + rowWidth - 28.0F, y + 4.0F, 20.0F, 10.0F, spec.enabled() ? 1.0F : 0.0F);

        float swatchY = y + 18.0F;
        SkijaUi.rounded(canvas, rowX + 7.0F, swatchY, 30.0F, 13.0F, 3.0F, spec.start());
        SkijaUi.outline(canvas, rowX + 7.0F, swatchY, 30.0F, 13.0F, 3.0F, 1.0F,
                open && openGradientStart ? GuiPalette.primary() : GuiPalette.outlineVariant());
        SkijaUi.rounded(canvas, rowX + 41.0F, swatchY, 30.0F, 13.0F, 3.0F, spec.end());
        SkijaUi.outline(canvas, rowX + 41.0F, swatchY, 30.0F, 13.0F, 3.0F, 1.0F,
                open && !openGradientStart ? GuiPalette.primary() : GuiPalette.outlineVariant());

        float angleX = rowX + 80.0F;
        float angleWidth = Math.max(20.0F, rowWidth - 96.0F);
        SkijaUi.text(canvas, Math.round(spec.angleDeg()) + "°", rowX + rowWidth - 44.0F, swatchY, 13.0F,
                GuiPalette.textFaint(), 6.0F);
        drawSlider(canvas, angleX, swatchY + 5.0F, angleWidth, spec.angleDeg() / 360.0F, GuiPalette.primary());

        if (!open) {
            return;
        }
        float channelY = y + GRADIENT_ROW_HEIGHT + 2.0F;
        int color = openGradientStart ? spec.start() : spec.end();
        float sliderX = rowX + 24.0F;
        float sliderWidth = rowWidth - 70.0F;
        for (int channel = 0; channel < 4; channel++) {
            float valueY = channelY + channel * CHANNEL_HEIGHT;
            SkijaUi.text(canvas, CHANNEL_LABELS[channel], rowX + 10.0F, valueY, CHANNEL_HEIGHT,
                    GuiPalette.textDim(), 6.0F);
            drawSlider(canvas, sliderX, valueY + 5.0F, sliderWidth, channelValue(color, channel) / 255.0F,
                    channelTint(color, channel));
        }
        drawHexField(canvas, rowX + 70.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 72.0F, 12.0F, focusHex);
    }

    private void drawIntroAddon(Canvas canvas, float x, float y, float width, float mouseX, float mouseY) {
        ThemeAddons.Intro intro = editing.intro();
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        SkijaUi.rounded(canvas, rowX, y, rowWidth, ADDON_ROW_HEIGHT * 5.0F + 2.0F,
                GuiPalette.CHIP_RADIUS, GuiPalette.panelInner());

        float rowY = y + 2.0F;
        drawAddonLabel(canvas, "Enabled", rowX, rowY);
        drawToggle(canvas, rowX + rowWidth - 28.0F, rowY + 4.0F, 20.0F, 10.0F, intro.enabled() ? 1.0F : 0.0F);
        rowY += ADDON_ROW_HEIGHT;

        drawAddonLabel(canvas, "Style", rowX, rowY);
        drawAddonValue(canvas, styleName(IntroStyles.byId(intro.styleId())), rowX, rowY, rowWidth);
        rowY += ADDON_ROW_HEIGHT;

        drawAddonLabel(canvas, "Chapter", rowX, rowY);
        drawAddonValue(canvas, "Ch." + intro.chapter(), rowX, rowY, rowWidth);
        rowY += ADDON_ROW_HEIGHT;

        drawAddonLabel(canvas, "Saved File", rowX, rowY);
        drawToggle(canvas, rowX + rowWidth - 28.0F, rowY + 4.0F, 20.0F, 10.0F,
                intro.savedFlavor() ? 1.0F : 0.0F);
        rowY += ADDON_ROW_HEIGHT;

        boolean replayHover = inside(mouseX, mouseY, rowX + 8.0F, rowY, rowWidth - 16.0F, ADDON_ROW_HEIGHT - 4.0F);
        SkijaUi.rounded(canvas, rowX + 8.0F, rowY, rowWidth - 16.0F, ADDON_ROW_HEIGHT - 4.0F, 3.0F,
                intro.enabled() ? (replayHover ? GuiPalette.section() : GuiPalette.panel()) : GuiPalette.panel());
        String replay = "Replay Intro";
        float replayWidth = SkijaUi.textWidth(replay, 6.6F);
        SkijaUi.text(canvas, replay, rowX + rowWidth * 0.5F - replayWidth * 0.5F, rowY, ADDON_ROW_HEIGHT - 4.0F,
                intro.enabled() ? GuiPalette.primary() : GuiPalette.textFaint(), 6.6F);
    }

    private void drawUiAddon(Canvas canvas, float x, float y, float width, float mouseX, float mouseY) {
        ThemeAddons.Ui ui = editing.ui();
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        SkijaUi.rounded(canvas, rowX, y, rowWidth, ADDON_ROW_HEIGHT * 4.0F + 2.0F,
                GuiPalette.CHIP_RADIUS, GuiPalette.panelInner());

        float rowY = y + 2.0F;
        drawAddonLabel(canvas, "Enabled", rowX, rowY);
        drawToggle(canvas, rowX + rowWidth - 28.0F, rowY + 4.0F, 20.0F, 10.0F, ui.enabled() ? 1.0F : 0.0F);
        rowY += ADDON_ROW_HEIGHT;

        drawAddonLabel(canvas, "Style", rowX, rowY);
        drawAddonValue(canvas, styleName(UiStyles.byId(ui.styleId())), rowX, rowY, rowWidth);
        rowY += ADDON_ROW_HEIGHT;

        boolean dr = UiStyles.isDr(ui.styleId());
        drawAddonLabel(canvas, "Chapter", rowX, rowY);
        drawAddonValue(canvas, dr ? "Ch." + ui.chapter() : "-", rowX, rowY, rowWidth);
        rowY += ADDON_ROW_HEIGHT;

        drawAddonLabel(canvas, "Background", rowX, rowY);
        drawAddonValue(canvas, dr ? backgroundName(ui.backgroundMode()) : "-", rowX, rowY, rowWidth);
    }

    private void drawAddonLabel(Canvas canvas, String label, float rowX, float rowY) {
        SkijaUi.text(canvas, label, rowX + 9.0F, rowY, ADDON_ROW_HEIGHT, GuiPalette.textDim(), 6.6F);
    }

    private void drawAddonValue(Canvas canvas, String value, float rowX, float rowY, float rowWidth) {
        float valueWidth = SkijaUi.textWidth(value, 6.6F);
        SkijaUi.text(canvas, value, rowX + rowWidth - 10.0F - valueWidth, rowY, ADDON_ROW_HEIGHT,
                GuiPalette.primary(), 6.6F);
    }

    private void drawToggle(Canvas canvas, float x, float y, float width, float height, float progress) {
        SkijaUi.rounded(canvas, x, y, width, height, height * 0.5F, GuiPalette.track());
        if (progress > 0.01F) {
            SkijaUi.rounded(canvas, x, y, width, height, height * 0.5F, GuiPalette.primary());
        }
        float knob = height - 3.0F;
        SkijaUi.rounded(canvas, x + 1.5F + progress * (width - knob - 3.0F), y + 1.5F, knob, knob,
                knob * 0.5F, GuiPalette.text());
    }

    private void drawToast(Canvas canvas, float x, float bottom, float width) {
        if (toast == null || System.currentTimeMillis() > toastUntilMs) {
            return;
        }
        String message = toast;
        float messageWidth = SkijaUi.textWidth(message, 6.6F);
        float boxWidth = messageWidth + 18.0F;
        float boxX = x + width - PAD - boxWidth;
        float boxY = bottom + 4.0F;
        SkijaUi.rounded(canvas, boxX, boxY, boxWidth, 18.0F, GuiPalette.CHIP_RADIUS, GuiPalette.section());
        SkijaUi.outline(canvas, boxX, boxY, boxWidth, 18.0F, GuiPalette.CHIP_RADIUS, 1.0F, GuiPalette.outline());
        SkijaUi.text(canvas, message, boxX + 9.0F, boxY, 18.0F, GuiPalette.text(), 6.6F);
    }

    // ------------------------------------------------------------------
    // 列表页输入
    // ------------------------------------------------------------------

    private boolean clickList(float mouseX, float mouseY, int button, float x, float width) {
        List<Row> rows = buildListRows(width);
        for (Row row : rows) {
            if (!inside(mouseX, mouseY, x + PAD, row.y, width - PAD * 2.0F, row.height)) {
                continue;
            }
            if (row.type == RowType.LIST_ADD) {
                ThemeEntry created = Themes.createCustomTheme("Custom Theme", Themes.currentEntry());
                if (created != null) {
                    ThemeRuntime.select(created.id());
                    openEditor(created);
                }
                return true;
            }
            if (row.type == RowType.LIST_CARD) {
                ThemeEntry entry = (ThemeEntry) row.payloadA;
                float deleteX = x + width - PAD - 18.0F;
                boolean custom = !entry.builtin();
                if (custom && mouseX >= deleteX && mouseX <= deleteX + 16.0F) {
                    if (entry.id().equals(pendingDelete)) {
                        pendingDelete = null;
                        Themes.deleteCustomTheme(entry.id());
                        ThemeRuntime.select(Themes.currentId());
                        showToast("Deleted " + entry.name());
                    } else {
                        pendingDelete = entry.id();
                    }
                    return true;
                }
                pendingDelete = null;
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (custom) {
                        openEditor(entry);
                    } else {
                        ThemeEntry copy = Themes.createCustomTheme(entry.name() + " Copy", entry);
                        if (copy != null) {
                            ThemeRuntime.select(copy.id());
                            openEditor(copy);
                        }
                    }
                } else {
                    ThemeRuntime.select(entry.id());
                }
                return true;
            }
            if (row.type == RowType.LIST_FOOTER) {
                float buttonWidth = (width - PAD * 2.0F - 6.0F) * 0.5F;
                if (mouseX <= x + PAD + buttonWidth) {
                    ThemeEntry imported = ThemeFiles.importTheme();
                    if (imported != null) {
                        ThemeRuntime.select(imported.id());
                        showToast("Imported " + imported.name());
                    }
                } else {
                    ThemeFiles.export(Themes.currentEntry());
                }
                return true;
            }
        }
        pendingDelete = null;
        return false;
    }

    // ------------------------------------------------------------------
    // 编辑页输入
    // ------------------------------------------------------------------

    private boolean clickEditor(float mouseX, float mouseY, int button, float x, float width) {
        List<Row> rows = buildEditorRows(width);
        for (Row row : rows) {
            if (!inside(mouseX, mouseY, x + PAD, row.y, width - PAD * 2.0F, row.height)) {
                continue;
            }
            switch (row.type) {
                case TOP_BAR -> {
                    return clickTopBar(mouseX, mouseY, row.y, x, width);
                }
                case SLOT -> {
                    return clickSlot((Slot) row.payloadA, mouseX, mouseY, row.y, x, width);
                }
                case GRADIENT -> {
                    return clickGradient((GradientSpec.Kind) row.payloadA, mouseX, mouseY, row.y, x, width);
                }
                case ADDON_INTRO -> {
                    return clickIntroAddon(mouseX, mouseY, row.y, x, width);
                }
                case ADDON_UI -> {
                    return clickUiAddon(mouseX, mouseY, row.y, x, width);
                }
                default -> {
                }
            }
        }
        return false;
    }

    private boolean clickTopBar(float mouseX, float mouseY, float rowY, float x, float width) {
        if (inside(mouseX, mouseY, x + PAD, rowY, 24.0F, TOP_BAR_HEIGHT)) {
            commitName();
            openList();
            return true;
        }
        float exportWidth = 52.0F;
        float exportX = x + width - PAD - exportWidth;
        if (inside(mouseX, mouseY, exportX, rowY, exportWidth, TOP_BAR_HEIGHT)) {
            // 编辑即保存，导出的就是当前主题
            ThemeFiles.export(Themes.currentEntry());
            showToast("Exported");
            return true;
        }
        float nameX = x + PAD + 30.0F;
        if (mouseX >= nameX && mouseY >= rowY && mouseY <= rowY + TOP_BAR_HEIGHT) {
            focusName = true;
            focusHex = false;
            textBuffer = editing.name();
            textCursor = textBuffer.length();
            selectAll = true;
            return true;
        }
        return false;
    }

    private boolean clickSlot(Slot slot, float mouseX, float mouseY, float rowY, float x, float width) {
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        if (openSlot != slot) {
            openSlot = slot;
            openGradient = null;
            focusHex = false;
            focusName = false;
            return true;
        }
        int color = pick(slot);
        if (inside(mouseX, mouseY, rowX + rowWidth - 42.0F, rowY + 2.0F, 34.0F, 16.0F)) {
            // 点色块收起
            openSlot = null;
            dragChannel = -1;
            return true;
        }
        float channelY = rowY + SLOT_ROW_HEIGHT + 2.0F;
        int channelIndex = (int) ((mouseY - channelY) / CHANNEL_HEIGHT);
        if (channelIndex >= 0 && channelIndex < 4) {
            float sliderX = rowX + 24.0F;
            float sliderWidth = rowWidth - 70.0F;
            dragChannel = channelIndex;
            dragTrackX = x + sliderX;
            dragTrackWidth = sliderWidth;
            dragCursorX = mouseX;
            applyChannelDrag(slot, mouseX);
            return true;
        }
        if (inside(mouseX, mouseY, rowX + 10.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 54.0F, 12.0F)) {
            editing.resetColor(slot);
            saveEditable();
            return true;
        }
        if (inside(mouseX, mouseY, rowX + 70.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 72.0F, 12.0F)) {
            focusHex = true;
            focusName = false;
            textBuffer = String.format("#%08X", color);
            textCursor = textBuffer.length();
            selectAll = true;
            return true;
        }
        return true;
    }

    private boolean clickGradient(GradientSpec.Kind kind, float mouseX, float mouseY, float rowY,
                                  float x, float width) {
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        GradientSpec spec = editing.gradient(kind);
        if (inside(mouseX, mouseY, rowX + rowWidth - 32.0F, rowY + 1.0F, 28.0F, 16.0F)) {
            editing.gradient(kind, spec.withEnabled(!spec.enabled()));
            saveEditable();
            return true;
        }
        if (inside(mouseX, mouseY, rowX + 7.0F, rowY + 18.0F, 30.0F, 13.0F)) {
            openGradient = kind;
            openGradientStart = true;
            openSlot = null;
            focusHex = false;
            return true;
        }
        if (inside(mouseX, mouseY, rowX + 41.0F, rowY + 18.0F, 30.0F, 13.0F)) {
            openGradient = kind;
            openGradientStart = false;
            openSlot = null;
            focusHex = false;
            return true;
        }
        float angleX = rowX + 80.0F;
        float angleWidth = Math.max(20.0F, rowWidth - 96.0F);
        if (inside(mouseX, mouseY, angleX - 4.0F, rowY + 16.0F, angleWidth + 8.0F, 18.0F)) {
            dragChannel = 9;
            dragTrackX = x + angleX;
            dragTrackWidth = angleWidth;
            dragCursorX = mouseX;
            applyAngleDrag(kind, mouseX);
            return true;
        }
        if (kind == openGradient) {
            float channelY = rowY + GRADIENT_ROW_HEIGHT + 2.0F;
            int channelIndex = (int) ((mouseY - channelY) / CHANNEL_HEIGHT);
            if (channelIndex >= 0 && channelIndex < 4) {
                float sliderX = rowX + 24.0F;
                float sliderWidth = rowWidth - 70.0F;
                dragChannel = channelIndex;
                dragTrackX = x + sliderX;
                dragTrackWidth = sliderWidth;
                dragCursorX = mouseX;
                applyGradientChannelDrag(kind, mouseX);
                return true;
            }
            if (inside(mouseX, mouseY, rowX + 70.0F, channelY + 4.0F * CHANNEL_HEIGHT - 2.0F, 72.0F, 12.0F)) {
                focusHex = true;
                focusName = false;
                textBuffer = String.format("#%08X",
                        openGradientStart ? spec.start() : spec.end());
                textCursor = textBuffer.length();
                selectAll = true;
                return true;
            }
        }
        return false;
    }

    private boolean clickIntroAddon(float mouseX, float mouseY, float rowY, float x, float width) {
        ThemeAddons.Intro intro = editing.intro();
        float rowX = x + PAD;
        float rowWidth = width - PAD * 2.0F;
        if (mouseY < rowY + 2.0F || mouseY > rowY + ADDON_ROW_HEIGHT * 5.0F) {
            return false;
        }
        int index = (int) ((mouseY - rowY - 2.0F) / ADDON_ROW_HEIGHT);
        switch (index) {
            case 0 -> {
                editing.intro(intro.withEnabled(!intro.enabled()));
                if (!editing.intro().enabled()) {
                    ThemeRuntime.stopIntro();
                }
                saveEditable();
                ThemeRuntime.applyAddons();
            }
            case 1 -> {
                List<IntroStyles.Style> styles = IntroStyles.all();
                int at = indexOfStyle(styles, intro.styleId());
                editing.intro(intro.withStyle(styles.get((at + 1) % styles.size()).id()));
                saveEditable();
                ThemeRuntime.applyAddons();
            }
            case 2 -> {
                editing.intro(intro.withChapter(intro.chapter() % 5 + 1));
                saveEditable();
                ThemeRuntime.applyAddons();
            }
            case 3 -> {
                editing.intro(intro.withSavedFlavor(!intro.savedFlavor()));
                saveEditable();
            }
            case 4 -> {
                if (mouseX >= rowX + 8.0F && mouseX <= rowX + rowWidth - 8.0F) {
                    saveEditable();
                    if (!ThemeRuntime.replayIntro()) {
                        showToast("Enable the intro first");
                    }
                }
            }
            default -> {
            }
        }
        return true;
    }

    private boolean clickUiAddon(float mouseX, float mouseY, float rowY, float x, float width) {
        ThemeAddons.Ui ui = editing.ui();
        if (mouseY < rowY + 2.0F || mouseY > rowY + ADDON_ROW_HEIGHT * 4.0F) {
            return false;
        }
        int index = (int) ((mouseY - rowY - 2.0F) / ADDON_ROW_HEIGHT);
        boolean dr = UiStyles.isDr(ui.styleId());
        switch (index) {
            case 0 -> {
                editing.ui(ui.withEnabled(!ui.enabled()));
                saveEditable();
                ThemeRuntime.applyAddons();
            }
            case 1 -> {
                List<UiStyles.Style> styles = UiStyles.all();
                int at = indexOfStyle(styles, ui.styleId());
                editing.ui(ui.withStyle(styles.get((at + 1) % styles.size()).id()));
                saveEditable();
                ThemeRuntime.applyAddons();
            }
            case 2 -> {
                if (dr) {
                    editing.ui(ui.withChapter(ui.chapter() % 5 + 1));
                    saveEditable();
                    ThemeRuntime.applyAddons();
                }
            }
            case 3 -> {
                if (dr) {
                    ThemeAddons.BackgroundMode next = ui.backgroundMode() == ThemeAddons.BackgroundMode.FOUNTAIN
                            ? ThemeAddons.BackgroundMode.DOOR
                            : ThemeAddons.BackgroundMode.FOUNTAIN;
                    editing.ui(ui.withBackgroundMode(next));
                    saveEditable();
                    ThemeRuntime.applyAddons();
                }
            }
            default -> {
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 公共输入
    // ------------------------------------------------------------------

    public boolean mouseClicked(float x, float width, float mouseX, float contentMouseY, int button) {
        if (page == Page.LIST) {
            return clickList(mouseX, contentMouseY, button, x, width);
        }
        return clickEditor(mouseX, contentMouseY, button, x, width);
    }

    public boolean mouseDragged(float deltaX, float deltaY) {
        if (dragChannel < 0) {
            return false;
        }
        dragCursorX += deltaX;
        if (dragChannel == 9) {
            if (openGradient != null) {
                applyAngleDrag(openGradient, dragCursorX);
            }
        } else if (openSlot != null) {
            applyChannelDrag(openSlot, dragCursorX);
        } else if (openGradient != null) {
            applyGradientChannelDrag(openGradient, dragCursorX);
        }
        return true;
    }

    public void mouseReleased() {
        if (dragChannel >= 0) {
            saveEditable();
        }
        dragChannel = -1;
    }

    public boolean keyPressed(int keyCode) {
        if (page != Page.EDITOR) {
            return false;
        }
        if (focusName || focusHex) {
            return handleTextKey(keyCode);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (openSlot != null || openGradient != null) {
                openSlot = null;
                openGradient = null;
                return true;
            }
            openList();
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (page != Page.EDITOR || (!focusName && !focusHex) || chr < 32 || chr == 127) {
            return false;
        }
        if (selectAll) {
            textBuffer = "";
            textCursor = 0;
            selectAll = false;
        }
        if (textBuffer.length() < 64) {
            textBuffer = textBuffer.substring(0, textCursor) + chr + textBuffer.substring(textCursor);
            textCursor++;
        }
        applyTextBuffer();
        return true;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private boolean handleTextKey(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commitText();
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> textCursor = Math.max(0, textCursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> textCursor = Math.min(textBuffer.length(), textCursor + 1);
            case GLFW.GLFW_KEY_HOME -> textCursor = 0;
            case GLFW.GLFW_KEY_END -> textCursor = textBuffer.length();
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (selectAll) {
                    textBuffer = "";
                    textCursor = 0;
                    selectAll = false;
                } else if (textCursor > 0) {
                    textBuffer = textBuffer.substring(0, textCursor - 1) + textBuffer.substring(textCursor);
                    textCursor--;
                }
                applyTextBuffer();
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (selectAll) {
                    textBuffer = "";
                    textCursor = 0;
                    selectAll = false;
                } else if (textCursor < textBuffer.length()) {
                    textBuffer = textBuffer.substring(0, textCursor) + textBuffer.substring(textCursor + 1);
                }
                applyTextBuffer();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void applyTextBuffer() {
        if (focusHex) {
            applyHex(textBuffer);
        } else if (focusName) {
            editing.rename(textBuffer);
        }
    }

    private void commitText() {
        if (focusHex) {
            applyHex(textBuffer);
        }
        if (focusName) {
            editing.rename(textBuffer);
        }
        focusHex = false;
        focusName = false;
        saveEditable();
    }

    private void commitName() {
        if (focusName) {
            editing.rename(textBuffer);
            focusName = false;
            saveEditable();
        }
    }

    private void applyHex(String hex) {
        Integer parsed = ThemeStore.parseColor(new JsonPrimitive(hex));
        if (parsed == null) {
            return;
        }
        if (openSlot != null) {
            editing.color(openSlot, parsed);
        } else if (openGradient != null) {
            GradientSpec spec = editing.gradient(openGradient);
            editing.gradient(openGradient, openGradientStart ? spec.withStart(parsed) : spec.withEnd(parsed));
        }
    }

    private void applyChannelDrag(Slot slot, float mouseX) {
        float fraction = (mouseX - dragTrackX) / Math.max(1.0F, dragTrackWidth);
        int channelValue = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * 255.0F);
        int color = pick(slot);
        editing.color(slot, withChannel(color, dragChannel, channelValue));
        textBuffer = String.format("#%08X", editing.color(slot));
        textCursor = textBuffer.length();
    }

    private void applyGradientChannelDrag(GradientSpec.Kind kind, float mouseX) {
        float fraction = (mouseX - dragTrackX) / Math.max(1.0F, dragTrackWidth);
        int channelValue = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * 255.0F);
        GradientSpec spec = editing.gradient(kind);
        int color = openGradientStart ? spec.start() : spec.end();
        int updated = withChannel(color, dragChannel, channelValue);
        editing.gradient(kind, openGradientStart ? spec.withStart(updated) : spec.withEnd(updated));
    }

    private void applyAngleDrag(GradientSpec.Kind kind, float mouseX) {
        float fraction = (mouseX - dragTrackX) / Math.max(1.0F, dragTrackWidth);
        float angle = Math.max(0.0F, Math.min(1.0F, fraction)) * 360.0F;
        editing.gradient(kind, editing.gradient(kind).withAngle(angle));
    }

    private void saveEditable() {
        if (editing == null) {
            return;
        }
        ThemeEntry saved = editing.save();
        if (saved != null) {
            // 只同步 DR 状态，不触发“切换主题”的演出重播
            ThemeRuntime.applyAddons();
        }
    }

    private void showToast(String message) {
        toast = message;
        toastUntilMs = System.currentTimeMillis() + 2400L;
    }

    private boolean blink() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }

    private static int indexOfStyle(List<?> styles, String id) {
        for (int i = 0; i < styles.size(); i++) {
            Object style = styles.get(i);
            String styleId = style instanceof IntroStyles.Style intro ? intro.id()
                    : style instanceof UiStyles.Style ui ? ui.id() : "";
            if (styleId.equalsIgnoreCase(id)) {
                return i;
            }
        }
        return 0;
    }

    private static String styleName(IntroStyles.Style style) {
        return style == null ? "-" : style.displayName();
    }

    private static String styleName(UiStyles.Style style) {
        return style == null ? "-" : style.displayName();
    }

    private static String backgroundName(ThemeAddons.BackgroundMode mode) {
        return mode == ThemeAddons.BackgroundMode.DOOR ? "Door" : "Fountain";
    }

    private static int pick(ThemeEntry entry, Slot slot) {
        Integer explicit = entry.palette().get(slot);
        return explicit != null ? explicit : effective(slot);
    }

    private int pick(Slot slot) {
        Integer explicit = editing == null ? null : editing.color(slot);
        return explicit != null ? explicit : effective(slot);
    }

    /** 槽位未显式设置时的全局回退值（编辑器显示“生效值”用）。 */
    private static int effective(Slot slot) {
        return switch (slot) {
            case WINDOW_BG, PANEL -> GuiPalette.panel();
            case WINDOW_HEADER, HEADER -> GuiPalette.header();
            case WINDOW_STROKE, OUTLINE -> GuiPalette.outline();
            case OUTLINE_VARIANT, STROKE_SOFT -> GuiPalette.outlineVariant();
            case PANEL_INNER -> GuiPalette.panelInner();
            case SECTION -> GuiPalette.section();
            case SURFACE -> UiTheme.surface();
            case SURFACE_HOVER -> GuiPalette.hover();
            case BACKDROP -> UiTheme.backdrop();
            case CARD_ENABLED -> GuiPalette.secondaryContainer();
            case CARD_DISABLED -> GuiPalette.trackFaint();
            case TEXT_PRIMARY -> GuiPalette.text();
            case TEXT_MUTED -> GuiPalette.textDim();
            case TEXT_FAINT -> GuiPalette.textFaint();
            case ACCENT -> GuiPalette.primary();
            case ACCENT_SOFT -> GuiPalette.active();
            case SUCCESS -> UiTheme.success();
            case WARNING -> UiTheme.warning();
            case DANGER -> UiTheme.danger();
            case INFO -> UiTheme.info();
            case TRACK -> GuiPalette.track();
            case SCROLL -> GuiPalette.scroll();
            case ISLAND_BODY -> 0xF20A0C10;
            case ISLAND_EDGE -> 0xA8FFFFFF;
            case ISLAND_GLOW -> 0x329BD7FF;
            case NOTIFY_ACCENT -> 0xFF9BD7FF;
        };
    }

    private static int channelValue(int color, int channel) {
        return switch (channel) {
            case 0 -> (color >> 16) & 0xFF;
            case 1 -> (color >> 8) & 0xFF;
            case 2 -> color & 0xFF;
            default -> (color >>> 24) & 0xFF;
        };
    }

    private static int channelTint(int color, int channel) {
        int value = channelValue(color, channel);
        return switch (channel) {
            case 0 -> 0xFF000000 | (value << 16) | 0x000000;
            case 1 -> 0xFF000000 | (value << 8);
            case 2 -> 0xFF000000 | value;
            default -> 0xFF000000 | (0xFFFFFF & color);
        };
    }

    private static int withChannel(int color, int channel, int value) {
        int v = Math.max(0, Math.min(255, value));
        return switch (channel) {
            case 0 -> (color & 0xFF00FFFF) | (v << 16);
            case 1 -> (color & 0xFFFF00FF) | (v << 8);
            case 2 -> (color & 0xFFFFFF00) | v;
            default -> (color & 0x00FFFFFF) | (v << 24);
        };
    }

    private static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static String fit(String text, float maxWidth, float size) {
        if (text == null || maxWidth <= 0.0F) {
            return "";
        }
        if (SkijaUi.boldTextWidth(text, size) <= maxWidth) {
            return text;
        }
        String value = text;
        while (!value.isEmpty()) {
            String candidate = value + "...";
            if (SkijaUi.boldTextWidth(candidate, size) <= maxWidth) {
                return candidate;
            }
            value = value.substring(0, value.length() - 1);
        }
        return "";
    }
}
