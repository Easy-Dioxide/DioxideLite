package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.util.TargetFinder;
import net.minecraft.world.entity.Entity;

public class AutoWalk extends Feature {
   private static final String ROTATE_TO_ENTITY_LABEL = "Rotate to Entity";
   private static final String AUTO_WALK_LABEL = "AutoWalk";
   private BooleanSetting rotateToEntity = new BooleanSetting(ROTATE_TO_ENTITY_LABEL, this, false);

   private void rotateToEntity(Entity entity) {
      double deltaX = entity.getX() - mc.player.getX();
      double deltaZ = entity.getZ() - mc.player.getZ();
      float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
      mc.player.setYRot(yaw);
   }

   public AutoWalk() {
      super(AUTO_WALK_LABEL, Category.MOVEMENT);
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION && mc.gui.screen() == null) {
         mc.options.keyUp.setDown(true);
         if (this.rotateToEntity.getValue()) {
            Entity entity = TargetFinder.nearestNonBotPlayer();
            if (entity != null) {
               this.rotateToEntity(entity);
            }
         }
      }
   }

   @Override
   public void onDisable() {
      mc.options.keyUp.setDown(false);
   }
}
