package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class TriggerBot extends Feature {
   private static final String TRIGGER_BOT_LABEL = "TriggerBot";

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         if (mc.player == null || mc.level == null || mc.gameMode == null
             || mc.gui.screen() != null || !mc.isWindowActive()) return;
         Entity entity = mc.crosshairPickEntity;
         if (FeatureManager.targets.shouldAttack(entity) && mc.player != null && mc.player.isAlive() && mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
         }
      }
   }

   public TriggerBot() {
      super(TRIGGER_BOT_LABEL, Category.COMBAT);
   }
}
