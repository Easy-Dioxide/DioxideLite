package com.dioxideliteng.krs.hacks;

import com.dioxideliteng.krs.events.EventListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compatibility base class for ported KRS modules.
 *
 * <p>KRS modules keep their original shape (constructor, {@code @Setting} value fields, {@code onXxx} listener methods).
 * Registration, enabling, key binds, settings UI and persistence are provided by DioxideLiteNG through the
 * {@link KrsFeature} that wraps each module. This class intentionally does <em>not</em> extend
 * {@link com.dioxideliteng.module.Feature}: KRS code reaches {@code mc} through {@code IMinecraft}, which would be an
 * ambiguous simple name next to {@code Feature.mc}.
 */
public abstract class Module implements EventListener {
    public final String moduleName;
    public final ModuleCategory moduleCategory;
    public int key;
    /** Mirrors the wrapping feature's enabled flag; it is already updated while {@link #onEnable()} runs. */
    public volatile boolean tempEnabled;
    public boolean showOnArray;

    private KrsFeature feature;

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Setting {
    }

    public Module(String moduleName, ModuleCategory moduleCategory, int key, boolean tempEnabled, boolean showOnArray) {
        this.moduleName = moduleName;
        this.moduleCategory = moduleCategory;
        this.key = key;
        this.tempEnabled = tempEnabled;
        this.showOnArray = showOnArray;
    }

    final void attach(KrsFeature feature) {
        this.feature = feature;
    }

    /** The DioxideLiteNG feature wrapping this module, or {@code null} before registration. */
    public final KrsFeature feature() {
        return feature;
    }

    /** Name shown in the Click GUI and config; differs from {@link #moduleName} when a DioxideLiteNG module owns it (suffix "2"). */
    public final String displayName() {
        return feature == null ? moduleName : feature.getName();
    }

    public synchronized void setState(boolean state) {
        if (feature == null) {
            this.tempEnabled = state;
            return;
        }
        feature.setEnabled(state);
    }

    public synchronized void toggle() {
        if (feature == null) {
            this.tempEnabled = !this.tempEnabled;
            return;
        }
        feature.toggle();
    }

    public String description() {
        return null;
    }

    public String tag() {
        return null;
    }

    public abstract void onEnable();

    public abstract void onDisable();
}
