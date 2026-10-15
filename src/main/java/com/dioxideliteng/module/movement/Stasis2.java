package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.util.krs.BlinkUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Krs Stasis (com.instrumentalist.krs.hacks.features.movement.Stasis) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE}; Krs SendPacketEvent ->
 *    {@code event instanceof EventPacketSend} (cancel via {@code setCancelled(true)}).
 */
public class Stasis2 extends Feature {
   private final BooleanSetting stopX = new BooleanSetting("Stop X", this, true);
   private final BooleanSetting stopY = new BooleanSetting("Stop Y", this, true);
   private final BooleanSetting stopZ = new BooleanSetting("Stop Z", this, true);
   private final BooleanSetting cancelPlayerMovement = new BooleanSetting("Cancel Player Movement", this, true);

   private int ticksSinceMotionRefresh;
   private Vec3 storedMovement = Vec3.ZERO;

   public Stasis2() {
      super("Stasis2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.level == null) return;

      BlinkUtil.INSTANCE.setLimiter(false);
      ticksSinceMotionRefresh = 0;

      var player = mc.player;
      if (player == null) return;

      player.setDeltaMovement(Vec3.ZERO);
      storedMovement = player.getDeltaMovement();
   }

   @Override
   public void onDisable() {
      resetMotionState();

      if (mc.level != null) {
         BlinkUtil.INSTANCE.setLimiter(false);
      }
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.UPDATE) {
         var player = mc.player;
         if (player == null) return;

         if (this.cancelPlayerMovement.getValue()) {
            player.setDeltaMovement(Vec3.ZERO);
            return;
         }

         Vec3 currentMovement = player.getDeltaMovement();
         refreshStoredMovement(currentMovement);
         player.setDeltaMovement(applyAxisLocks(currentMovement));
      } else if (event instanceof EventPacketSend sending) {
         if (mc.level == null) return;

         if (this.cancelPlayerMovement.getValue() && sending.getPacket() instanceof ServerboundMovePlayerPacket) {
            cancelMovementPacket(sending);
            return;
         }

         if (ticksSinceMotionRefresh != 0) {
            sending.setCancelled(true);
         }
      }
   }

   private void refreshStoredMovement(Vec3 currentMovement) {
      ticksSinceMotionRefresh++;
      if (ticksSinceMotionRefresh < 15) return;

      ticksSinceMotionRefresh = 0;
      storedMovement = currentMovement;
   }

   private Vec3 applyAxisLocks(Vec3 currentMovement) {
      return new Vec3(
            this.stopX.getValue() ? storedMovement.x : currentMovement.x,
            this.stopY.getValue() ? storedMovement.y : currentMovement.y,
            this.stopZ.getValue() ? storedMovement.z : currentMovement.z
      );
   }

   private void cancelMovementPacket(EventPacketSend event) {
      var player = mc.player;
      if (player != null) {
         Vec3 movement = player.getDeltaMovement();
         player.setDeltaMovement(movement.x, 0.0, movement.z);
         BlinkUtil.INSTANCE.setLimiter(true);
      }

      event.setCancelled(true);
   }

   private void resetMotionState() {
      ticksSinceMotionRefresh = 0;
      storedMovement = Vec3.ZERO;
   }
}
