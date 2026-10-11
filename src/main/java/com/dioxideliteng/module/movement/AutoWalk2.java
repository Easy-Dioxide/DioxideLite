package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import net.minecraft.client.KeyMapping;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.AutoWalk) and adapted to the client. */
public class AutoWalk2 extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String ONLY_IN_GAME_LABEL = "Only In Game";
   private static final String SPRINT_LABEL = "Sprint";
   private static final String SWITCH_TICKS_LABEL = "Switch Ticks";
   private static final String FORWARD = "Forward";
   private static final String BACKWARD = "Backward";
   private static final String LEFT = "Left";
   private static final String RIGHT = "Right";
   private static final String JUMP = "Jump";
   private static final String RANDOM = "Random";

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, FORWARD,
      new String[]{FORWARD, BACKWARD, LEFT, RIGHT, JUMP, RANDOM});
   private final BooleanSetting onlyInGame = new BooleanSetting(ONLY_IN_GAME_LABEL, this, true);
   private final BooleanSetting sprint = new BooleanSetting(SPRINT_LABEL, this, false);
   private final NumberSetting switchTicks = new NumberSetting(SWITCH_TICKS_LABEL, this, 20.0, 5.0, 100.0, 1.0);

   private boolean pressingMovement;
   private int ticks;
   private int randomDirection;

   public AutoWalk2() {
      super("AutoWalk2", Category.MOVEMENT);
      this.switchTicks.setVisible(() -> this.mode.is(RANDOM));
   }

   @Override
   public void onDisable() {
      this.releaseMovement();
      this.ticks = 0;
      this.randomDirection = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      if (mc.player == null || mc.level == null || this.onlyInGame.getValue() && mc.gui.screen() != null) {
         this.releaseMovement();
         return;
      }

      this.releaseMovement();
      String currentMode = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (currentMode.equals(RANDOM.toLowerCase(Locale.ROOT))) {
         if (this.ticks <= 0) {
            this.randomDirection = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 5);
            this.ticks = (int) this.switchTicks.getValue();
         } else {
            this.ticks--;
         }
      }

      String active = currentMode.equals(RANDOM.toLowerCase(Locale.ROOT)) ? this.randomMode() : currentMode;
      switch (active) {
         case "backward" -> mc.options.keyDown.setDown(true);
         case "left" -> mc.options.keyLeft.setDown(true);
         case "right" -> mc.options.keyRight.setDown(true);
         case "jump" -> {
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(true);
         }
         default -> mc.options.keyUp.setDown(true);
      }
      if (this.sprint.getValue()) {
         mc.options.keySprint.setDown(true);
      }
      this.pressingMovement = true;
   }

   private String randomMode() {
      return switch (this.randomDirection) {
         case 1 -> "backward";
         case 2 -> "left";
         case 3 -> "right";
         case 4 -> "jump";
         default -> "forward";
      };
   }

   private void releaseMovement() {
      if (!this.pressingMovement) return;
      KeyMapping.setAll();
      this.pressingMovement = false;
   }
}
