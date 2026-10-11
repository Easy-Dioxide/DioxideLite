package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.AutoSneak) and adapted. */
public class AutoSneak extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String ALWAYS = "Always";
   private static final String AIR = "Air";
   private static final String EDGE = "Edge";
   private static final String USE_ITEM = "Use Item";
   private static final String HURT = "Hurt";

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, ALWAYS,
         new String[]{ALWAYS, AIR, EDGE, USE_ITEM, HURT});

   private boolean pressingSneak;

   public AutoSneak() {
      super("Auto Sneak", Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.releaseSneak();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) {
         this.releaseSneak();
         return;
      }

      boolean shouldSneak;
      if (this.mode.is(AIR)) {
         shouldSneak = !player.onGround();
      } else if (this.mode.is(EDGE)) {
         shouldSneak = player.onGround()
               && level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ())).isAir();
      } else if (this.mode.is(USE_ITEM)) {
         shouldSneak = player.isUsingItem();
      } else if (this.mode.is(HURT)) {
         shouldSneak = player.hurtTime > 0;
      } else {
         shouldSneak = true;
      }

      if (shouldSneak) {
         mc.options.keyShift.setDown(true);
         this.pressingSneak = true;
      } else {
         this.releaseSneak();
      }
   }

   private void releaseSneak() {
      if (!this.pressingSneak) return;
      KeyMapping.setAll();
      this.pressingSneak = false;
   }
}
