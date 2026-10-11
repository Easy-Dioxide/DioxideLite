package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import java.util.Locale;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoRespawn) and adapted.
 *  Krs PacketUtil.sendPacket maps to mc.getConnection().send(...). */
public class AutoRespawn extends Feature {
   private static final String PACKET = "Packet";
   private static final String CLIENT = "Client";
   private static final String BOTH = "Both";

   private final ModeSetting mode = new ModeSetting("Mode", this, PACKET, new String[]{PACKET, CLIENT, BOTH});
   private final NumberSetting delay = new NumberSetting("Delay", this, 0.0, 0.0, 20.0, 1.0);
   private final BooleanSetting closeDeathScreen = new BooleanSetting("Close Death Screen", this, true);

   private int deathTicks = 0;
   private boolean requested = false;

   public AutoRespawn() {
      super("AutoRespawn", Category.PLAYER);
   }

   @Override
   public void onEnable() {
      reset();
   }

   @Override
   public void onDisable() {
      reset();
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());

      var player = mc.player;
      if (player == null || !player.isDeadOrDying()) {
         reset();
         return;
      }

      if (this.deathTicks < (int) this.delay.getValue()) {
         this.deathTicks++;
         return;
      }

      if (this.requested) return;

      switch (this.mode.getValue().toLowerCase(Locale.ROOT)) {
         case "client" -> player.respawn();
         case "both" -> {
            mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            player.respawn();
         }
         default -> mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
      }
      if (this.closeDeathScreen.getValue() && mc.gui.screen() instanceof DeathScreen)
         mc.gui.setScreen(null);
      this.requested = true;
   }

   private void reset() {
      this.deathTicks = 0;
      this.requested = false;
   }
}
