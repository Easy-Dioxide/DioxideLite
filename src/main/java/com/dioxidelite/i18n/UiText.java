package com.dioxidelite.i18n;

import com.dioxidelite.DioxideLite;

/**
 * Resolves static UI labels that are drawn directly onto a Skija canvas.
 * <p>
 * Skija text never passes through the vanilla font renderer, so labels rendered this way cannot
 * use {@code Component.translatable}. They resolve through the same
 * {@code DioxideLite.gui.*} language entries instead, keyed by slug and falling back to the
 * original English string when the key is absent, so a missing translation degrades to English
 * rather than blank.
 */
public final class UiText {

    private UiText() {
    }

    public static String tr(String suffix, String fallback) {
        // [DioxideLite 修复] 前缀必须是 NAME("DioxideLite")，不是 MOD_ID("dioxide-lite")；
        // 语言文件全部以 "DioxideLite." 开头，用 MOD_ID 会让所有键静默失配。
        return TranslationKey.of(DioxideLite.NAME + ".gui." + suffix, fallback).get();
    }
}