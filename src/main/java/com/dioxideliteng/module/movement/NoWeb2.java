package com.dioxideliteng.module.movement;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Port of Krs NoWeb (com.instrumentalist.krs.hacks.features.movement.NoWeb) to
 * DioxideLiteNG. Logic is a 1:1 copy; only the event API is adapted:
 *  - Krs UpdateEvent -> {@link Events#UPDATE}.
 */
public class NoWeb2 extends Feature {
   private final NumberSetting speed = new NumberSetting("Speed", this, 0.35, 0.05, 1.0, 0.05);
   private final BooleanSetting vertical = new BooleanSetting("Vertical", this, true);

   public NoWeb2() {
      super("NoWeb2", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.UPDATE) return;

      var player = mc.player;
      if (player == null || !isInWeb()) return;

      if (MovementUtil.isMoving())
         MovementUtil.setSpeed(Math.max(MovementUtil.getSpeed(), this.speed.getValue()));

      if (this.vertical.getValue()) {
         if (mc.options.keyJump.isDown())
            MovementUtil.setVelocityY(this.speed.getValue());
         else if (mc.options.keyShift.isDown())
            MovementUtil.setVelocityY(-this.speed.getValue());
      }
   }

   private boolean isInWeb() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null)
         return false;

      AABB box = player.getBoundingBox();
      for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
         for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
            for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
               if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB))
                  return true;
            }
         }
      }
      return false;
   }
}
