package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoTool) and adapted.
 *  Krs PlayerUtil slot-spoofing (doSpoof/stopSpoof) is dropped — this port performs a
 *  real hotbar switch to the best tool while breaking and restores the previous slot
 *  after a short delay. ToolUtil.findBestTool is inlined with getDestroySpeed. */
public class AutoTool2 extends Feature {
   private BlockPos breakingPos = null;
   private int toolOriginalSlot = -1;
   private int toolSelectedSlot = -1;
   private int restoreDelayTicks = 0;

   public AutoTool2() {
      super("AutoTool2", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      resetState();
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.WORLD) {
         resetState();
         return;
      }
      if (event instanceof EventPacketSend send
            && send.getPacket() instanceof ServerboundPlayerActionPacket actionPacket
            && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
         this.breakingPos = actionPacket.getPos();
         this.restoreDelayTicks = 4;
         return;
      }
      if (event != Events.TICK) return;

      var player = mc.player;
      if (player == null) {
         resetState();
         return;
      }

      if (this.breakingPos != null) updateBreakingTool();
   }

   private void updateBreakingTool() {
      if (!isBreakingInputHeld()) {
         resetToolSlot();
         return;
      }

      BlockPos targetPos = getTargetBlockPos();
      if (targetPos != null && mc.level != null && !mc.level.getBlockState(targetPos).isAir()) {
         this.breakingPos = targetPos;
         if (switchToBestTool(targetPos)) {
            this.restoreDelayTicks = 4;
         } else {
            resetToolSlot();
         }
         return;
      }

      if (this.restoreDelayTicks > 0) {
         this.restoreDelayTicks--;
         return;
      }

      resetToolSlot();
   }

   private boolean isBreakingInputHeld() {
      return mc.options.keyAttack.isDown() && !mc.options.keyUse.isDown();
   }

   private BlockPos getTargetBlockPos() {
      if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
      return ((BlockHitResult) mc.hitResult).getBlockPos();
   }

   private boolean switchToBestTool(BlockPos pos) {
      var player = mc.player;
      if (player == null || mc.level == null) return false;

      BlockState state = mc.level.getBlockState(pos);
      int bestToolSlot = -1;
      float bestSpeed = 1.0F;
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.isEmpty()) continue;
         float destroySpeed = stack.getDestroySpeed(state);
         if (destroySpeed > bestSpeed) {
            bestSpeed = destroySpeed;
            bestToolSlot = slot;
         }
      }

      if (bestToolSlot == -1) return false;

      if (this.toolOriginalSlot == -1 && player.getInventory().getSelectedSlot() == bestToolSlot) return true;

      if (this.toolOriginalSlot == -1) this.toolOriginalSlot = player.getInventory().getSelectedSlot();

      this.toolSelectedSlot = bestToolSlot;
      if (player.getInventory().getSelectedSlot() != bestToolSlot)
         player.getInventory().setSelectedSlot(bestToolSlot);

      return true;
   }

   private void resetToolSlot() {
      var player = mc.player;
      if (player != null && this.toolOriginalSlot >= 0 && this.toolOriginalSlot <= 8
            && player.getInventory().getSelectedSlot() == this.toolSelectedSlot)
         player.getInventory().setSelectedSlot(this.toolOriginalSlot);

      this.breakingPos = null;
      this.toolOriginalSlot = -1;
      this.toolSelectedSlot = -1;
      this.restoreDelayTicks = 0;
   }

   private void resetState() {
      resetToolSlot();
   }
}
