package com.dioxideliteng.krs.utils.value;

import java.awt.Color;

public class ColorValue extends SettingValue<Color> {
    public ColorValue(String name, Color value, DisplayableCondition displayable) {
        super(name, value, displayable);
    }

    public ColorValue(String name, Color value) {
        this(name, value, () -> true);
    }

    public float[] getHSB() {
        return Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), null);
    }

    public void setHSB(float hue, float saturation, float brightness) {
        Color rgb = new Color(Color.HSBtoRGB(
                Math.min(Math.max(hue, 0f), 1f),
                Math.min(Math.max(saturation, 0f), 1f),
                Math.min(Math.max(brightness, 0f), 1f)
        ));
        set(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), value.getAlpha()));
    }

    public String toHex() {
        char[] hex = new char[9];
        hex[0] = '#';
        appendHexByte(hex, 1, value.getRed());
        appendHexByte(hex, 3, value.getGreen());
        appendHexByte(hex, 5, value.getBlue());
        appendHexByte(hex, 7, value.getAlpha());
        return new String(hex);
    }

    private static void appendHexByte(char[] target, int offset, int value) {
        target[offset] = Character.forDigit((value >>> 4) & 0xF, 16);
        target[offset + 1] = Character.forDigit(value & 0xF, 16);
    }

    public void fromHex(String hex) {
        if (hex == null) return;

        try {
            hex = hex.trim().replace("#", "");
            if (hex.length() == 6) {
                hex = hex + "ff";
            }
            if (hex.length() != 8) return;

            long color = Long.parseLong(hex, 16);
            set(new Color(
                    (int) ((color >> 24) & 0xFF),
                    (int) ((color >> 16) & 0xFF),
                    (int) ((color >> 8) & 0xFF),
                    (int) (color & 0xFF)
            ));
        } catch (Exception e) {
            // Invalid hex color, keep current value
        }
    }
}
