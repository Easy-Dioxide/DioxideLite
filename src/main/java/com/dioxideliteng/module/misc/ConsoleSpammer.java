package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.ConsoleSpammer) and adapted to the client. */
public class ConsoleSpammer extends Feature {
   private static final String MISSING_PLACEMENT = "Missing Placement";
   private static final String WORLD_EDIT = "World Edit";

   private final ModeSetting mode = new ModeSetting("Mode", this, MISSING_PLACEMENT,
         new String[]{MISSING_PLACEMENT, WORLD_EDIT});
   private final NumberSetting sendDelay = new NumberSetting("Send Delay", this, 5.0, 1.0, 50.0, 1.0);

   private int sendTicks;

   public ConsoleSpammer() {
      super("ConsoleSpammer", Category.MISC);
   }

   @Override
   public void onDisable() {
      this.sendTicks = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());
      if (mc.player == null || mc.level == null) return;

      if (this.sendTicks < (int) this.sendDelay.getValue()) {
         this.sendTicks++;
         return;
      }
      this.sendTicks = 0;

      String current = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (current.equals(MISSING_PLACEMENT.toLowerCase(Locale.ROOT))) {
         var pos = mc.player.blockPosition();
         mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
               BlockHitResult.miss(new Vec3(-1000, 1000, -1000), Direction.DOWN, pos), 1));
         mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
               BlockHitResult.miss(new Vec3(1000, -1000, 1000), Direction.DOWN, pos), 1));
         mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
               BlockHitResult.miss(new Vec3(1000, 1000, -1000), Direction.DOWN, pos), 1));
      } else if (current.equals(WORLD_EDIT.toLowerCase(Locale.ROOT))) {
         mc.getConnection().send(new ServerboundCommandSuggestionPacket(0,
               "//search d" + ThreadLocalRandom.current().nextInt(1000)));
      }
   }
}
