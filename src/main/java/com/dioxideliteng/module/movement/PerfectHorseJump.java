package com.dioxideliteng.module.movement;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;

/**
 * Port of Krs PerfectHorseJump (com.instrumentalist.krs.hacks.features.movement.PerfectHorseJump)
 * to DioxideLiteNG. Logic is a 1:1 copy; only the module-state API is adapted:
 *  - {@code ModuleManager.getModuleState(PerfectHorseJump.class)} -> the resolved
 *    instance check (Scaffold2-style lookup; FeatureManager is owned outside this port).
 *
 * <p>Krs calls {@link #modifiedHorseJump(float)} from the {@code getJumpRidingScale}
 * ModifyReturnValue hook in ClientPlayerEntityMixin; the injection itself is not
 * part of this port (see report).
 */
public class PerfectHorseJump extends Feature {

   public PerfectHorseJump() {
      super("Perfect Horse Jump", Category.MOVEMENT);
   }

   public static float modifiedHorseJump(float original) {
      PerfectHorseJump module = inst();
      if (module != null && module.isEnabled())
         return 1f;

      return original;
   }

   private static PerfectHorseJump inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof PerfectHorseJump p) return p;
      }
      return null;
   }
}
