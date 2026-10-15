package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;

import java.util.Locale;

/**
 * Port of Krs AutoSneak (com.instrumentalist.krs.hacks.features.movement.AutoSneak) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE};
 *  - Krs {@code tag()} -> {@link Feature#setSuffix(String)} refreshed on UPDATE.
 */
public class AutoSneak2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Always", new String[]{"Always", "Air", "Edge", "Use Item", "Hurt"});

   private boolean pressingSneak = false;

   public AutoSneak2() {
      super("AutoSneak2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      releaseSneak();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;
      setSuffix(this.mode.getValue());

      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) {
         releaseSneak();
         return;
      }

      boolean shouldSneak = switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "air" -> !player.onGround();
         case "edge" -> player.onGround() && level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ())).isAir();
         case "use item" -> player.isUsingItem();
         case "hurt" -> player.hurtTime > 0;
         default -> true;
      };

      if (shouldSneak) {
         mc.options.keyShift.setDown(true);
         pressingSneak = true;
      } else {
         releaseSneak();
      }
   }

   private void releaseSneak() {
      if (!pressingSneak)
         return;

      KeyMapping.setAll();
      pressingSneak = false;
   }
}
