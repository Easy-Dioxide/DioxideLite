package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.TickTimer;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.ConsoleSpammer).
 * Krs parity: "Missing Placement" spams far-away miss use-item-on packets (console
 * error spam), "World Edit" spams command-suggestion packets with oversized //search
 * commands. Both run on a tick timer with the configured send delay.
 */
public class ConsoleSpammer extends Feature {

   private final ModeSetting mode = new ModeSetting("Mode", this, "Missing Placement",
         new String[]{"Missing Placement", "World Edit"});
   private final NumberSetting sendDelay = new NumberSetting("Send Delay", this, 5.0, 1.0, 50.0, 1.0);

   private final TickTimer sendTimer = new TickTimer();

   public ConsoleSpammer() {
      super("Console Spammer", Category.MISC);
   }

   @Override
   public void onDisable() {
      if (mc.player == null) return;
      sendTimer.reset();
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      if (mc.player == null || mc.level == null) return;

      switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "missing placement" -> {
            sendTimer.update();
            if (sendTimer.hasTimePassed((int) this.sendDelay.getValue())) {
               PacketUtil.sendPacket(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, BlockHitResult.miss(new Vec3(-1000, 1000, -1000), Direction.DOWN, mc.player.blockPosition()), 1));
               PacketUtil.sendPacket(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, BlockHitResult.miss(new Vec3(1000, -1000, 1000), Direction.DOWN, mc.player.blockPosition()), 1));
               PacketUtil.sendPacket(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, BlockHitResult.miss(new Vec3(1000, 1000, -1000), Direction.DOWN, mc.player.blockPosition()), 1));
               DynamicIslandManager.notifyInfo("Console Spammer", "Sent packet");
               sendTimer.reset();
            }
         }
         case "world edit" -> {
            sendTimer.update();
            if (sendTimer.hasTimePassed((int) this.sendDelay.getValue())) {
               PacketUtil.sendPacket(new ServerboundCommandSuggestionPacket(0, "//search d" + ThreadLocalRandom.current().nextInt(1000)));
               DynamicIslandManager.notifyInfo("Console Spammer", "Sent packet");
               sendTimer.reset();
            }
         }
      }
   }
}
