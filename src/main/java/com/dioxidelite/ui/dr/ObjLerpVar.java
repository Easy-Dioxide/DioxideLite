package com.dioxidelite.ui.dr;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * obj_lerpvar 的转写（scr_lerpvar / scr_lerp_instance_var / scr_lerpvar_instance 都创建它）。
 * Step：init==0 时若 pointa 是变量名（GML 的 is_string 分支）就读目标当前值；
 * time++ 后按 easetype（0 线性 / 其余走 easeinout 指定的 scr_ease_* 曲线）写回目标变量；
 * time >= maxtime 自毁。
 *
 * 转写用 setter/reader 绑定目标变量（variable_instance_set/get 的类型安全等价物）。
 */
final class ObjLerpVar {
    private final DoubleConsumer setter;
    private final DoubleSupplier reader;   // 非空 = pointa 传的是变量名（读当前值）
    private final double pointaIn;
    private final double pointb;
    private final int maxtime;
    private final int easetype;
    private final String easeinout;
    private int time;
    private int init;
    private double pointa;
    boolean dead;

    ObjLerpVar(DoubleConsumer setter, DoubleSupplier reader, double pointa, double pointb,
               int maxtime, int easetype, String easeinout) {
        this.setter = setter;
        this.reader = reader;
        this.pointaIn = pointa;
        this.pointb = pointb;
        this.maxtime = maxtime;
        this.easetype = easetype;
        this.easeinout = easeinout;
    }

    void step() {
        if (init == 0) {
            pointa = reader != null ? reader.getAsDouble() : pointaIn;
            init = 1;
        }
        time++;
        double t = time / (double) maxtime;
        double v;
        if (easetype == 0) {
            v = pointa + (pointb - pointa) * t;
        } else if ("out".equals(easeinout)) {
            v = pointa + (pointb - pointa) * Ease.out(t, easetype);
        } else if ("in".equals(easeinout)) {
            v = pointa + (pointb - pointa) * Ease.in(t, easetype);
        } else if ("inout".equals(easeinout)) {
            v = pointa + (pointb - pointa) * Ease.inout(t, easetype);
        } else {
            v = pointa + (pointb - pointa) * t;
        }
        setter.accept(v);
        if (time >= maxtime) {
            dead = true;
        }
    }
}
