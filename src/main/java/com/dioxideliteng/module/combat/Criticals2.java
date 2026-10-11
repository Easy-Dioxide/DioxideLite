package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Criticals) and adapted. */
public class Criticals2 extends Feature {
   private static final String PACKET_LABEL = "Packet";
   private static final String LESS_PACKET_LABEL = "Less Packet";

   private final ModeSetting mode = new ModeSetting("Mode", this, PACKET_LABEL, new String[]{PACKET_LABEL, LESS_PACKET_LABEL});
   private final BooleanSetting cooldownCheck = new BooleanSetting("Cooldown Check", this, false);

   private final double[] packetValues = {0.0625, 0.0, 0.05, 0.0};
   private final double[] lessPacketValues = {0.0625, 0.0};

   public Criticals2() {
      super("Criticals2", Category.COMBAT);
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.ATTACK) return;
      this.setSuffix(this.mode.getValue());
      var player = mc.player;
      if (player == null) return;
      if (!player.onGround() || player.getMainHandItem().getItem() instanceof MaceItem) return;
      if (this.cooldownCheck.getValue() && player.getAttackStrengthScale(0.0F) < 0.8F) return;

      Vec3 pos = player.position();
      boolean horizontalCollision = player.horizontalCollision;
      double[] values = this.mode.is(LESS_PACKET_LABEL) ? this.lessPacketValues : this.packetValues;
      for (double critY : values) {
         mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y + critY, pos.z, false, horizontalCollision));
      }
   }
}
