package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.event.impl.EventWorld;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.BlinkUtil;
import com.dioxideliteng.util.krs.EntityExtension;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

/**
 * Port of Krs AntiVoid (com.instrumentalist.krs.hacks.features.movement.AntiVoid) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event/notification API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE}; Krs WorldEvent -> {@code EventWorld};
 *    Krs ReceivedPacketEvent -> {@code EventPacketReceive};
 *  - {@code Client.notificationManager.addNotification(...)} (NotificationManager is not
 *    ported) -> {@link DynamicIslandManager#notifyInfo(String, String)}.
 */
public class AntiVoid extends Feature {
   private final BooleanSetting stopXZ = new BooleanSetting("StopXZ", this, true);
   private final NumberSetting distance = new NumberSetting("Distance", this, 6, 0, 10, 1);

   private int canTick = 0;
   private Integer unSafeY = null;

   public AntiVoid() {
      super("Anti Void", Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      if (mc.player == null) return;
      BlinkUtil.INSTANCE.sync(true, false);
      BlinkUtil.INSTANCE.stopBlink();
      canTick = 0;
      unSafeY = null;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventWorld) {
         canTick = 0;
         if (unSafeY != null) {
            BlinkUtil.INSTANCE.sync(true, false);
            BlinkUtil.INSTANCE.stopBlink();
            unSafeY = null;
         }
      } else if (event == Events.UPDATE) {
         var player = mc.player;
         if (player == null || canTick > 0 || !EntityExtension.isFallingToVoid(player) || player.tickCount <= 50 || player.getAbilities().flying || player.isShiftKeyDown() || player.isSpectator() || player.isInWater()) {
            if (unSafeY != null) {
               BlinkUtil.INSTANCE.sync(true, false);
               BlinkUtil.INSTANCE.stopBlink();
               unSafeY = null;
            }
            if (canTick > 0)
               canTick--;
            return;
         }

         if (unSafeY == null)
            unSafeY = player.blockPosition().getY() - (int) distance.getValue();

         BlinkUtil.INSTANCE.doBlink();
         if (player.blockPosition().getY() <= unSafeY) {
            BlinkUtil.INSTANCE.sync(false, true);
            if (stopXZ.getValue())
               MovementUtil.stopMoving();
         }
      } else if (event instanceof EventPacketReceive receiving) {
         var player = mc.player;
         if (player == null || player.tickCount <= 50) return;

         if (receiving.getPacket() instanceof ClientboundPlayerPositionPacket && BlinkUtil.INSTANCE.getBlinking() && unSafeY != null) {
            canTick = 80;
            DynamicIslandManager.notifyInfo("Anti Void", "AntiVoid flagged (wait for " + canTick + "s to re-activate)");
            BlinkUtil.INSTANCE.sync(true);
            BlinkUtil.INSTANCE.stopBlink();
            if (stopXZ.getValue())
               MovementUtil.stopMoving();
            unSafeY = null;
         }
      }
   }
}
