package com.dioxidelite.notification;

import com.dioxidelite.ui.UiTheme;

/** Semantic colour used by the in-game notification HUD. */
public enum NotificationType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR;

    /** 动态取色：跟随当前主题的状态色槽。 */
    public int color() {
        return switch (this) {
            case INFO -> UiTheme.info();
            case SUCCESS -> UiTheme.success();
            case WARNING -> UiTheme.warning();
            case ERROR -> UiTheme.danger();
        };
    }
}
