package com.dioxidelite.client.gui.clickgui.pages;

import com.dioxidelite.Config;
import com.dioxidelite.client.gui.clickgui.UiText;
import com.dioxidelite.client.gui.clickgui.widget.SettingCycle;
import com.dioxidelite.client.gui.clickgui.widget.SettingModule;
import com.dioxidelite.client.gui.clickgui.widget.SettingSlider;
import com.dioxidelite.client.gui.clickgui.widget.SettingToggle;

import java.util.List;

public class ThemePage extends BasePage {
    public ThemePage() {
        modules.add(new SettingModule(
                UiText.t("ClickGUI 主题", "ClickGUI Theme"),
                UiText.t("在原版、Minimal 与 Signature 三套完整 ClickGUI 布局之间切换", "Switch between the complete original, Minimal and Signature ClickGUI layouts"),
                null)
                .addSub(UiText.t("界面", "Layout"),
                        UiText.t("主题只改变界面表现，不改变功能逻辑", "Changes presentation only; module logic stays untouched"),
                        new SettingCycle(
                                List.of(UiText.t("DioxideLite", "DioxideLite"), UiText.t("DioxideLite Minimal", "DioxideLite Minimal"), UiText.t("DioxideLite Signature", "DioxideLite Signature")),
                                () -> switch (Config.clickGuiTheme) { case ORIGINAL -> 0; case MINIMAL_POP -> 1; case SIGNATURE -> 2; },
                                i -> { Config.clickGuiTheme = i == 2 ? Config.ClickGuiTheme.SIGNATURE : (i == 1 ? Config.ClickGuiTheme.MINIMAL_POP : Config.ClickGuiTheme.ORIGINAL); Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("Liquid Glass 全局视觉", "Liquid Glass Global Visuals"),
                UiText.t("一键将液态玻璃材质应用到游戏内 HUD、聊天、物品栏和界面卡片；不影响主菜单", "Apply the Liquid Glass material to in-game HUD, chat, inventory and screen cards; the main menu is excluded"),
                new SettingToggle(() -> Config.liquidGlassAllVisuals, v -> { Config.liquidGlassAllVisuals = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("视觉模板", "Visual Template"),
                UiText.t("切换 DioxideLite 的整套界面语言", "Switch the complete DioxideLite visual language"),
                null)
                .addSub(UiText.t("模板", "Template"),
                        UiText.t("Liquid Glass / Aurora / RISE Clean / Minimal / Signature", "Liquid Glass / Aurora / RISE Clean / Minimal / Signature"),
                        new SettingCycle(
                                List.of(
                                        UiText.t("Liquid Glass", "Liquid Glass"),
                                        UiText.t("Aurora", "Aurora"),
                                        UiText.t("RISE Clean", "RISE Clean"),
                                        UiText.t("DioxideLite Minimal", "DioxideLite Minimal"),
                                        UiText.t("DioxideLite Signature", "DioxideLite Signature")),
                                () -> Config.visualStyle.ordinal(),
                                i -> {
                                    Config.visualStyle = Config.VisualStyle.values()[Math.max(0, Math.min(i, Config.VisualStyle.values().length - 1))];
                                    Config.save();
                                }))
                .addSub(UiText.t("玻璃透明度", "Glass Opacity"),
                        UiText.t("控制液态玻璃的透光程度", "Controls glass translucency"),
                        new SettingSlider(20, 90, "%.0f%%",
                                () -> (double) Config.glassOpacity * 100,
                                v -> { Config.glassOpacity = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("玻璃模糊", "Glass Blur"),
                        UiText.t("背景模糊强度；性能模式会自动跳过", "Background blur strength; performance mode can skip it"),
                        new SettingSlider(0, 200, "%.0f%%",
                                () -> (double) Config.glassBlur * 100,
                                v -> { Config.glassBlur = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("边缘高光", "Edge Highlight"),
                        UiText.t("调整玻璃边缘的柔和高光", "Adjust the soft edge highlight"),
                        new SettingSlider(0, 100, "%.0f%%",
                                () -> (double) Config.glassHighlight * 100,
                                v -> { Config.glassHighlight = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("性能模式", "Performance Mode"),
                        UiText.t("关闭实时背景模糊，减少低端设备 GPU 压力", "Skip live background blur to reduce GPU load"),
                        new SettingToggle(() -> Config.performanceMode,
                                v -> { Config.performanceMode = v; Config.save(); })));

        modules.add(new SettingModule(
                UiText.t("动态岛", "Dynamic Island"),
                UiText.t("可独立调整尺寸、模糊和透明度", "Independently adjust size, blur and opacity"),
                null)
                .addSub(UiText.t("宽度", "Width"),
                        UiText.t("动态岛基础宽度比例", "Base width scale"),
                        new SettingSlider(55, 180, "%.0f%%",
                                () -> (double) Config.dynamicIslandWidthScale * 100,
                                v -> { Config.dynamicIslandWidthScale = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("高度", "Height"),
                        UiText.t("动态岛基础高度比例", "Base height scale"),
                        new SettingSlider(65, 160, "%.0f%%",
                                () -> (double) Config.dynamicIslandHeightScale * 100,
                                v -> { Config.dynamicIslandHeightScale = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("模糊", "Blur"),
                        UiText.t("动态岛背景模糊", "Dynamic Island background blur"),
                        new SettingSlider(0, 200, "%.0f%%",
                                () -> (double) Config.dynamicIslandBlur * 100,
                                v -> { Config.dynamicIslandBlur = v.floatValue() / 100f; Config.save(); }))
                .addSub(UiText.t("透明度", "Opacity"),
                        UiText.t("动态岛玻璃层透明度", "Dynamic Island glass opacity"),
                        new SettingSlider(20, 100, "%.0f%%",
                                () -> (double) Config.dynamicIslandOpacity * 100,
                                v -> { Config.dynamicIslandOpacity = v.floatValue() / 100f; Config.save(); })));
    }

    @Override public String getTitle() { return UiText.t("视觉设置", "Visual Settings"); }
    @Override public String getSubtitle() { return UiText.t("液态玻璃、模板与动态岛", "Liquid Glass, templates and Dynamic Island"); }
}
