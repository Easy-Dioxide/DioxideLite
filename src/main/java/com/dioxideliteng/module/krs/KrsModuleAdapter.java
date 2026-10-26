package com.dioxideliteng.module.krs;

import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.ColorValue;
import com.dioxideliteng.krs.utils.value.FloatValue;
import com.dioxideliteng.krs.utils.value.IntValue;
import com.dioxideliteng.krs.utils.value.KeyBindValue;
import com.dioxideliteng.krs.utils.value.ListValue;
import com.dioxideliteng.krs.utils.value.SettingValue;
import com.dioxideliteng.krs.utils.value.TextValue;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.setting.Setting;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 把一个 KRS {@link Module} 包装成 DioxideLite NG 的 {@link Feature}。
 *
 * <p><b>功能不变</b>：模块逻辑、事件监听、绕过行为全部沿用 KRS 原实现，
 * 本类只负责「挂载」——让 NG 的 ClickGui 能列出这个模块、开关它、改它的参数。</p>
 *
 * <p><b>参数双向同步</b>：KRS 的 {@code SettingValue} 与 NG 的 {@code Setting} 语义不同，
 * 这里为每个 KRS 设置项建一个 NG 设置项，双向同步：
 * <ul>
 *   <li>NG 改动 → 写回 KRS 的 {@code SettingValue.set(...)}，触发它自己的 onChange 逻辑；</li>
 *   <li>KRS 内部改动（如模块自己改模式）→ 在下一次 {@link #clientTick()} 时回写到 NG。</li>
 * </ul>
 * 这样界面显示与实际生效值永远一致。</p>
 */
public class KrsModuleAdapter extends Feature {

    private final Module module;
    /** KRS 设置项与 NG 设置项的配对，用于回写。 */
    private final List<Binding> bindings = new ArrayList<>();
    private final List<Field> settingFields;

    public KrsModuleAdapter(Module module, String featureName) {
        super(featureName, toCategory(module.moduleCategory));
        this.module = module;
        this.settingFields = collectSettingFields(module.getClass());
        registerSettings();
        // KRS 模块默认全部关闭，开关状态完全交给 NG 管理。
        this.module.tempEnabled = false;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public void onEnable() {
        module.setState(true);
    }

    @Override
    public void onDisable() {
        module.setState(false);
    }

    /**
     * 每 tick 双向同步一次：
     * 先把 NG 界面上的改动推给 KRS，再把 KRS 内部改动的值拉回界面。
     * NG 的设置类没有变更回调，所以用「快照比对」判断哪一侧变了，开销仅几个字段比较。
     */
    @Override
    public void clientTick() {
        if (bindings.isEmpty()) {
            return;
        }
        for (Binding binding : bindings) {
            binding.pushToKrsIfChanged();
        }
        if (!isEnabled()) {
            return;
        }
        for (Binding binding : bindings) {
            binding.pullFromKrs();
        }
    }

    @Override
    public void setKey(int key) {
        // KRS 自己维护按键表，NG 的按键绑定仍然生效（FeatureManager 会调用 toggle）。
        super.setKey(key);
        module.key = key;
    }

    /**
     * KRS 分类 → NG 分类。
     *
     * <p>注意 KRS 的 {@code ModuleCategory} 枚举常量是首字母大写写法
     * （{@code Combat} / {@code Movement} / {@code Render} …），不是全大写，
     * 这里按枚举对象本身匹配，不用字符串比较，避免大小写不一致导致
     * 所有模块静默落进 MISC。</p>
     */
    private static Category toCategory(com.dioxideliteng.krs.hacks.ModuleCategory krsCategory) {
        if (krsCategory == null) {
            return Category.MISC;
        }
        return switch (krsCategory) {
            case Combat -> Category.COMBAT;
            case Movement -> Category.MOVEMENT;
            case Player -> Category.PLAYER;
            case Render -> Category.VISUAL;
            // Level / Exploit / Dev 以及未来新增的分类统一归入 MISC，
            // 显式写 default 是因为穷举 switch 在枚举扩充后会抛 MatchException。
            default -> Category.MISC;
        };
    }

    /** KRS 用 {@code @Module.Setting} 标注字段，这里按同一规则收集。 */
    private static List<Field> collectSettingFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Module.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Module.Setting.class) && SettingValue.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private void registerSettings() {
        for (Field field : settingFields) {
            SettingValue<?> value;
            try {
                value = (SettingValue<?>) field.get(module);
            } catch (IllegalAccessException ignored) {
                continue;
            }
            if (value == null) {
                continue;
            }
            Setting bridged = bridge(value);
            if (bridged != null) {
                bindings.add(new Binding(value, bridged));
            }
        }
    }

    /**
     * 把一个 KRS 设置项映射为 NG 设置项。返回 null 表示 NG 的 ClickGui 无法编辑该类型
     * （例如颜色选择器），此时仅保留在 KRS 内部，不影响模块功能。
     */
    private Setting bridge(SettingValue<?> value) {
        String label = value.name;
        if (value instanceof BooleanValue boolValue) {
            BooleanSetting setting = new BooleanSetting(label, this, Boolean.TRUE.equals(boolValue.get()));
            return setting;
        }

        if (value instanceof FloatValue floatValue) {
            double step = (floatValue.maximum - floatValue.minimum) > 10f ? 0.1 : 0.01;
            NumberSetting setting = new NumberSetting(
                    label, this,
                    clampToRange(floatValue.get(), floatValue.minimum, floatValue.maximum),
                    floatValue.minimum, floatValue.maximum, step);
            return setting;
        }

        if (value instanceof IntValue intValue) {
            NumberSetting setting = new NumberSetting(
                    label, this,
                    clampToRange(intValue.get(), intValue.minimum, intValue.maximum),
                    intValue.minimum, intValue.maximum, 1.0);
            return setting;
        }

        if (value instanceof ListValue listValue) {
            String[] options = listValue.values == null || listValue.values.length == 0
                    ? new String[] {String.valueOf(listValue.get())}
                    : listValue.values;
            String current = String.valueOf(listValue.get());
            boolean known = false;
            for (String option : options) {
                if (option.equals(current)) {
                    known = true;
                    break;
                }
            }
            ModeSetting setting = new ModeSetting(label, this, known ? current : options[0], options);
            return setting;
        }

        if (value instanceof KeyBindValue || value instanceof TextValue || value instanceof ColorValue) {
            // NG 的 ClickGui 没有对应的编辑器类型，保持只读，功能不受影响。
            return null;
        }

        // 未知类型：退化为只读的布尔占位会误导用户，直接跳过。
        return null;
    }

    private static double clampToRange(Number number, double min, double max) {
        double raw = number == null ? min : number.doubleValue();
        return Math.max(min, Math.min(max, raw));
    }

    /**
     * 桥接后的显示名。KRS 内置按「是否是当前生效的模式」显示的副标题在这里不适用，
     * 因为副标题由子模式模块自己维护，所以为空。
     */
    @Override
    public String getSuffix() {
        return "";
    }

    /** 一个 KRS 设置项与 NG 设置项的配对。 */
    private final class Binding {
        private final SettingValue<?> krs;
        private final Setting ng;
        private Object lastPushed;
        private Object lastPulled;

        private Binding(SettingValue<?> krs, Setting ng) {
            this.krs = krs;
            this.ng = ng;
            this.lastPushed = krs.get();
            this.lastPulled = krs.get();
        }

        /** NG → KRS：界面改动写回 KRS，触发它自己的变更回调。 */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void pushToKrsIfChanged() {
            Object current = krs.get();
            Object ngValue = readNgValue();
            if (ngValue == null || ngValue.equals(lastPushed)) {
                return;
            }
            lastPushed = ngValue;

            if (krs instanceof ListValue listValue) {
                int index = indexOf(listValue, (String) ngValue);
                if (index != listValue.getCurrentIndex()) {
                    listValue.setByIndex(index);
                }
            } else if (krs instanceof IntValue) {
                int desired = (int) Math.round(((Number) ngValue).doubleValue());
                if (!Integer.valueOf(desired).equals(current)) {
                    ((SettingValue) krs).set(desired);
                }
            } else if (krs instanceof FloatValue) {
                float desired = ((Number) ngValue).floatValue();
                if (!Float.valueOf(desired).equals(current)) {
                    ((SettingValue) krs).set(desired);
                }
            } else if (krs instanceof BooleanValue) {
                if (!ngValue.equals(current)) {
                    ((SettingValue) krs).set(ngValue);
                }
            }
            lastPulled = krs.get();
        }

        /** 读取 NG 设置项当前的值；类型不支持时返回 null。 */
        private Object readNgValue() {
            if (ng instanceof BooleanSetting bool) {
                return bool.getValue();
            }
            if (ng instanceof NumberSetting number) {
                return number.getValue();
            }
            if (ng instanceof ModeSetting mode) {
                return mode.getValue();
            }
            return null;
        }

        /** KRS → NG：模块内部改了值，界面跟着变。 */
        private void pullFromKrs() {
            Object current = krs.get();
            if (current == null || current.equals(lastPulled)) {
                return;
            }
            lastPulled = current;
            if (ng instanceof BooleanSetting bool && current instanceof Boolean b) {
                bool.setValue(b);
            } else if (ng instanceof NumberSetting number && current instanceof Number n) {
                number.setValue(n.doubleValue());
            } else if (ng instanceof ModeSetting mode && krs instanceof ListValue listValue) {
                int index = listValue.getCurrentIndex();
                String[] options = mode.getOptions();
                if (index >= 0 && index < options.length) {
                    mode.setValue(options[index]);
                }
            }
        }

        private int indexOf(ListValue listValue, String option) {
            String[] options = listValue.values;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(option)) {
                    return i;
                }
            }
            return 0;
        }
    }

    /** 供桥接层在 NG 侧修改参数前调用：把 NG 的改动下推给 KRS。 */
    public void flushSettings() {
        for (Binding binding : bindings) {
            binding.pushToKrsIfChanged();
        }
    }
}
