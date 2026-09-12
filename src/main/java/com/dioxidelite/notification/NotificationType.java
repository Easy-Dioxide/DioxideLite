package com.dioxidelite.notification;

import com.dioxidelite.ui.UiTheme;

/** Semantic colour used by the in-game notification HUD. */
public enum NotificationType {
    INFO(UiTheme.INFO),
    SUCCESS(UiTheme.SUCCESS),
    WARNING(UiTheme.WARNING),
    ERROR(UiTheme.DANGER);

    private final int color;

    NotificationType(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }
}
