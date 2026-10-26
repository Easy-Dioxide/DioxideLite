package com.dioxideliteng.krs.utils.value;

public class TextValue extends SettingValue<String> {
    public TextValue(String name, String value, DisplayableCondition displayable) {
        super(name, value, displayable);
    }

    public TextValue(String name, String value) {
        this(name, value, () -> true);
    }
}