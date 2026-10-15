package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import com.dioxideliteng.util.krs.TimerUtil;

import java.util.Locale;

/**
 * Port of Krs Spider (com.instrumentalist.krs.hacks.features.movement.Spider) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE.
 */
public class Spider2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Vanilla", new String[]{"Vanilla", "Timer", "Pulse"});
   private final NumberSetting speed = new NumberSetting("Speed", this, 0.32, 0.1, 1.0, 0.1);
   private final NumberSetting timerSpeed = new NumberSetting("Timer Speed", this, 1.7, 1.1, 4.0, 0.1);
   private final BooleanSetting onlyMoving = new BooleanSetting("Only Moving", this, true);

   private boolean timered = false;
   private int pulseTicks = 0;

   public Spider2() {
      super("Spider2", Category.MOVEMENT);
      this.speed.setVisible(() -> !this.mode.is("Timer"));
      this.timerSpeed.setVisible(() -> this.mode.is("Timer"));
   }

   @Override
   public void onDisable() {
      resetTimer();
      pulseTicks = 0;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      setSuffix(this.mode.getValue());

      var player = mc.player;
      if (player == null) {
         resetTimer();
         pulseTicks = 0;
         return;
      }

      boolean climbing = player.horizontalCollision && !player.onClimbable() && (!this.onlyMoving.getValue() || MovementUtil.isMoving());
      if (!climbing) {
         resetTimer();
         pulseTicks = 0;
         return;
      }

      switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "timer" -> {
            TimerUtil.timerSpeed = (float) this.timerSpeed.getValue();
            timered = true;
            if (player.getDeltaMovement().y < 0.0)
               MovementUtil.setVelocityY(0.0);
         }
         case "pulse" -> {
            pulseTicks++;
            if (pulseTicks >= 3) {
               MovementUtil.setVelocityY(this.speed.getValue());
               pulseTicks = 0;
            } else if (player.getDeltaMovement().y < 0.0) {
               MovementUtil.setVelocityY(0.0);
            }
         }
         default -> MovementUtil.setVelocityY(this.speed.getValue());
      }
   }

   private void resetTimer() {
      if (timered) {
         TimerUtil.reset();
         timered = false;
      }
   }
}
