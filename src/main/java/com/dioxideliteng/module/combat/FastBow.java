package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.FastBow) and adapted. */
public class FastBow extends Feature {
   private final NumberSetting packets = new NumberSetting("Packets", this, 20.0, 1.0, 20.0, 1.0);
   private final NumberSetting chargeTicks = new NumberSetting("Charge Ticks", this, 1.0, 1.0, 20.0, 1.0);

   public FastBow() {
      super("Fast Bow", Category.COMBAT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      var player = mc.player;
      var gameMode = mc.gameMode;
      if (player == null || gameMode == null || !player.isUsingItem()) return;

      ItemStack useItem = player.getUseItem();
      if (useItem.getUseAnimation() != ItemUseAnimation.BOW
            || player.getTicksUsingItem() < (int) this.chargeTicks.getValue()) return;

      InteractionHand hand = player.getUsedItemHand();
      float yaw = player.getYRot();
      float pitch = player.getXRot();
      boolean onGround = player.onGround();
      boolean horizontalCollision = player.horizontalCollision;
      int packetCount = (int) this.packets.getValue();
      for (int i = 0; i < packetCount; i++) {
         mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, horizontalCollision));
      }

      mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));

      player.releaseUsingItem();
      if (mc.options.keyUse.isDown()
            && player.getItemInHand(hand).getUseAnimation() == ItemUseAnimation.BOW) {
         gameMode.useItem(player, hand);
      }
   }
}
