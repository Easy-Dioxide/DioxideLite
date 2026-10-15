package com.dioxideliteng.module.movement;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;

/**
 * Port of Krs NoPush (com.instrumentalist.krs.hacks.features.movement.NoPush) to
 * DioxideLiteNG. The Krs module body is empty (no settings, no events); all logic
 * lives in the Krs mixins:
 *  - ClientPlayerEntityMixin: {@code @Inject moveTowardsClosestSpace(DD)V HEAD}
 *    cancelled while NoPush (or Phase) is enabled;
 *  - EntityMixin: {@code @ModifyArgs} on push(Entity)'s {@code Entity.push(DDD)}
 *    zeroing the x/z arguments for the LocalPlayer while NoPush (or Phase) is enabled.
 *
 * <p>This port preserves the module and exposes the shared condition as
 * {@link #shouldCancelPush()} so the mixin hooks can be attached 1:1 later; the
 * injections themselves are not part of this port (see report).
 */
public class NoPush extends Feature {

   public NoPush() {
      super("No Push", Category.MOVEMENT);
   }

   /**
    * Krs parity: {@code ModuleManager.getModuleState(NoPush.class)} — resolved
    * through the instance lookup (FeatureManager is owned outside this port).
    */
   public static boolean shouldCancelPush() {
      NoPush module = inst();
      return module != null && module.isEnabled();
   }

   private static NoPush inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof NoPush n) return n;
      }
      return null;
   }
}
