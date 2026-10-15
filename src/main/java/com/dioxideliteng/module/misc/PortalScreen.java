package com.dioxideliteng.module.misc;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.PortalScreen).
 * Krs parity: the module owns no event handlers; its static screenOpenLimitHook is the
 * integration point called from the Krs ClientPlayerEntityMixin screen hook. The hook is
 * ported as-is (backed by the registered instance, the equivalent of
 * ModuleManager.getModuleState) so the screen hook can be wired without further changes.
 */
public class PortalScreen extends Feature {

   private static PortalScreen instance;

   public PortalScreen() {
      super("Portal Screen", Category.MISC);
      instance = this;
   }

   public static boolean screenOpenLimitHook(Boolean original) {
      if (instance != null && instance.isEnabled())
         return true;

      return original;
   }
}
