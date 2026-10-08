package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;

public class Speed extends Feature {
   private static final String JUMP_LABEL = "Jump";
   private static final String ROTATE_LABEL = "Rotate";
   private static final String SPEED_LABEL = "Speed";
   private BooleanSetting rotate = new BooleanSetting(ROTATE_LABEL, this, false);
   private BooleanSetting jump = new BooleanSetting(JUMP_LABEL, this, false);

   @Override
   public int getPriority(Event event) {
      return event == Events.ROTATION ? -3 : 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION
         && this.rotate.getValue()
         && !mc.options.keyRight.isDown()
         && !mc.options.keyLeft.isDown()
         && (!mc.player.onGround() || !mc.options.keyJump.isDown() && !this.jump.getValue())) {
         Events.ROTATION.setYaw(mc.player.getYRot() + 45.0F);
      }

      if (event == Events.POST_MOVE_INPUT && this.jump.getValue()) {
         mc.player.input.makeJump();
      }
   }

   public Speed() {
      super(SPEED_LABEL, Category.MOVEMENT);
   }
}
