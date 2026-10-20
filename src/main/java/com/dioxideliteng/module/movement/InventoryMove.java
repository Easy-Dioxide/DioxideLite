package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.phys.Vec3;

public class InventoryMove extends Feature {
   private static final String INV_MOVE_LABEL = "InvMove";
   private static final String MOTION_LABEL = "Motion";

   private NumberSetting motion;

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION && mc.gui.screen() != null && !(mc.gui.screen() instanceof ChatScreen)) {
         KeyMapping.setAll();
         this.scaleHorizontalMotion();
      }
   }

   private void scaleHorizontalMotion() {
      Vec3 motion = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(motion.x * this.motion.getValue(), motion.y, motion.z * this.motion.getValue());
   }

   public InventoryMove() {
      super(INV_MOVE_LABEL, Category.MOVEMENT);
      this.motion = new NumberSetting(MOTION_LABEL, this, 1.0, 0.1, 1.0, 0.1);
   }
}
