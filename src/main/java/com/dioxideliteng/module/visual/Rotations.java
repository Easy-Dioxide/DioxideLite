package com.dioxideliteng.module.visual;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;

/**
 * Port of Krs Rotations (com.instrumentalist.krs.hacks.features.render.Rotations).
 * Logic is a 1:1 copy; only the module/value API is adapted:
 *  - Krs {@code ModuleCategory.Render} maps onto {@link Category#VISUAL};
 *  - Krs {@code ModuleManager.getModuleState(Rotations.class)} maps onto the
 *    registered-instance check (Scaffold2-style lookup);
 *  - Krs constructs the module with {@code tempEnabled = true}, so the port
 *    enables itself on construction (persisted config states still win).
 *
 * <p>The module is the render-side switch of the silent-aim rotation system:
 * while the RotationManager is rotating, the local avatar is rendered with the
 * spoofed rotation. "Vanilla" on feeds the interpolated values through vanilla
 * state extraction (see MixinLivingEntityRenderer); "Vanilla" off overwrites
 * the render state directly at the AvatarRenderer tail (see MixinAvatarRenderer).
 */
public class Rotations extends Feature {
   private final BooleanSetting vanilla = new BooleanSetting("Vanilla", this, true);

   public Rotations() {
      super("Rotations", Category.VISUAL);
      // Krs parity: the module ships enabled by default (tempEnabled = true).
      setEnabled(true);
   }

   public boolean isVanilla() {
      return this.vanilla.getValue();
   }

   /** Krs Rotations.shouldUseVanilla(): module enabled && "Vanilla" setting. */
   public static boolean shouldUseVanilla() {
      Rotations module = inst();
      return module != null && module.isEnabled() && module.isVanilla();
   }

   /** Krs ModuleManager.getModuleState(Rotations.class). */
   public static boolean isModuleEnabled() {
      Rotations module = inst();
      return module != null && module.isEnabled();
   }

   private static Rotations inst() {
      for (var feature : FeatureManager.getModules()) {
         if (feature instanceof Rotations rotations) return rotations;
      }
      return null;
   }
}
