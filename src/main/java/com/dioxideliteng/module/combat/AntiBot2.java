package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventPacketReceive;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.AntiBot) and adapted. */
public class AntiBot2 extends Feature {
   private static final String ADVANCED_LABEL = "Advanced";
   private static final String HYPIXEL_LABEL = "Hypixel";
   private static final String CUBECRAFT_LABEL = "Cubecraft";
   private static final String CUBECRAFT_BEDROCK_LABEL = "Cubecraft Bedrock";
   private static final String SHOTBOW_LABEL = "Shotbow";

   private final ModeSetting mode = new ModeSetting("Mode", this, ADVANCED_LABEL,
         new String[]{ADVANCED_LABEL, HYPIXEL_LABEL, CUBECRAFT_LABEL, CUBECRAFT_BEDROCK_LABEL, SHOTBOW_LABEL});

   private final Set<UUID> suspectList = new HashSet<>();
   private final Set<UUID> botList = new HashSet<>();
   private final Set<UUID> onlineIds = new HashSet<>();
   private final Set<UUID> listedProfiles = new HashSet<>();
   private final HashMap<UUID, RotationState> rotationTracker = new HashMap<>();
   private final HashSet<UUID> damagedEntities = new HashSet<>();

   private int armorTimer;
   private boolean armorChecker;

   public AntiBot2() {
      super("AntiBot2", Category.COMBAT);
   }

   @Override
   public void onDisable() {
      reset();
   }

   @Override
   public void onEvent(Event event) {
      if (event == Events.TICK) {
         onUpdate();
         onTick();
      } else if (event instanceof EventPacketReceive receiving && !receiving.isCancelled()) {
         onReceivedPacket(receiving);
      } else if (event == Events.WORLD) {
         reset();
      }
   }

   private void reset() {
      this.botList.clear();
      this.suspectList.clear();
      this.onlineIds.clear();
      this.listedProfiles.clear();
      this.rotationTracker.clear();
      this.damagedEntities.clear();
      this.armorTimer = 0;
      this.armorChecker = false;
   }

