package com.dioxideliteng.krs.utils.value;

public class KeyBindValue extends SettingValue<Integer> {
    public enum ConfigStorage {
        MODULE_CONFIG,
        BIND_CONFIG
    }

    private ChangeListener changeListener;
    private final ConfigStorage configStorage;

    public KeyBindValue(String name, int value, DisplayableCondition displayable) {
        this(name, value, displayable, ConfigStorage.MODULE_CONFIG);
    }

    public KeyBindValue(String name, int value) {
        this(name, value, () -> true);
    }

    public KeyBindValue(String name, int value, ConfigStorage configStorage) {
        this(name, value, () -> true, configStorage);
    }

    public KeyBindValue(String name, int value, DisplayableCondition displayable, ConfigStorage configStorage) {
        super(name, value, displayable);
        this.configStorage = configStorage != null ? configStorage : ConfigStorage.MODULE_CONFIG;
    }

    public ConfigStorage configStorage() {
        return configStorage;
    }

    @Override
    protected void onChanged(Integer oldValue, Integer newValue) {
        super.onChanged(oldValue, newValue);
        if (changeListener != null)
            changeListener.onChange(newValue);
    }

    public interface ChangeListener {
        void onChange(int newValue);
    }
}
