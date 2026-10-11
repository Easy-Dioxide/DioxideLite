package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventPacketSend;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.LongThrow) and adapted to the client. */
public class LongThrow extends Feature {
   private final NumberSetting packets = new NumberSetting("Packets", this, 20.0, 1.0, 20.0, 1.0);

   public LongThrow() {
      super("LongThrow", Category.MISC);
   }

   @Override
   public void onEvent(Event event) {
      if (!(event instanceof EventPacketSend sending)) return;
      var player = mc.player;
      if (player == null) return;

      var packet = sending.getPacket();
      boolean shouldBoost = false;

      if (packet instanceof ServerboundPlayerActionPacket actionPacket
            && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
         Item item = player.getUseItem().getItem();
         shouldBoost = item instanceof BowItem || item instanceof TridentItem;
      } else if (packet instanceof ServerboundUseItemPacket useItemPacket
            && useItemPacket.hand() == InteractionHand.MAIN_HAND) {
         Item item = player.getMainHandItem().getItem();
         shouldBoost = item instanceof EggItem || item instanceof ExperienceBottleItem
               || item instanceof SnowballItem || item instanceof EnderpearlItem
               || item instanceof SplashPotionItem;
      }

      if (!shouldBoost) return;

      mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      boolean horizontalCollision = player.horizontalCollision;
      int packetCount = (int) this.packets.getValue();
      for (int i = 0; i < packetCount; i++) {
         mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y - 1e-12, z, true, horizontalCollision));
         mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 1e-12, z, false, horizontalCollision));
      }
   }
}
