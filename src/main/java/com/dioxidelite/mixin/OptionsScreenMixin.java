package com.dioxidelite.mixin;

import com.dioxidelite.ui.screen.BakeProgressWatcher;
import com.dioxidelite.ui.screen.MenuBackgroundSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds shared background controls to Minecraft's options screen.
 *
 * <p>第一行是「模式循环」；导入/网格/默认放在同一行，放不下就自动换到第二行。
 * 视频首次使用需要烘焙帧序列，烘焙期间所有按钮禁用并显示进度。
 *
 * <p>注意：26.1.2 的 {@link OptionsScreen} 只声明 {@code init}，渲染入口在父类
 * {@link Screen}（{@code extractRenderStateWithTooltipAndSubtitles}），而 Mixin 不向父类查找
 * 注入目标 —— 所以每帧轮询走 {@link BakeProgressWatcher} 接口，由
 * {@code VanillaScreenThemeMixin} 转发。
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen implements BakeProgressWatcher {

    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    /** 第一行是「模式循环」；导入/网格/默认放在同一行，放不下就自动换到第二行。 */
    private static final int[] BUTTON_WIDTHS = {96, 84, 88, 60, 68};

    @Unique
    private final List<Button> DioxideLite$backgroundButtons = new ArrayList<>();
    @Unique
    private MenuBackgroundSettings.VideoBakeTask DioxideLite$bakeTask;
    /** 第一行的「模式循环」按钮，它的文案会跟着当前背景模式变。 */
    @Unique
    private Button DioxideLite$modeButton;
    @Unique
    private String DioxideLite$modeLabelText;

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void DioxideLite$addBackgroundControls(CallbackInfo ci) {
        // rebuildWidgets（改 GUI 缩放等）会重跑 init：旧控件已经被 clearWidgets 清掉了，
        // 引用也要一起丢掉，否则列表会一直堆失效按钮。
        DioxideLite$backgroundButtons.clear();
        DioxideLite$modeButton = null;
        DioxideLite$modeLabelText = null;

        DioxideLite$modeButton = addBackgroundButton(BUTTON_WIDTHS[0],
                MenuBackgroundSettings.currentModeLabel(),
                Component.translatableWithFallback("DioxideLite.options_screen.cycle_tooltip",
                        "Click to cycle the menu background"),
                button -> {
                    trackBake(MenuBackgroundSettings.cycleMode());
                    refreshBackgroundButtons();
                });
        DioxideLite$modeLabelText = DioxideLite$modeButton.getMessage().getString();

        for (int index = 1; index < BUTTON_WIDTHS.length; index++) {
            final int slot = index;
            addBackgroundButton(BUTTON_WIDTHS[index], labelFor(index), null,
                    button -> onBackgroundAction(slot));
        }
        // init 末尾自己会调一次 repositionElements，但那时按钮还没建出来，这里必须补排一次。
        DioxideLite$layoutBackgroundButtons();
        refreshBackgroundButtons();
    }

    /**
     * 按当前窗口宽度摆放这排按钮。
     *
     * <p>{@code init} 与 {@code repositionElements} 都会调 —— 改窗口大小 / 改 GUI 缩放只走后者，
     * 不重排的话按钮会停在被清掉的旧坐标上。
     */
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void DioxideLite$repositionBackgroundControls(CallbackInfo ci) {
        DioxideLite$layoutBackgroundButtons();
    }

    @Unique
    private void DioxideLite$layoutBackgroundButtons() {
        if (DioxideLite$backgroundButtons.size() != BUTTON_WIDTHS.length) return;

        int total = sum(BUTTON_WIDTHS) + GAP * (BUTTON_WIDTHS.length - 1);
        boolean twoRows = width - 16 < total;
        int x = twoRows ? 8 : Math.max(8, width - total - 8);
        int y = width < 500 ? 32 : 8;

        for (int i = 0; i < BUTTON_WIDTHS.length; i++) {
            int buttonWidth = BUTTON_WIDTHS[i];
            // 第0 个（模式循环）固定留在第一行行首，其余在单行模式下放不下就换行。
            if (i > 0 && !twoRows && x + buttonWidth > width - 8) {
                x = 8;
                y += BUTTON_HEIGHT + GAP;
            }
            Button button = DioxideLite$backgroundButtons.get(i);
            button.setX(x);
            button.setY(y);
            x += buttonWidth + GAP;
        }
    }

    @Unique
    private static Component labelFor(int index) {
        return switch (index) {
            case 1 -> Component.translatableWithFallback(
                    "DioxideLite.options_screen.import_pic", "IMPORT PIC");
            case 2 -> Component.translatableWithFallback(
                    "DioxideLite.options_screen.import_video", "IMPORT VIDEO");
            case 3 -> Component.translatableWithFallback(
                    "DioxideLite.options_screen.grid", "GRID");
            case 4 -> Component.translatableWithFallback(
                    "DioxideLite.options_screen.default", "DEFAULT");
            default -> Component.literal("?");
        };
    }

    @Unique
    private void onBackgroundAction(int index) {
        switch (index) {
            case 1 -> {
                if (MenuBackgroundSettings.importImage()) refreshBackgroundButtons();
            }
            case 2 -> trackBake(MenuBackgroundSettings.importVideo());
            case 3 -> {
                MenuBackgroundSettings.useGrid();
                refreshBackgroundButtons();
            }
            case 4 -> {
                MenuBackgroundSettings.reset();
                refreshBackgroundButtons();
            }
            default -> {
            }
        }
    }

    @Unique
    private Button addBackgroundButton(int buttonWidth, Component label,
                                      Component tooltip, Button.OnPress action) {
        Button.Builder builder = Button.builder(label, action)
                .bounds(0, 0, buttonWidth, BUTTON_HEIGHT);
        if (tooltip != null) {
            builder.tooltip(Tooltip.create(tooltip));
        }
        Button button = addRenderableWidget(builder.build());
        DioxideLite$backgroundButtons.add(button);
        return button;
    }

    @Unique
    private void trackBake(MenuBackgroundSettings.VideoBakeTask task) {
        if (task == null) return;
        DioxideLite$bakeTask = task;
        refreshBackgroundButtons();
    }

    /** 烘焙在后台线程进行，这里每帧轮询一次完成状态并刷新按钮可用性。 */
    @Override
    public void dioxideLite$pollBake() {
        if (DioxideLite$bakeTask != null) refreshBackgroundButtons();
        DioxideLite$syncModeButtonLabel();
    }

    /**
     * 模式按钮直接显示当前背景模式；视频烘焙期间改成显示进度。
     *
     * <p>1080p 视频要烘焙约 18 秒，原来这段时间界面上什么反馈都没有，
     * 看着就像「点了没反应」。
     */
    @Unique
    private void DioxideLite$syncModeButtonLabel() {
        Button button = DioxideLite$modeButton;
        if (button == null) return;

        MenuBackgroundSettings.VideoBakeTask task = DioxideLite$bakeTask;
        Component label;
        if (task != null && !task.isFinished()) {
            label = Component.translatableWithFallback("DioxideLite.options_screen.baking",
                    "Baking");
            int percent = Math.round(task.progress() * 100.0F);
            if (percent >= 0) {
                label = label.copy().append(Component.literal(" " + percent + "%"));
            }
        } else {
            label = MenuBackgroundSettings.currentModeLabel();
        }

        String text = label.getString();
        if (!text.equals(DioxideLite$modeLabelText)) {
            DioxideLite$modeLabelText = text;
            button.setMessage(label);
        }
    }

    /** 烘焙期间禁用按钮；烘焙结束后恢复。 */
    @Unique
    private void refreshBackgroundButtons() {
        MenuBackgroundSettings.VideoBakeTask task = DioxideLite$bakeTask;
        boolean baking = task != null && !task.isFinished();
        for (Button button : DioxideLite$backgroundButtons) {
            button.active = !baking;
        }
        if (!baking && task != null) {
            DioxideLite$bakeTask = null;
            // 烘焙成功时模式已经由后台登记、渲染线程切过去了，这里只需恢复按钮。
            for (Button button : DioxideLite$backgroundButtons) {
                button.active = true;
            }
        }
    }

    @Unique
    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }
}