package com.dioxidelite.ui.dr;

import java.util.function.DoubleConsumer;

/**
 * obj_script_delayed + scr_var 的转写（scr_delay_var / scr_var_delay）：
 * alarm[0]=delay 帧后把目标变量设为 value，然后自毁。
 */
final class ObjScriptDelayed {
    private final DoubleConsumer setter;
    private final double value;
    private int alarm;
    boolean dead;

    ObjScriptDelayed(DoubleConsumer setter, double value, int delay) {
        this.setter = setter;
        this.value = value;
        this.alarm = delay;
    }

    void step() {
        if (alarm >= 0) {
            alarm--;
            if (alarm < 0) {
                setter.accept(value);
                dead = true;
            }
        }
    }
}
