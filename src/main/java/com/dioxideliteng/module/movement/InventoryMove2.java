package com.dioxideliteng.module.movement;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Port of Krs InventoryMove (com.instrumentalist.krs.hacks.features.movement.InventoryMove)
 * to DioxideLiteNG. Logic is a 1:1 copy; only the module-state API is adapted:
 *  - {@code ModuleManager.getModuleState(InventoryMove.class)} -> the resolved instance
 *    check (Scaffold2-style lookup; FeatureManager is owned outside this port);
 *  - Krs settings are static fields; DioxideLiteNG registers settings per instance, so the
 *    statics resolve the single module instance;
 *  - The Krs {@code screen instanceof EmptyScreen} branch (xyz.breadloaf.imguimc) is
 *    dropped: this client has no ImGuiMC screen dependency (documented deviation);
 *  - {@code moveFreely()} (the ClientPlayerEntity#tick HEAD hook in Krs) is preserved for
 *    the mixin wiring; the native InventoryMove already releases keys on ROTATION while a
 *    non-chat screen is open.
 */
public class InventoryMove2 extends Feature {
   private final BooleanSetting inventoryOnly = new BooleanSetting("Inventory Only", this, false);

   public InventoryMove2() {
      super("InventoryMove2", Category.MOVEMENT);
   }

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
      InventoryMove2 module = inst();
      return screen == null
            || module != null && module.isEnabled()
            && (!module.inventoryOnly.getValue() || screen instanceof InventoryScreen)
            && !(screen instanceof ChatScreen);
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEnable() {
   }

   private static InventoryMove2 inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof InventoryMove2 e) return e;
      }
      return null;
   }
}