   private void onUpdate() {
      if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
      String current = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (current.equals(HYPIXEL_LABEL.toLowerCase(Locale.ROOT))) {
         this.listedProfiles.clear();
         for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            this.listedProfiles.add(entry.getProfile().id());
         }
         for (Player player : mc.level.players()) {
            UUID profile = player.getGameProfile().id();
            if (profile == null) continue;
            if (!this.listedProfiles.contains(profile)) this.botList.add(profile);
            else this.botList.remove(profile);
         }
      } else if (current.equals(ADVANCED_LABEL.toLowerCase(Locale.ROOT))) {
         this.onlineIds.clear();
         for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            this.onlineIds.add(entry.getProfile().id());
         }
      }
   }

   private void onTick() {
      var player = mc.player;
      var level = mc.level;
      if (player == null || level == null) return;
      String current = this.mode.getValue().toLowerCase(Locale.ROOT);
      if (current.equals(SHOTBOW_LABEL.toLowerCase(Locale.ROOT))) {
         if (this.suspectList.isEmpty()) return;
         for (Player entity : level.players()) {
            if (!this.suspectList.contains(entity.getUUID())) continue;
            List<ItemStack> armor = null;
            if (!isFullyArmored(entity)) {
               armor = armorSlots(entity);
               this.armorChecker = true;
            }
            if (this.armorChecker) {
               this.armorTimer++;
               if (this.armorTimer >= 2) {
                  if ((isFullyArmored(entity) || updatesArmor(entity, armor)) && entity.getGameProfile().properties().isEmpty()) {
                     this.botList.add(entity.getUUID());
                  }
                  this.suspectList.remove(entity.getUUID());
                  this.armorTimer = 0;
                  this.armorChecker = false;
               }
            }
         }
      } else if (current.equals(CUBECRAFT_BEDROCK_LABEL.toLowerCase(Locale.ROOT))) {
         for (Player entity : level.players()) {
            if (entity == player) continue;
            UUID uuid = entity.getUUID();
            if (entity.hurtTime > 0) this.damagedEntities.add(uuid);
            if (this.damagedEntities.contains(uuid)) {
               this.botList.remove(uuid);
               this.rotationTracker.remove(uuid);
               continue;
            }
            float currentYaw = entity.getYRot();
            RotationState state = this.rotationTracker.computeIfAbsent(uuid, ignored -> new RotationState(currentYaw));
            if (Math.abs(currentYaw - state.lastYaw) > 1.0F) {
               state.movingTimer++;
               state.stillTimer = 0;
            } else {
               state.stillTimer++;
            }
            state.lastYaw = currentYaw;
            if (state.stillTimer >= 20) {
               this.botList.remove(uuid);
               this.rotationTracker.remove(uuid);
               continue;
            }
            if (state.movingTimer >= 150) {
               this.botList.add(uuid);
               this.rotationTracker.remove(uuid);
            }
         }
      }
   }

   private void onReceivedPacket(EventPacketReceive receiving) {
      if (mc.player == null || mc.getConnection() == null) return;
      var packet = receiving.getPacket();
      if (!this.mode.getValue().equalsIgnoreCase(SHOTBOW_LABEL)) return;
      if (packet instanceof ClientboundPlayerInfoUpdatePacket updatePacket) {
         for (var entry : updatePacket.entries()) {
            GameProfile profile = entry.profile();
            if (profile == null) continue;
            if (entry.latency() < 2 || !profile.properties().isEmpty() || isGameProfileUnique(profile)) continue;
            if (isDuplicated(profile)) {
               this.botList.add(entry.profileId());
               continue;
            }
            this.suspectList.add(entry.profileId());
         }
      } else if (packet instanceof ClientboundPlayerInfoRemovePacket removePacket) {
         for (UUID uuid : removePacket.profileIds()) {
            this.suspectList.remove(uuid);
            this.botList.remove(uuid);
         }
      }
   }

   private boolean isGameProfileUnique(GameProfile originalProfile) {
      int matches = 0;
      for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
         if (entry.getProfile().name().equals(originalProfile.name()) && entry.getProfile().id().equals(originalProfile.id())) {
            matches++;
            if (matches > 1) return false;
         }
      }
      return matches == 1;
   }

   private boolean isDuplicated(GameProfile originalProfile) {
      for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
         if (entry.getProfile().name().equals(originalProfile.name()) && !entry.getProfile().id().equals(originalProfile.id())) {
            return true;
         }
      }
      return false;
   }

   public boolean isBot(LivingEntity playerEntity) {
      if (!this.isEnabled()) return false;
      String current = this.mode.getValue();
      UUID uuid = playerEntity.getUUID();
      ClientPacketListener connection = mc.getConnection();
      if (this.botList.contains(uuid)) return true;
      if (current.equalsIgnoreCase(ADVANCED_LABEL) && connection != null) {
         return !this.onlineIds.contains(uuid);
      }
      if (current.equalsIgnoreCase(CUBECRAFT_LABEL)) {
         return playerEntity.getAttributes().getValue(Attributes.SCALE) <= 0.4F;
      }
      return false;
   }

   private static boolean isFullyArmored(Player entity) {
      return isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.HEAD))
            && isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.CHEST))
            && isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.LEGS))
            && isEnchantedArmor(entity.getItemBySlot(EquipmentSlot.FEET));
   }

   private static boolean isEnchantedArmor(ItemStack stack) {
      return stack.has(DataComponents.EQUIPPABLE) && stack.isEnchanted();
   }

   private static boolean updatesArmor(Player entity, java.util.List<ItemStack> prevArmor) {
      if (prevArmor == null) return true;
      var previousIterator = prevArmor.iterator();
      return isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.HEAD)
            || isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.CHEST)
            || isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.LEGS)
            || isUpdatedArmorSlot(entity, previousIterator, EquipmentSlot.FEET)
            || previousIterator.hasNext();
   }

   private static boolean isUpdatedArmorSlot(Player entity, java.util.Iterator<ItemStack> previousIterator, EquipmentSlot slot) {
      return !previousIterator.hasNext() || !previousIterator.next().equals(entity.getItemBySlot(slot));
   }

   private static java.util.ArrayList<ItemStack> armorSlots(LivingEntity entity) {
      var armor = new java.util.ArrayList<ItemStack>(4);
      armor.add(entity.getItemBySlot(EquipmentSlot.HEAD));
      armor.add(entity.getItemBySlot(EquipmentSlot.CHEST));
      armor.add(entity.getItemBySlot(EquipmentSlot.LEGS));
      armor.add(entity.getItemBySlot(EquipmentSlot.FEET));
      return armor;
   }

   private static final class RotationState {
      private float lastYaw;
      private int movingTimer;
      private int stillTimer;

      private RotationState(float lastYaw) {
         this.lastYaw = lastYaw;
      }
   }
}
