package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoLeave) and adapted.
 *  Krs MovementUtil.isBlockBelow is inlined; level.disconnect(Component) is a real 26.3
 *  ClientLevel method. */
public class AutoLeave extends Feature {
   private static final String HEALTH = "Health";
   private static final String VOID = "Void";
   private static final String BOTH = "Both";
   private static final String NO_TOTEM = "No Totem";
   private static final String SMART = "Smart";

   private final ModeSetting mode = new ModeSetting("Mode", this, HEALTH, new String[]{HEALTH, VOID, BOTH, NO_TOTEM, SMART});
   private final NumberSetting health = new NumberSetting("Health", this, 4.0, 1.0, 20.0, 1.0);
   private final BooleanSetting multiplayerOnly = new BooleanSetting("Multiplayer Only", this, true);

   private boolean left = false;

   public AutoLeave() {
      super("AutoLeave", Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.left = false;
   }

   @Override
   public void onDisable() {
      this.left = false;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      this.setSuffix(this.mode.getValue());

      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null || this.left || this.multiplayerOnly.getValue() && mc.hasSingleplayerServer())
         return;

      boolean healthDanger = usesHealth() && player.getHealth() <= this.health.getValue();
      boolean voidDanger = usesVoid() && player.getY() < level.getMinY() - 8 && !isBlockBelow();
      boolean totemDanger = usesTotem() && player.getHealth() <= this.health.getValue() && !hasTotem();
      if (!healthDanger && !voidDanger && !totemDanger) return;

      this.left = true;
      level.disconnect(Component.literal("Auto Leave"));
   }

   private boolean usesHealth() {
      return this.mode.is(HEALTH) || this.mode.is(BOTH) || this.mode.is(SMART);
   }

   private boolean usesVoid() {
      return this.mode.is(VOID) || this.mode.is(BOTH) || this.mode.is(SMART);
   }

   private boolean usesTotem() {
      return this.mode.is(NO_TOTEM) || this.mode.is(SMART);
   }

   private boolean hasTotem() {
      var player = mc.player;
      if (player == null) return false;

      if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return true;

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         if (player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING) return true;
      }
      return false;
   }

   private boolean isBlockBelow() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return false;
      BlockPos pos = player.blockPosition();
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      int x = pos.getX();
      int z = pos.getZ();
      for (int y = pos.getY() - 1; y >= level.getMinY(); y--) {
         cursor.set(x, y, z);
         if (!level.getBlockState(cursor).isAir()) return true;
      }
      return false;
   }
}
