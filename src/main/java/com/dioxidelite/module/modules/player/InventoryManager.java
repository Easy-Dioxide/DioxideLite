package com.dioxidelite.module.modules.player;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;

/** OpenOnyx naming adapter backed by DioxideLite's fully implemented Inv Manager. */
public final class InventoryManager extends Module {
    public static final InventoryManager INSTANCE = new InventoryManager();
    private InventoryManager() { super("Inventory Manager", Category.PLAYER); }
    @Override protected void onEnable() { if (!InvManager.INSTANCE.isEnabled()) InvManager.INSTANCE.setEnabled(true); }
    @Override protected void onDisable() { if (InvManager.INSTANCE.isEnabled()) InvManager.INSTANCE.setEnabled(false); }
}
