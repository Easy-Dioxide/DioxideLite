package com.dioxidelite.ui.dr;

/**
 * scr_ease_in / scr_ease_out / scr_ease_inout 的逐行转写（GML 的 switch 全套）。
 */
public final class Ease {
    private Ease() {
    }

    public static double in(double t, int type) {
        if (type < -3 || type > 7) {
            return t;
        }
        switch (type) {
            case -3:
            case -2:
                return t;
            case -1: {
                double s = 1.70158;
                return t * t * (((s + 1) * t) - s);
            }
            case 0:
                return t;
            case 1:
                return -Math.cos(t * 1.5707963267948966) + 1;
            case 6:
                return Math.pow(2, 10 * (t - 1));
            case 7:
                return -(Math.sqrt(1 - t * t) - 1);
            default:
                return Math.pow(t, type);
        }
    }

    public static double out(double t, int type) {
        if (type < -3 || type > 7) {
            return t;
        }
        switch (type) {
            case -3:
            case -2:
                return t;
            case -1: {
                // ease_out_back(t, 0, 1, 1)
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
            }
            case 0:
                return t;
            case 1:
                return Math.sin(t * 1.5707963267948966);
            case 2:
                return -t * (t - 2);
            case 6:
                return -Math.pow(2, -10 * t) + 1;
            case 7:
                t--;
                return Math.sqrt(1 - (t * t));
            default: {
                t--;
                if (type == 4) {
                    return -1 * (Math.pow(t, type) - 1);
                }
                return Math.pow(t, type) + 1;
            }
        }
    }

    public static double inout(double t, int type) {
        if (type < -3 || type > 7) {
            return t;
        }
        if (type == -3 || type == -2 || type == -1) {
            return t;
        }
        if (type == 1) {
            return -0.5 * Math.cos((Math.PI * t) - 1);
        }
        if (type == 0) {
            return t;
        }
        t *= 2;
        if (t < 1) {
            return 0.5 * in(t, type);
        }
        t--;
        return 0.5 * (out(t, type) + 1);
    }
}
