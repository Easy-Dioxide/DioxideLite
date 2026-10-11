package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.TimerController;
import java.util.Locale;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.level.Timer) and adapted.
 *  Uses the client's TimerController.setMultiplier instead of Krs's TimerUtil.timerSpeed.
 *  Class named Timer2 to avoid clashing with the existing movement/Timer module. */
public class Timer2 extends Feature {
   private static final String VANILLA = "Vanilla";
   private static final String ROUNDING_ERROR = "Rounding Error";

   private final ModeSetting mode = new ModeSetting("Mode", this, VANILLA,
         new String[]{VANILLA, ROUNDING_ERROR});
   private final NumberSetting speed = new NumberSetting("Speed", this, 1.5, 0.1, 10.0, 0.1);
   private final BooleanSetting moveOnly = new BooleanSetting("Move Only", this, false);

   public Timer2() {
      super("Timer2", Category.PLAYER);
      this.speed.setVisible(this::isVanilla);
      this.moveOnly.setVisible(this::isVanilla);
   }

   private boolean isVanilla() {
      return this.mode.is(VANILLA);
   }

   @Override
   public void onDisable() {
      TimerController.setMultiplier(1.0F);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ROTATION) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null) return;

      String current = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (current.equals(VANILLA.toLowerCase(Locale.ROOT))) {
         boolean moving = false;
         var dm = player.getDeltaMovement();
         if (dm.x * dm.x + dm.z * dm.z > 1.0E-6) moving = true;
         if (this.moveOnly.getValue() && !moving) {
            TimerController.setMultiplier(1.0F);
         } else {
            TimerController.setMultiplier((float) this.speed.getValue());
         }
      } else if (current.equals(ROUNDING_ERROR.toLowerCase(Locale.ROOT))) {
         TimerController.setMultiplier(1.0075F);
      }
   }
}
