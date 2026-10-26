package com.dioxideliteng.krs.hacks.features.movement;



import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.imguimc.screen.EmptyScreen;

public class InventoryMove extends Module {

    public InventoryMove() {
        super("Inventory Move", ModuleCategory.Movement, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private static final BooleanValue inventoryOnly = new BooleanValue("Inventory Only", false);

    public static void moveFreely() {
        if (canMoveFreely() && mc.gui.screen() != null) {
            KeyMapping.setAll();
            KeyMapping sneakKey = mc.options.keyShift;
            sneakKey.setDown(false);
        }
    }

    public static boolean canMoveFreely() {
        return canMoveFreely(mc.gui.screen());
    }

    public static boolean canMoveFreely(Screen screen) {
        return screen == null
                || screen instanceof EmptyScreen
                || ModuleManager.getModuleState(InventoryMove.class)
                && (!inventoryOnly.get() || screen instanceof InventoryScreen)
                && !(screen instanceof ChatScreen);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
