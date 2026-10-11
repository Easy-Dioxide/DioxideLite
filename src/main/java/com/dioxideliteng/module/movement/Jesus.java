package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventBlock;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.movement.Jesus) and adapted. */
public class Jesus extends Feature {
   private static final String MODE_LABEL = "Mode";
   private static final String VANILLA = "Vanilla";
   private static final String VERUS = "Verus";
   private static final String BOUNCY = "Bouncy";
   private static final String MINI_JUMP = "Mini Jump";

   private static final VoxelShape WATER_SHAPE = Shapes.create(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 0.9999D, 1.0D));

   private boolean wasInLiquid;

   private final ModeSetting mode = new ModeSetting(MODE_LABEL, this, VANILLA,
         new String[]{VANILLA, VERUS, BOUNCY, MINI_JUMP});

   public Jesus() {
      super("Jesus", Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.wasInLiquid = false;
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.BLOCK) {
         this.onBlock((EventBlock) event);
      } else if (event == Events.TICK) {
         this.onTick();
      }
   }

   private void onBlock(EventBlock event) {
      var player = mc.player;
      if (player == null || mc.level == null || !usesSolidCollision(this.mode.getValue()) || !isLiquid(event.getBlockState()))
         return;

      Entity vehicle = player.getVehicle();
      boolean canStandOnLiquid = isLiquidBelow(player, false)
            || isLiquidBelow(vehicle, false) && vehicle.fallDistance < 3.0F;

      if (!player.isShiftKeyDown()
            && player.fallDistance < 3.0F
            && !isLiquidAtFeet(player)
            && canStandOnLiquid
            && isEntityAboveBlock(player, event.getBlockPos()))
         event.setVoxelShape(WATER_SHAPE);
   }

   private void onTick() {
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null) return;

      if (this.mode.is(VERUS)) {
         if (!player.isInWater()) return;
         if (player.hasEffect(MobEffects.SPEED))
            this.strafe(0.38);
         else
            this.strafe(0.33);
      } else if (this.mode.is(BOUNCY)) {
         this.handleWaterMode(0.6D, true);
      } else if (this.mode.is(MINI_JUMP)) {
         this.handleWaterMode(0.2D, false);
      }
   }

   private void handleWaterMode(double liquidBoost, boolean bouncy) {
      var player = mc.player;
      if (player == null) return;

      if (player.isShiftKeyDown()) return;

      if (isLiquidAtFeet(player) && !isSneakInputDown()) {
         this.setVelocityY(0.1D);
         return;
      }

      if (player.onGround() || player.onClimbable())
         this.wasInLiquid = false;

      double motionY = player.getDeltaMovement().y;
      if (motionY > 0.0D && this.wasInLiquid) {
         if (motionY < 0.03D) {
            pushVelocityY(0.06713D);
         } else if (motionY <= 0.05D) {
            if (bouncy) scaleVelocityY(1.20000000999D);
            pushVelocityY(0.06D);
         } else if (motionY <= 0.08D) {
            if (bouncy) scaleVelocityY(1.20000003D);
            pushVelocityY(0.055D);
         } else if (motionY <= 0.112D) {
            pushVelocityY(0.0535D);
         } else {
            scaleVelocityY(bouncy ? 1.000000000002D : 0.500000000002D);
            pushVelocityY(0.0517D);
         }
      }

      motionY = player.getDeltaMovement().y;
      if (this.wasInLiquid && motionY < 0.0D && motionY > -0.3D)
         pushVelocityY(0.045835D);

      player.fallDistance = 0.0F;

      if (!isLiquidBelow(player, true)) return;

      this.setVelocityY(liquidBoost);

      this.wasInLiquid = true;
   }

   private static void pushVelocityY(double y) {
      var player = mc.player;
      if (player == null) return;
      player.push(0.0D, y, 0.0D);
   }

   private static void scaleVelocityY(double multiplier) {
      var player = mc.player;
      if (player == null) return;
      var motion = player.getDeltaMovement();
      player.setDeltaMovement(motion.x, motion.y * multiplier, motion.z);
   }

   private void setVelocityY(double y) {
      var player = mc.player;
      if (player == null) return;
      var motion = player.getDeltaMovement();
      player.setDeltaMovement(motion.x, y, motion.z);
   }

   private void strafe(double speed) {
      var player = mc.player;
      if (player == null) return;
      var dm = player.getDeltaMovement();
      double current = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
      if (current > 1.0E-6) {
         double factor = speed / current;
         player.setDeltaMovement(dm.x * factor, dm.y, dm.z * factor);
      }
   }

   private static boolean usesSolidCollision(String value) {
      return value.equalsIgnoreCase(VANILLA)
            || value.equalsIgnoreCase(BOUNCY)
            || value.equalsIgnoreCase(MINI_JUMP);
   }

   private static boolean isSneakInputDown() {
      return mc.options.keyShift.isDown();
   }

   private static boolean isLiquidBelow(Entity entity, boolean shallow) {
      if (entity == null) return false;
      double offset = shallow ? 0.03D : entity instanceof Player ? 0.2D : 0.5D;
      return isLiquidAt(entity, entity.getY() - offset, true);
   }

   private static boolean isLiquidAtFeet(Entity entity) {
      return entity != null && isLiquidAt(entity, entity.getY() + 0.01D, false);
   }

   private static boolean isLiquidAt(Entity entity, double y, boolean floorY) {
      var level = mc.level;
      if (entity == null || level == null) return false;
      AABB box = entity.getBoundingBox();
      int minX = Mth.floor(box.minX);
      int maxX = Mth.ceil(box.maxX);
      int minZ = Mth.floor(box.minZ);
      int maxZ = Mth.ceil(box.maxZ);
      int blockY = floorY ? Mth.floor(y) : (int) y;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int x = minX; x < maxX; x++) {
         for (int z = minZ; z < maxZ; z++) {
            pos.set(x, blockY, z);
            if (isLiquid(level.getBlockState(pos))) return true;
         }
      }
      return false;
   }

   private static boolean isLiquid(BlockState state) {
      var fluidState = state.getFluidState();
      return fluidState.is(FluidTags.WATER) || fluidState.is(FluidTags.LAVA);
   }

   private static boolean isEntityAboveBlock(Entity entity, BlockPos pos) {
      return entity.getY() >= pos.getY();
   }
}
