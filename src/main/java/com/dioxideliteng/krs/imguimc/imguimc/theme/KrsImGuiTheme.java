package com.dioxideliteng.krs.imguimc.theme;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

final class KrsImGuiTheme {

    private KrsImGuiTheme() {
    }

    static void applyClassic() {
        ImGuiStyle style = ImGui.getStyle();
        applyLayout(style);
        applyDarkColors(style, 150, 105, 255, 225, 95, 205);
    }

    static void applyDark() {
        ImGuiStyle style = ImGui.getStyle();
        applyLayout(style);
        applyDarkColors(style, 185, 125, 255, 245, 110, 205);
    }

    static void applyLight() {
        ImGuiStyle style = ImGui.getStyle();
        applyLayout(style);

        style.setColor(ImGuiCol.Text, 30, 27, 42, 255);
        style.setColor(ImGuiCol.TextDisabled, 104, 96, 120, 230);
        style.setColor(ImGuiCol.WindowBg, 250, 248, 255, 252);
        style.setColor(ImGuiCol.ChildBg, 255, 255, 255, 245);
        style.setColor(ImGuiCol.PopupBg, 255, 255, 255, 250);
        style.setColor(ImGuiCol.Border, 112, 96, 140, 78);
        style.setColor(ImGuiCol.BorderShadow, 0, 0, 0, 0);
        style.setColor(ImGuiCol.FrameBg, 247, 244, 255, 255);
        style.setColor(ImGuiCol.FrameBgHovered, 232, 224, 255, 255);
        style.setColor(ImGuiCol.FrameBgActive, 218, 205, 255, 255);
        style.setColor(ImGuiCol.TitleBg, 238, 233, 249, 255);
        style.setColor(ImGuiCol.TitleBgActive, 229, 222, 247, 255);
        style.setColor(ImGuiCol.TitleBgCollapsed, 238, 233, 249, 240);
        style.setColor(ImGuiCol.MenuBarBg, 240, 236, 249, 252);
        style.setColor(ImGuiCol.ScrollbarBg, 78, 60, 110, 24);
        style.setColor(ImGuiCol.ScrollbarGrab, 108, 82, 160, 105);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 136, 88, 220, 150);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 176, 96, 210, 190);
        style.setColor(ImGuiCol.CheckMark, 118, 72, 210, 255);
        style.setColor(ImGuiCol.SliderGrab, 118, 72, 210, 230);
        style.setColor(ImGuiCol.SliderGrabActive, 190, 82, 185, 255);
        style.setColor(ImGuiCol.Button, 232, 224, 255, 255);
        style.setColor(ImGuiCol.ButtonHovered, 218, 205, 255, 255);
        style.setColor(ImGuiCol.ButtonActive, 204, 188, 250, 255);
        style.setColor(ImGuiCol.Header, 239, 234, 252, 255);
        style.setColor(ImGuiCol.HeaderHovered, 224, 214, 255, 255);
        style.setColor(ImGuiCol.HeaderActive, 211, 196, 252, 255);
        style.setColor(ImGuiCol.Separator, 112, 96, 140, 68);
        style.setColor(ImGuiCol.SeparatorHovered, 136, 88, 220, 150);
        style.setColor(ImGuiCol.SeparatorActive, 190, 82, 185, 190);
        style.setColor(ImGuiCol.ResizeGrip, 136, 88, 220, 82);
        style.setColor(ImGuiCol.ResizeGripHovered, 136, 88, 220, 132);
        style.setColor(ImGuiCol.ResizeGripActive, 190, 82, 185, 175);
        style.setColor(ImGuiCol.Tab, 239, 235, 249, 255);
        style.setColor(ImGuiCol.TabHovered, 222, 211, 255, 255);
        style.setColor(ImGuiCol.TabActive, 213, 199, 250, 255);
        style.setColor(ImGuiCol.TabUnfocused, 232, 228, 241, 240);
        style.setColor(ImGuiCol.TabUnfocusedActive, 224, 214, 248, 255);
        style.setColor(ImGuiCol.DockingPreview, 136, 88, 220, 150);
        style.setColor(ImGuiCol.DockingEmptyBg, 240, 236, 249, 230);
        style.setColor(ImGuiCol.PlotLines, 118, 72, 210, 230);
        style.setColor(ImGuiCol.PlotLinesHovered, 190, 82, 185, 255);
        style.setColor(ImGuiCol.PlotHistogram, 118, 72, 210, 220);
        style.setColor(ImGuiCol.PlotHistogramHovered, 190, 82, 185, 250);
        style.setColor(ImGuiCol.TableHeaderBg, 239, 235, 249, 255);
        style.setColor(ImGuiCol.TableBorderStrong, 112, 96, 140, 82);
        style.setColor(ImGuiCol.TableBorderLight, 112, 96, 140, 48);
        style.setColor(ImGuiCol.TableRowBg, 255, 255, 255, 0);
        style.setColor(ImGuiCol.TableRowBgAlt, 80, 58, 120, 16);
        style.setColor(ImGuiCol.TextSelectedBg, 136, 88, 220, 96);
        style.setColor(ImGuiCol.DragDropTarget, 190, 82, 185, 210);
        style.setColor(ImGuiCol.NavHighlight, 136, 88, 220, 170);
        style.setColor(ImGuiCol.NavWindowingHighlight, 255, 255, 255, 180);
        style.setColor(ImGuiCol.NavWindowingDimBg, 24, 28, 36, 72);
        style.setColor(ImGuiCol.ModalWindowDimBg, 24, 28, 36, 120);
    }

    private static void applyLayout(ImGuiStyle style) {
        float scale = getScale();

        style.setAlpha(1.0f);
        style.setDisabledAlpha(0.58f);
        style.setWindowPadding(14f * scale, 11f * scale);
        style.setFramePadding(9f * scale, 6f * scale);
        style.setCellPadding(6f * scale, 4f * scale);
        style.setItemSpacing(9f * scale, 7f * scale);
        style.setItemInnerSpacing(7f * scale, 5f * scale);
        style.setIndentSpacing(18f * scale);
        style.setScrollbarSize(14f * scale);
        style.setGrabMinSize(10f * scale);
        style.setWindowRounding(8f * scale);
        style.setChildRounding(6f * scale);
        style.setFrameRounding(5f * scale);
        style.setPopupRounding(6f * scale);
        style.setScrollbarRounding(7f * scale);
        style.setGrabRounding(5f * scale);
        style.setTabRounding(6f * scale);
        style.setWindowBorderSize(1f * scale);
        style.setChildBorderSize(1f * scale);
        style.setPopupBorderSize(1f * scale);
        style.setFrameBorderSize(1f * scale);
        style.setTabBorderSize(0f);
        style.setWindowTitleAlign(0.5f, 0.5f);
        style.setAntiAliasedLines(true);
        style.setAntiAliasedFill(true);
    }

    private static void applyDarkColors(ImGuiStyle style, int accentRed, int accentGreen, int accentBlue, int glowRed, int glowGreen, int glowBlue) {
        style.setColor(ImGuiCol.Text, 244, 247, 252, 255);
        style.setColor(ImGuiCol.TextDisabled, 148, 158, 174, 215);
        style.setColor(ImGuiCol.WindowBg, 17, 20, 25, 242);
        style.setColor(ImGuiCol.ChildBg, 23, 27, 34, 228);
        style.setColor(ImGuiCol.PopupBg, 20, 24, 31, 248);
        style.setColor(ImGuiCol.Border, 255, 255, 255, 68);
        style.setColor(ImGuiCol.BorderShadow, 0, 0, 0, 0);
        style.setColor(ImGuiCol.FrameBg, 38, 43, 52, 235);
        style.setColor(ImGuiCol.FrameBgHovered, accentRed, accentGreen, accentBlue, 76);
        style.setColor(ImGuiCol.FrameBgActive, glowRed, glowGreen, glowBlue, 104);
        style.setColor(ImGuiCol.TitleBg, 13, 15, 19, 255);
        style.setColor(ImGuiCol.TitleBgActive, 25, 29, 36, 255);
        style.setColor(ImGuiCol.TitleBgCollapsed, 13, 15, 19, 235);
        style.setColor(ImGuiCol.MenuBarBg, 25, 29, 36, 245);
        style.setColor(ImGuiCol.ScrollbarBg, 0, 0, 0, 38);
        style.setColor(ImGuiCol.ScrollbarGrab, 255, 255, 255, 90);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, accentRed, accentGreen, accentBlue, 125);
        style.setColor(ImGuiCol.ScrollbarGrabActive, glowRed, glowGreen, glowBlue, 165);
        style.setColor(ImGuiCol.CheckMark, accentRed, accentGreen, accentBlue, 255);
        style.setColor(ImGuiCol.SliderGrab, accentRed, accentGreen, accentBlue, 225);
        style.setColor(ImGuiCol.SliderGrabActive, glowRed, glowGreen, glowBlue, 255);
        style.setColor(ImGuiCol.Button, accentRed, accentGreen, accentBlue, 78);
        style.setColor(ImGuiCol.ButtonHovered, accentRed, accentGreen, accentBlue, 128);
        style.setColor(ImGuiCol.ButtonActive, glowRed, glowGreen, glowBlue, 158);
        style.setColor(ImGuiCol.Header, accentRed, accentGreen, accentBlue, 42);
        style.setColor(ImGuiCol.HeaderHovered, accentRed, accentGreen, accentBlue, 84);
        style.setColor(ImGuiCol.HeaderActive, glowRed, glowGreen, glowBlue, 118);
        style.setColor(ImGuiCol.Separator, 255, 255, 255, 68);
        style.setColor(ImGuiCol.SeparatorHovered, accentRed, accentGreen, accentBlue, 130);
        style.setColor(ImGuiCol.SeparatorActive, glowRed, glowGreen, glowBlue, 170);
        style.setColor(ImGuiCol.ResizeGrip, accentRed, accentGreen, accentBlue, 60);
        style.setColor(ImGuiCol.ResizeGripHovered, accentRed, accentGreen, accentBlue, 118);
        style.setColor(ImGuiCol.ResizeGripActive, glowRed, glowGreen, glowBlue, 160);
        style.setColor(ImGuiCol.Tab, 30, 35, 43, 245);
        style.setColor(ImGuiCol.TabHovered, accentRed, accentGreen, accentBlue, 130);
        style.setColor(ImGuiCol.TabActive, glowRed, glowGreen, glowBlue, 150);
        style.setColor(ImGuiCol.TabUnfocused, 22, 26, 33, 220);
        style.setColor(ImGuiCol.TabUnfocusedActive, glowRed, glowGreen, glowBlue, 105);
        style.setColor(ImGuiCol.DockingPreview, accentRed, accentGreen, accentBlue, 145);
        style.setColor(ImGuiCol.DockingEmptyBg, 17, 20, 25, 210);
        style.setColor(ImGuiCol.PlotLines, accentRed, accentGreen, accentBlue, 225);
        style.setColor(ImGuiCol.PlotLinesHovered, glowRed, glowGreen, glowBlue, 255);
        style.setColor(ImGuiCol.PlotHistogram, accentRed, accentGreen, accentBlue, 210);
        style.setColor(ImGuiCol.PlotHistogramHovered, glowRed, glowGreen, glowBlue, 245);
        style.setColor(ImGuiCol.TableHeaderBg, 34, 39, 48, 245);
        style.setColor(ImGuiCol.TableBorderStrong, 255, 255, 255, 72);
        style.setColor(ImGuiCol.TableBorderLight, 255, 255, 255, 44);
        style.setColor(ImGuiCol.TableRowBg, 255, 255, 255, 0);
        style.setColor(ImGuiCol.TableRowBgAlt, 255, 255, 255, 24);
        style.setColor(ImGuiCol.TextSelectedBg, accentRed, accentGreen, accentBlue, 76);
        style.setColor(ImGuiCol.DragDropTarget, glowRed, glowGreen, glowBlue, 210);
        style.setColor(ImGuiCol.NavHighlight, accentRed, accentGreen, accentBlue, 165);
        style.setColor(ImGuiCol.NavWindowingHighlight, 255, 255, 255, 180);
        style.setColor(ImGuiCol.NavWindowingDimBg, 0, 0, 0, 80);
        style.setColor(ImGuiCol.ModalWindowDimBg, 0, 0, 0, 110);
    }

    private static float getScale() {
        ImGuiIO io = ImGui.getIO();
        float scale = Math.max(io.getDisplayFramebufferScaleX(), io.getDisplayFramebufferScaleY());

        if (!Float.isFinite(scale) || scale < 1.0f)
            return 1.0f;

        return scale;
    }
}
