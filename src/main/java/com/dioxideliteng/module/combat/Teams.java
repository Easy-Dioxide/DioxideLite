package com.dioxideliteng.module.combat;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Teams) and adapted. */
public class Teams extends Feature {
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
      this.helmet.setVisible(() -> this.armorColor.getValue());
      this.chestPlate.setVisible(() -> this.armorColor.getValue());
      this.leggings.setVisible(() -> this.armorColor.getValue());
      this.boots.setVisible(() -> this.armorColor.getValue());
   }

   public boolean isInTeam(LivingEntity entity) {
      var player = mc.player;
      if (player == null) return false;
      if (this.scoreboardTeam.getValue() && player.isAlliedTo(entity)) return true;
      Component clientDisplayName = player.getDisplayName();
      Component targetDisplayName = entity.getDisplayName();
      return this.nameColor.getValue() && checkName(clientDisplayName, targetDisplayName)
            || this.prefix.getValue() && checkPrefix(targetDisplayName, clientDisplayName)
            || this.armorColor.getValue() && checkArmor(player, entity);
   }

   private static boolean checkName(Component clientDisplayName, Component targetDisplayName) {
      var targetColor = clientDisplayName.getStyle().getColor();
      var clientColor = targetDisplayName.getStyle().getColor();
      return targetColor != null && clientColor != null && targetColor.equals(clientColor);
   }

   private static boolean checkPrefix(Component targetDisplayName, Component clientDisplayName) {
      String targetName = stripColorCodes(targetDisplayName.getString());
      String clientName = stripColorCodes(clientDisplayName.getString());
      int targetSeparator = targetName.indexOf(' ');
      int clientSeparator = clientName.indexOf(' ');
      return targetSeparator > 0
            && clientSeparator > 0
            && targetName.regionMatches(0, clientName, 0, targetSeparator)
            && targetSeparator == clientSeparator;
   }

   private boolean checkArmor(Player ownPlayer, LivingEntity entity) {
      if (!(entity instanceof Player player)) return false;
      return this.helmet.getValue() && matchesArmorSlot(ownPlayer, player, EquipmentSlot.HEAD)
            || this.chestPlate.getValue() && matchesArmorSlot(ownPlayer, player, EquipmentSlot.CHEST)
            || this.leggings.getValue() && matchesArmorSlot(ownPlayer, player, EquipmentSlot.LEGS)
            || this.boots.getValue() && matchesArmorSlot(ownPlayer, player, EquipmentSlot.FEET);
   }

   private static boolean matchesArmorSlot(Player ownPlayer, Player player, EquipmentSlot slot) {
      Integer ownColor = getArmorColor(ownPlayer.getItemBySlot(slot));
      if (ownColor == null) return false;
      Integer otherColor = getArmorColor(player.getItemBySlot(slot));
      if (otherColor == null) return false;
      return ownColor.equals(otherColor);
   }

   private static Integer getArmorColor(ItemStack stack) {
      var dyed = stack.get(DataComponents.DYED_COLOR);
      return dyed == null ? null : dyed.rgb();
   }

   private static String stripColorCodes(String value) {
      return value.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "");
   }
}
