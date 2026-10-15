package com.dioxideliteng.module.combat;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.util.krs.EntityExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Teams).
 * Krs keeps the settings and the isInClientPlayersTeam check static; the client
 * requires per-instance settings, so the static entry points resolve the
 * registered module instance (same pattern as TPStealer/WTap static hooks).
 * The instance isInTeam adapter is what the shared BehaviorUtils calls,
 * mirroring the Krs BehaviorUtils.isTeammate gate (module enabled + player check).
 */
public class Teams extends Feature {
   private static Teams instance;

   private final BooleanSetting scoreboardTeam = new BooleanSetting("Scoreboard Team", this, true);
   private final BooleanSetting nameColor = new BooleanSetting("Name Color", this, false);
   private final BooleanSetting prefix = new BooleanSetting("Prefix", this, false);
   private final BooleanSetting armorColor = new BooleanSetting("Armor Color", this, false);
   private final BooleanSetting helmet = new BooleanSetting("Helmet", this, true);
   private final BooleanSetting chestPlate = new BooleanSetting("Chest Plate", this, true);
   private final BooleanSetting leggings = new BooleanSetting("Leggings", this, true);
   private final BooleanSetting boots = new BooleanSetting("Boots", this, true);

   public Teams() {
      super("Teams", Category.COMBAT);
      instance = this;
      this.helmet.setVisible(this.armorColor::getValue);
      this.chestPlate.setVisible(this.armorColor::getValue);
      this.leggings.setVisible(this.armorColor::getValue);
      this.boots.setVisible(this.armorColor::getValue);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   public boolean isInTeam(Entity entity) {
      if (!(entity instanceof LivingEntity livingEntity)) return false;
      return isInClientPlayersTeam(livingEntity);
   }

   public static boolean isInClientPlayersTeam(LivingEntity entity) {
      var player = mc.player;
      if (player == null) return false;

      Teams teams = instance;
      if (teams == null) return false;

      if (teams.scoreboardTeam.getValue() && player.isAlliedTo(entity))
         return true;

      Component clientDisplayName = player.getDisplayName();
      Component targetDisplayName = entity.getDisplayName();

      return teams.nameColor.getValue() && checkName(clientDisplayName, targetDisplayName)
              || teams.prefix.getValue() && checkPrefix(targetDisplayName, clientDisplayName)
              || teams.armorColor.getValue() && checkArmor(player, entity);
   }

   private static boolean checkName(Component clientDisplayName, Component targetDisplayName) {
      var targetColor = clientDisplayName.getStyle().getColor();
      var clientColor = targetDisplayName.getStyle().getColor();
      return targetColor != null && clientColor != null && targetColor.equals(clientColor);
   }

   private static boolean checkPrefix(Component targetDisplayName, Component clientDisplayName) {
      String targetName = EntityExtension.stripMinecraftColorCodes(targetDisplayName.getString());
      String clientName = EntityExtension.stripMinecraftColorCodes(clientDisplayName.getString());
      int targetSeparator = targetName.indexOf(' ');
      int clientSeparator = clientName.indexOf(' ');

      return targetSeparator > 0
              && clientSeparator > 0
              && targetName.regionMatches(0, clientName, 0, targetSeparator)
              && targetSeparator == clientSeparator;
   }

   private static boolean checkArmor(Player ownPlayer, LivingEntity entity) {
      if (!(entity instanceof Player player)) return false;

      Teams teams = instance;
      if (teams == null) return false;

      return teams.helmet.getValue() && matchesArmorColor(ownPlayer, player, EquipmentSlot.HEAD)
              || teams.chestPlate.getValue() && matchesArmorColor(ownPlayer, player, EquipmentSlot.CHEST)
              || teams.leggings.getValue() && matchesArmorColor(ownPlayer, player, EquipmentSlot.LEGS)
              || teams.boots.getValue() && matchesArmorColor(ownPlayer, player, EquipmentSlot.FEET);
   }

   private static boolean matchesArmorColor(Player ownPlayer, Player player, EquipmentSlot slot) {
      Integer ownColor = EntityExtension.getArmorColor(ownPlayer.getItemBySlot(slot));
      if (ownColor == null) return false;
      Integer otherColor = EntityExtension.getArmorColor(player.getItemBySlot(slot));
      if (otherColor == null) return false;
      return ownColor.equals(otherColor);
   }
}
