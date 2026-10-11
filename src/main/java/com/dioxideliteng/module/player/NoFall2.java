package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.TimerController;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.NoFall) and adapted.
 *  Self-contained modes are ported (Packet, Spoof, No Ground, Hypixel NCP, Cubecraft
 *  Reduce). Krs TimerUtil maps to TimerController; PacketUtil.sendPacket maps to
 *  mc.getConnection().send(...). The "Less Packet" mode (needs an isMoving setter absent
 *  on EventPreMotion) and "Grim 1.9+" mode (needs silent packet sends + PRE_MOTION
 *  cancellation that the client does not honor) are omitted. */
public class NoFall2 extends Feature {
   private static final String PACKET = "Packet";
   private static final String SPOOF = "Spoof";
   private static final String NO_GROUND = "No Ground";
   private static final String HYPIXEL_NCP = "Hypixel NCP";
   private static final String CUBECRAFT_REDUCE = "Cubecraft Reduce";

   private final ModeSetting mode = new ModeSetting("Mode", this, PACKET,
         new String[]{PACKET, SPOOF, NO_GROUND, HYPIXEL_NCP, CUBECRAFT_REDUCE});

   private boolean timered = false;
   private int timerStage = 0;
   private int fallTicks = 0;

   public NoFall2() {
      super("NoFall2", Category.PLAYER);
   }

   @Override
   public void onDisable() {
      resetTimer();
      this.timered = false;
      this.fallTicks = 0;
   }

   @Override
   public void onEnable() {
      resetTimer();
      this.timered = false;
      this.fallTicks = 0;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.ROTATION) {
         this.setSuffix(this.mode.getValue());
         return;
      }
      if (event == Events.TICK) {
         onTick();
         return;
      }
      if (event == Events.PRE_MOTION) {
         onMotion();
      }
   }

   private void onTick() {
      var player = mc.player;
      if (player == null || player.isSpectator() || player.isFallFlying() || player.isSwimming()) {
         resetTimer();
         this.timered = false;
         this.fallTicks = 0;
         return;
      }

      if (this.mode.is(HYPIXEL_NCP)) {
         if (player.getDeltaMovement().y <= -0.6 && !isBlockBelow() && !mc.options.keyShift.isDown() && !isFallingToVoid(player)) {
            switch (this.timerStage) {
               case 0 -> {
                  TimerController.setMultiplier(0.5f);
                  mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                  this.timerStage++;
                  this.timered = true;
               }
               case 1 -> {
                  TimerController.reset();
                  this.timerStage = 0;
               }
            }
         } else if (this.timered) {
            TimerController.reset();
            this.timerStage = 0;
            this.timered = false;
         }
      }
   }

   private void onMotion() {
      var player = mc.player;
      if (player == null || player.isSpectator() || player.isFallFlying() || player.isSwimming()) {
         resetTimer();
         this.timered = false;
         this.fallTicks = 0;
         return;
      }

      double motionY = player.getDeltaMovement().y;
      String current = this.mode.getValue().toLowerCase(Locale.ROOT);
      switch (current) {
         case "packet" -> {
            if (player.onGround()) {
               this.fallTicks = 0;
               return;
            }
            if (motionY < -0.5) {
               this.fallTicks++;
               if (this.fallTicks > 1) {
                  Events.PRE_MOTION.setOnGround(true);
                  this.fallTicks = 0;
               }
            }
         }
         case "spoof" -> Events.PRE_MOTION.setOnGround(true);
         case "no ground" -> Events.PRE_MOTION.setOnGround(false);
         case "cubecraft reduce" -> {
            if (motionY < -0.5) {
               double distanceToGround = getDistanceToGround();
               if (distanceToGround >= 0.0 && distanceToGround <= 2.0 && !hasBlockAbove(2.0) && !hasPowderSnowNearby(2.0)) {
                  Events.PRE_MOTION.setY(Events.PRE_MOTION.getY() + 2);
                  Events.PRE_MOTION.setOnGround(false);
               }
            }
         }
         default -> { }
      }
   }

   private void resetTimer() {
      if (this.timerStage != 0) {
         TimerController.reset();
         this.timerStage = 0;
      }
   }

   private boolean isBlockBelow() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return false;
      BlockPos pos = player.blockPosition();
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      int x = pos.getX();
      int z = pos.getZ();
      int limit = Math.max(level.getMinY(), pos.getY() - 20);
      for (int y = pos.getY() - 1; y >= limit; y--) {
         cursor.set(x, y, z);
         if (!level.getBlockState(cursor).isAir()) return true;
      }
      return false;
   }

   private boolean isFallingToVoid(net.minecraft.world.entity.player.Player player) {
      var level = mc.level;
      if (level == null) return false;
      return player.getY() < level.getMinY() - 16;
   }

   private double getDistanceToGround() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return Double.MAX_VALUE;

      AABB playerBox = player.getBoundingBox();
      double playerY = playerBox.minY;
      double closestDistance = Double.MAX_VALUE;

      for (int x = (int) Math.floor(playerBox.minX); x <= (int) Math.floor(playerBox.maxX); x++) {
         for (int y = (int) Math.floor(playerY); y >= (int) Math.floor(playerY - 20.0); y--) {
            for (int z = (int) Math.floor(playerBox.minZ); z <= (int) Math.floor(playerBox.maxZ); z++) {
               BlockPos pos = new BlockPos(x, y, z);
               var state = level.getBlockState(pos);
               if (state.isAir()) continue;

               for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
                  AABB collisionBox = box.move(pos);
                  if (collisionBox.maxY > playerY || collisionBox.maxX <= playerBox.minX || collisionBox.minX >= playerBox.maxX || collisionBox.maxZ <= playerBox.minZ || collisionBox.minZ >= playerBox.maxZ)
                     continue;

                  closestDistance = Math.min(closestDistance, playerY - collisionBox.maxY);
               }
            }
         }
      }

      return closestDistance;
   }

   private boolean hasBlockAbove(double distance) {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return true;

      AABB playerBox = player.getBoundingBox();
      AABB checkBox = new AABB(playerBox.minX, playerBox.maxY, playerBox.minZ, playerBox.maxX, playerBox.maxY + distance, playerBox.maxZ);
      for (var shape : level.getBlockCollisions(player, checkBox)) {
         if (!shape.isEmpty()) return true;
      }

      return false;
   }

   private boolean hasPowderSnowNearby(double distance) {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return true;

      AABB checkBox = player.getBoundingBox().inflate(distance, distance, distance);
      for (int x = (int) Math.floor(checkBox.minX); x <= (int) Math.floor(checkBox.maxX); x++) {
         for (int y = (int) Math.floor(checkBox.minY); y <= (int) Math.floor(checkBox.maxY); y++) {
            for (int z = (int) Math.floor(checkBox.minZ); z <= (int) Math.floor(checkBox.maxZ); z++) {
               if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.POWDER_SNOW)) return true;
            }
         }
      }

      return false;
   }
}
