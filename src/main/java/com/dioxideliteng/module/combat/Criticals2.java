package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.impl.EventAttack;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.module.FeatureManager;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Criticals).
 * Notable adaptations vs the original:
 *  - ModuleManager.getModuleState(FlyModule.class) resolved through an instanceof
 *    lookup over the client movement.Flight module (same pattern as Scaffold2 /
 *    TargetStrafe); KillAura static reads resolved through FeatureManager.killAura2.
 *  - The Krs onSendPacket hook is an empty no-op in the original (dead code, zero
 *    functional effect) and is therefore not ported.
 *  - Krs tag() mapped to Feature#setSuffix, refreshed on attack like the Krs tag.
 * The crit packet sequences and the attack gate (onGround / mace / fly / killaura
 * tp reach / cooldown check) are byte-exact Krs parity.
 */
public class Criticals2 extends Feature {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Packet",
           new String[]{"Packet", "Less Packet"});

   private final BooleanSetting cooldownCheck = new BooleanSetting("Cooldown Check", this, false);

   private final double[] packetValues = new double[]{0.0625D, 0.0D, 0.05D, 0.0D};
   private final double[] lessPacketValues = new double[]{0.0625D, 0.0D};

   public Criticals2() {
      super("Criticals2", Category.COMBAT);
   }

   @Override
   public void onEvent(Event event) {
      if (event instanceof EventAttack attack) {
         onAttack(attack);
      }
   }

   private void onAttack(EventAttack event) {
      var player = mc.player;
      if (player == null) return;
      if (!player.onGround()
            || player.getMainHandItem().getItem() instanceof MaceItem
            || isFlyEnabled()
            || (isKillAuraEnabled()
            && FeatureManager.killAura2.closestEntity != null
            && (FeatureManager.killAura2.randomTp.getValue()
            || (FeatureManager.killAura2.tpReach.getValue()
            && mc.player.distanceTo(FeatureManager.killAura2.closestEntity) >= FeatureManager.killAura2.attackRange.getValue()))))
         return;
      if (cooldownCheck.getValue() && player.getAttackStrengthScale(0f) < 0.8) return;

      this.setSuffix(mode.getValue());

      Vec3 pos = player.position();
      boolean horizontalCollision = player.horizontalCollision;

      switch (mode.getValue().toLowerCase()) {
         case "packet":
            for (double critY : packetValues) {
               PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y + critY, pos.z, false, horizontalCollision));
            }
            break;

         case "less packet":
            for (double critY : lessPacketValues) {
               PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y + critY, pos.z, false, horizontalCollision));
            }
            break;
      }
   }

   private static boolean isFlyEnabled() {
      var modules = FeatureManager.getModules();
      if (modules == null) return false;
      for (var feature : modules) {
         if (feature instanceof com.dioxideliteng.module.movement.Flight)
            return feature.isEnabled();
      }
      return false;
   }

   private static boolean isKillAuraEnabled() {
      return FeatureManager.killAura2 != null && FeatureManager.killAura2.isEnabled();
   }
}
