package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.RandomUtil;
import net.minecraft.client.KeyMapping;

import java.util.Locale;

/**
 * Port of Krs AutoWalk (com.instrumentalist.krs.hacks.features.movement.AutoWalk) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE;
 *  - {@code com.instrumentalist.krs.utils.math.RandomUtil} -> {@link RandomUtil} (util/krs).
 */
public class AutoWalk2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Forward", new String[]{"Forward", "Backward", "Left", "Right", "Jump", "Random"});
   private final BooleanSetting onlyInGame = new BooleanSetting("Only In Game", this, true);
   private final BooleanSetting sprint = new BooleanSetting("Sprint", this, false);
   private final NumberSetting switchTicks = new NumberSetting("Switch Ticks", this, 20, 5, 100, 1);

   private boolean pressingMovement = false;
   private int ticks = 0;
   private int randomDirection = 0;

   public AutoWalk2() {
      super("AutoWalk2", Category.MOVEMENT);
      this.switchTicks.setVisible(() -> this.mode.is("Random"));
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      releaseMovement();
      ticks = 0;
      randomDirection = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      setSuffix(this.mode.getValue());

      if (mc.player == null || mc.level == null || this.onlyInGame.getValue() && mc.gui.screen() != null) {
         releaseMovement();
         return;
      }

      releaseMovement();
      String currentMode = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (currentMode.equals("random")) {
         if (ticks <= 0) {
            randomDirection = RandomUtil.nextInt(0, 5);
            ticks = (int) this.switchTicks.getValue();
         } else ticks--;
      }

      switch (currentMode.equals("random") ? randomMode() : currentMode) {
         case "backward" -> mc.options.keyDown.setDown(true);
         case "left" -> mc.options.keyLeft.setDown(true);
         case "right" -> mc.options.keyRight.setDown(true);
         case "jump" -> {
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(true);
         }
         default -> mc.options.keyUp.setDown(true);
      }
      if (this.sprint.getValue())
         mc.options.keySprint.setDown(true);
      pressingMovement = true;
   }

   private String randomMode() {
      return switch (randomDirection) {
         case 1 -> "backward";
         case 2 -> "left";
         case 3 -> "right";
         case 4 -> "jump";
         default -> "forward";
      };
   }

   private void releaseMovement() {
      if (!pressingMovement)
         return;

      KeyMapping.setAll();
      pressingMovement = false;
   }
}
