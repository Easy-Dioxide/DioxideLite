package com.dioxideliteng.krs.hacks;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * DioxideLiteNG {@link Feature} that hosts one ported KRS {@link Module}.
 *
 * <p>Everything DioxideLiteNG offers for its own modules (Click GUI, key binds, config save/load, array list, island
 * toggle notices) works through this wrapper. KRS listener methods are driven by
 * {@link com.dioxideliteng.krs.KrsBridge}, not by {@link #onEvent}.
 */
public final class KrsFeature extends Feature {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Module module;
    private final List<KrsSettings.Binding> bindings;
    private boolean enableFailed;

    public KrsFeature(Module module, String name) {
        super(name, 0, categoryOf(module.moduleCategory));
        this.module = module;
        module.attach(this);
        this.bindings = KrsSettings.bind(module, this);
    }

    public Module module() {
        return module;
    }

    static Category categoryOf(ModuleCategory category) {
        return switch (category) {
            case Combat -> Category.COMBAT;
            case Movement -> Category.MOVEMENT;
            case Player -> Category.PLAYER;
            case Render -> Category.VISUAL;
            case Level, Exploit, Dev -> Category.MISC;
        };
    }

    @Override
    public void clientTick() {
        for (KrsSettings.Binding binding : bindings) {
            try {
                binding.pull();
            } catch (RuntimeException failure) {
                LOGGER.warn("Failed to read setting of {}", getName(), failure);
            }
        }
        String tag = module.tag();
        setSuffix(tag == null ? "" : tag);
    }

    @Override
    public void toggle() {
        boolean enabling = !isEnabled();
        // KRS contract: the state flag is already correct while onEnable()/onDisable() run.
        module.tempEnabled = enabling;
        enableFailed = false;
        super.toggle();
        if (enableFailed && isEnabled()) {
            // onEnable() threw: roll back exactly like KRS did (runs onDisable() for cleanup).
            enableFailed = false;
            module.tempEnabled = false;
            super.toggle();
        }
        module.tempEnabled = isEnabled();
    }

    @Override
    public void restoreEnabled(boolean desired) {
        super.restoreEnabled(desired);
        module.tempEnabled = isEnabled();
    }

    @Override
    public void onEnable() {
        try {
            module.onEnable();
        } catch (RuntimeException | LinkageError failure) {
            enableFailed = true;
            LOGGER.error("Failed to enable {}", getName(), failure);
        }
    }

    @Override
    public void onDisable() {
        try {
            module.onDisable();
        } catch (RuntimeException | LinkageError failure) {
            LOGGER.error("Failed to disable {}", getName(), failure);
        }
    }
}
