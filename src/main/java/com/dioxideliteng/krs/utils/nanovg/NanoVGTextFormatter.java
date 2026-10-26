package com.dioxideliteng.krs.utils.nanovg;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

public final class NanoVGTextFormatter {
    private NanoVGTextFormatter() {
    }

    public static String formatColors(Component component) {
        return formatColors(component, 0);
    }

    public static String formatColors(Component component, int skippedCharacters) {
        if (component == null)
            return "";

        StringBuilder formattedText = new StringBuilder(32);
        int[] remainingCharacters = {Math.max(0, skippedCharacters)};
        int[] currentColor = {-1};
        component.visit((style, text) -> {
            if (text.isEmpty())
                return Optional.empty();

            int startIndex = Math.min(remainingCharacters[0], text.length());
            remainingCharacters[0] -= startIndex;
            if (startIndex >= text.length())
                return Optional.empty();

            TextColor textColor = style.getColor();
            int rgb = textColor != null ? textColor.getValue() & 0xFFFFFF : -1;
            if (rgb != currentColor[0]) {
                if (rgb < 0) {
                    formattedText.append('\u00A7').append('r');
                } else {
                    appendHexColor(formattedText, rgb);
                }
                currentColor[0] = rgb;
            }

            formattedText.append(text, startIndex, text.length());
            return Optional.empty();
        }, Style.EMPTY);
        return formattedText.toString();
    }

    private static void appendHexColor(StringBuilder output, int rgb) {
        output.append('\u00A7').append('x');
        for (int shift = 20; shift >= 0; shift -= 4) {
            output.append('\u00A7').append(Character.forDigit(rgb >> shift & 0xF, 16));
        }
    }
}
