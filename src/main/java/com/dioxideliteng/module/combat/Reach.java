package com.dioxideliteng.module.combat;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Ported from Krs (com.instrumentalist.krs.hacks.features.combat.Reach) and adapted to use the player interaction-range attributes. */
public class Reach extends Feature {
   private final BooleanSetting block = new BooleanSetting("Block", this, true);
   private final NumberSetting blockReach = new NumberSetting("Block Reach", this, 6.0, 0.1, 6.0, 0.1);
   private final BooleanSetting entity = new BooleanSetting("Entity", this, true);
   private final NumberSetting entityReach = new NumberSetting("Entity Reach", this, 6.0, 0.1, 6.0, 0.1);

   private double originalBlockBase = -1.0;
   private double originalEntityBase = -1.0;

   public Reach() {
      super("Reach", Category.COMBAT);
   }

   @Override
   public void onEnable() {
      var player = mc.player;
      if (player == null) return;
      AttributeInstance blockAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      AttributeInstance entityAttr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
      this.originalBlockBase = blockAttr == null ? -1.0 : blockAttr.getBaseValue();
      this.originalEntityBase = entityAttr == null ? -1.0 : entityAttr.getBaseValue();
      applyReach();
   }

   @Override
   public void onDisable() {
      var player = mc.player;
      if (player == null) return;
      AttributeInstance blockAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      AttributeInstance entityAttr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
      if (blockAttr != null && this.originalBlockBase >= 0.0) blockAttr.setBaseValue(this.originalBlockBase);
      if (entityAttr != null && this.originalEntityBase >= 0.0) entityAttr.setBaseValue(this.originalEntityBase);
      this.originalBlockBase = -1.0;
      this.originalEntityBase = -1.0;
   }

   @Override
   public void onEvent(Event event) {
      if (event != Events.TICK) return;
      applyReach();
   }

   private void applyReach() {
      var player = mc.player;
      if (player == null) return;
      AttributeInstance blockAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      AttributeInstance entityAttr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
      if (this.block.getValue() && blockAttr != null) blockAttr.setBaseValue(this.blockReach.getValue());
      if (this.entity.getValue() && entityAttr != null) entityAttr.setBaseValue(this.entityReach.getValue());
   }
}
