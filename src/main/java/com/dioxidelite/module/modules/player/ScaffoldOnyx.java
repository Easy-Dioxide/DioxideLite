package com.dioxidelite.module.modules.player;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.modules.movement.Scaffold;

/** OpenOnyx Player-category Scaffold entry; delegates to the canonical Scaffold engine. */
public final class ScaffoldOnyx extends Module {
    public static final ScaffoldOnyx INSTANCE = new ScaffoldOnyx();
    private ScaffoldOnyx() { super("Scaffold (Onyx)", Category.PLAYER); }
    @Override protected void onEnable() { if (!Scaffold.INSTANCE.isEnabled()) Scaffold.INSTANCE.setEnabled(true); }
    @Override protected void onDisable() { if (Scaffold.INSTANCE.isEnabled()) Scaffold.INSTANCE.setEnabled(false); }
}
