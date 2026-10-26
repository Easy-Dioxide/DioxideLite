package com.dioxideliteng.krs.utils.value;

public class ListValue extends SettingValue<String> {
    public final String[] values;

    private int currentIndex;

    public ListValue(String name, String[] values, String value, DisplayableCondition displayable) {
        super(name, value, displayable);
        this.values = values != null ? values.clone() : new String[0];
        this.currentIndex = findIndex(this.value);
        if (this.currentIndex < 0 && this.values.length > 0) {
            this.currentIndex = 0;
            super.changeValue(this.values[0]);
        }
    }

    public ListValue(String name, String[] values, String value) {
        this(name, values, value, () -> true);
    }

    public boolean contains(String string) {
        return findIndex(string) >= 0;
    }

    @Override
    protected void changeValue(String value) {
        int index = findIndex(value);
        if (index < 0) return;

        super.changeValue(values[index]);
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setByIndex(int index) {
        if (index >= 0 && index < values.length) {
            set(values[index]);
        }
    }

    public void nextValue() {
        if (values.length == 0) return;

        int index = (currentIndex + 1) % values.length;
        setByIndex(index);
    }

    public void previousValue() {
        if (values.length == 0) return;

        int index = (currentIndex - 1 + values.length) % values.length;
        setByIndex(index);
    }

    private int findIndex(String value) {
        if (value == null) return -1;

        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(value))
                return i;
        }

        return -1;
    }
}
