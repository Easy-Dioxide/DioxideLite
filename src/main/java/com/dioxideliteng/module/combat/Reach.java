package com.dioxideliteng.module.combat;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Reach).
 * The module itself has no event handlers: its whole behavior lives in the
 * static reach hooks, called from mixins at Player#blockInteractionRange and
 * Player#entityInteractionRange (see mixins.MixinPlayer), exactly like the
 * Krs PlayerEntityMixin wiring.
 */
public class Reach extends Feature {
   private static Reach instance;

   private final BooleanSetting block = new BooleanSetting("Block", this, true);
   private final NumberSetting blockReach = new NumberSetting("Block Reach", this, 6.0, 0.1, 6.0, 0.1);
   private final BooleanSetting entity = new BooleanSetting("Entity", this, true);
   private final NumberSetting entityReach = new NumberSetting("Entity Reach", this, 6.0, 0.1, 6.0, 0.1);

   public Reach() {
      super("Reach", Category.COMBAT);
      instance = this;
      this.blockReach.setVisible(this.block::getValue);
      this.entityReach.setVisible(this.entity::getValue);
   }

   public static double hookBlockReach(double original) {
      Reach module = instance;
      if (module == null || !module.isEnabled() || !module.block.getValue())
         return original;

      return module.blockReach.getValue();
   }

   public static double hookEntityReach(double original) {
      Reach module = instance;
      if (module == null || !module.isEnabled() || !module.entity.getValue())
         return original;

      return module.entityReach.getValue();
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }
}
