package com.dioxideliteng.krs.utils.value;

import com.dioxideliteng.krs.utils.ChatUtil;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class SettingValue<T> {
    public final String name;
    public T value;
    public final DisplayableCondition canDisplay;

    /**
     * Set by the DioxideLiteNG settings binder: forwards programmatic changes to the setting shown in the GUI.
     * GUI edits flow the other way through {@link #set(Object)} once per client tick.
     */
    public Consumer<T> sink;

    public SettingValue(String name, T value, DisplayableCondition canDisplay) {
        this.name = name;
        this.value = value;
        this.canDisplay = canDisplay;
    }

    public void set(T newValue) {
        if (Objects.equals(newValue, value)) return;

        T oldValue = get();

        try {
            onChange(oldValue, newValue);
            changeValue(newValue);
            onChanged(oldValue, newValue);
            if (sink != null) sink.accept(this.value);
        } catch (Exception e) {
            ChatUtil.showLog("[ValueSystem (" + name + ")]: " + e.getClass().getName() + " (" + e.getMessage() + ") [" + oldValue + " >> " + newValue + "]");
        }
    }

    public T get() {
        return value;
    }

    protected void changeValue(T value) {
        this.value = value;
    }

    protected void onChange(T oldValue, T newValue) {}

    protected void onChanged(T oldValue, T newValue) {}

    @FunctionalInterface
    public interface DisplayableCondition {
        boolean canDisplay();
    }
}
