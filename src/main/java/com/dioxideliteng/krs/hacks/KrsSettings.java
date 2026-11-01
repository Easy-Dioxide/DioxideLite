package com.dioxideliteng.krs.hacks;

import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.ColorValue;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.krs.utils.value.IntValue;
import com.dioxideliteng.krs.utils.value.ListValue;
import com.dioxideliteng.krs.utils.value.SettingValue;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.setting.Setting;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps the KRS value system ({@code @Module.Setting} fields) onto the setting types every DioxideLiteNG click GUI,
 * config file and preset can already draw and persist: {@link BooleanSetting}, {@link NumberSetting} and
 * {@link ModeSetting}.
 *
 * <p>The two sides are kept in sync without listeners (DioxideLiteNG settings have none):
 * programmatic changes made by module code reach the GUI immediately through {@link SettingValue#sink};
 * GUI edits are read back once per client tick through {@link Binding#pull()}.
 * {@code TextValue} and {@code KeyBindValue} have no DioxideLiteNG counterpart and stay module-internal.
 */
final class KrsSettings {
    private KrsSettings() {
    }

    /** Reads the GUI-side value and applies it to the KRS value if the user changed it. */
    @FunctionalInterface
    interface Binding {
        void pull();
    }

    static List<Binding> bind(Module module, Feature feature) {
        List<Binding> bindings = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        // Superclass first so shared base-class settings keep their natural position.
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> type = module.getClass(); type != null && type != Module.class && type != Object.class; type = type.getSuperclass()) {
            chain.add(0, type);
        }

        for (Class<?> type : chain) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Module.Setting.class)) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(module);
                    if (value instanceof SettingValue<?> settingValue) {
                        bindValue(settingValue, feature, usedNames, bindings);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // A setting that cannot be bound simply stays module-internal.
                }
            }
        }
        return bindings;
    }

    private static String unique(Set<String> used, String wanted) {
        String name = wanted;
        for (int i = 2; !used.add(name); i++) {
            name = wanted + " " + i;
        }
        return name;
    }

    private static <S extends Setting> S visibleWhen(S setting, SettingValue<?> value) {
        if (value.canDisplay != null) {
            setting.setVisible(value.canDisplay::canDisplay);
        }
        return setting;
    }

    private static void bindValue(SettingValue<?> value, Feature feature, Set<String> used, List<Binding> out) {
        if (value instanceof BooleanValue booleanValue) {
            BooleanSetting setting = visibleWhen(new BooleanSetting(unique(used, value.name), feature, booleanValue.value), value);
            booleanValue.sink = setting::setValue;
            out.add(() -> {
                boolean gui = setting.getValue();
                if (booleanValue.value != gui) booleanValue.set(gui);
            });
        } else if (value instanceof IntValue intValue) {
            NumberSetting setting = visibleWhen(new NumberSetting(unique(used, value.name), feature,
                intValue.value, intValue.minimum, intValue.maximum, 1.0), value);
            double[] synced = {setting.getValue()};
            intValue.sink = v -> {
                setting.setValue(v);
                synced[0] = setting.getValue();
            };
            out.add(() -> {
                double gui = setting.getValue();
                if (gui != synced[0]) {
                    synced[0] = gui;
                    intValue.set((int) Math.round(gui));
                }
            });
        } else if (value instanceof FloatValue floatValue) {
            double range = Math.abs((double) floatValue.maximum - floatValue.minimum);
            double step = range >= 60.0 ? 0.5 : range >= 8.0 ? 0.1 : 0.01;
            NumberSetting setting = visibleWhen(new NumberSetting(unique(used, value.name), feature,
                floatValue.value, floatValue.minimum, floatValue.maximum, step), value);
            double[] synced = {setting.getValue()};
            floatValue.sink = v -> {
                setting.setValue(v);
                synced[0] = setting.getValue();
            };
            out.add(() -> {
                double gui = setting.getValue();
                if (gui != synced[0]) {
                    synced[0] = gui;
                    floatValue.set((float) gui);
                }
            });
        } else if (value instanceof ListValue listValue) {
            if (listValue.values.length == 0) return;
            ModeSetting setting = visibleWhen(new ModeSetting(unique(used, value.name), feature, listValue.value, listValue.values), value);
            listValue.sink = v -> {
                try {
                    setting.setValue(v);
                } catch (IllegalArgumentException ignored) {
                    // value is not one of the options; keep the GUI as it was
                }
            };
            out.add(() -> {
                String gui = setting.getValue();
                if (!gui.equals(listValue.value)) listValue.set(gui);
            });
        } else if (value instanceof ColorValue colorValue) {
            bindColor(colorValue, feature, used, out);
        }
        // TextValue / KeyBindValue: no DioxideLiteNG equivalent, intentionally left module-internal.
    }

    private static void bindColor(ColorValue colorValue, Feature feature, Set<String> used, List<Binding> out) {
        Color start = colorValue.value;
        boolean alpha = start.getAlpha() != 255;
        String base = colorValue.name;
        NumberSetting red = visibleWhen(new NumberSetting(unique(used, base + " Red"), feature, start.getRed(), 0, 255, 1.0), colorValue);
        NumberSetting green = visibleWhen(new NumberSetting(unique(used, base + " Green"), feature, start.getGreen(), 0, 255, 1.0), colorValue);
        NumberSetting blue = visibleWhen(new NumberSetting(unique(used, base + " Blue"), feature, start.getBlue(), 0, 255, 1.0), colorValue);
        NumberSetting opacity = alpha
            ? visibleWhen(new NumberSetting(unique(used, base + " Alpha"), feature, start.getAlpha(), 0, 255, 1.0), colorValue)
            : null;

        int[] synced = {packed(red, green, blue, opacity, start)};
        colorValue.sink = c -> {
            red.setValue(c.getRed());
            green.setValue(c.getGreen());
            blue.setValue(c.getBlue());
            if (opacity != null) opacity.setValue(c.getAlpha());
            synced[0] = packed(red, green, blue, opacity, c);
        };
        out.add(() -> {
            int gui = packed(red, green, blue, opacity, colorValue.value);
            if (gui != synced[0]) {
                synced[0] = gui;
                colorValue.set(new Color(gui >> 16 & 0xFF, gui >> 8 & 0xFF, gui & 0xFF, gui >>> 24));
            }
        });
    }

    private static int packed(NumberSetting r, NumberSetting g, NumberSetting b, NumberSetting a, Color fallback) {
        int alpha = a == null ? fallback.getAlpha() : (int) Math.round(a.getValue());
        return (alpha & 0xFF) << 24 | ((int) Math.round(r.getValue()) & 0xFF) << 16
            | ((int) Math.round(g.getValue()) & 0xFF) << 8 | ((int) Math.round(b.getValue()) & 0xFF);
    }
}
