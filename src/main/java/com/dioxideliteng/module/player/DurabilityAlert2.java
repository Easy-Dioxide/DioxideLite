package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.DurabilityAlert).
 *
 * Adaptations:
 *  - IntValue/BooleanValue mapped to NumberSetting/BooleanSetting;
 *  - {@code Client.notificationManager.addNotification} replaced with DynamicIslandManager.notifyInfo;
 *  - Krs onUpdate -> Events.UPDATE; Krs onWorld -> Events.WORLD; tag() -> setSuffix.
 */
public class DurabilityAlert2 extends Feature {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] HAND_SLOTS = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };

    private final NumberSetting threshold = new NumberSetting("Threshold", this, 15, 1, 50, 1);
    private final NumberSetting repeatDelay = new NumberSetting("Repeat Delay", this, 30, 0, 300, 1);
    private final BooleanSetting armor = new BooleanSetting("Armor", this, true);
    private final BooleanSetting hands = new BooleanSetting("Hands", this, true);

    private final EnumMap<EquipmentSlot, SlotState> slotStates = new EnumMap<>(EquipmentSlot.class);
    private int checkTicks;

    public DurabilityAlert2() {
        super("DurabilityAlert2", Category.PLAYER);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.UPDATE) {
            setSuffix((int) threshold.getValue() + "%");
            onUpdate();
        } else if (event == Events.WORLD) {
            reset();
        }
    }

    private void onUpdate() {
        if (mc.player == null) {
            reset();
            return;
        }

        if (++checkTicks < 5)
            return;
        checkTicks = 0;

        if (armor.getValue()) {
            for (EquipmentSlot slot : ARMOR_SLOTS)
                checkSlot(slot);
        } else {
            for (EquipmentSlot slot : ARMOR_SLOTS)
                slotStates.remove(slot);
        }

        if (hands.getValue()) {
            for (EquipmentSlot slot : HAND_SLOTS)
                checkSlot(slot);
        } else {
            for (EquipmentSlot slot : HAND_SLOTS)
                slotStates.remove(slot);
        }
    }

    private void checkSlot(EquipmentSlot slot) {
        ItemStack stack = mc.player.getItemBySlot(slot);
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            slotStates.remove(slot);
            return;
        }

        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        int remainingPercent = (int) Math.ceil(remaining * 100.0 / stack.getMaxDamage());
        SlotState state = slotStates.get(slot);
        if (state == null || state.item != stack.getItem() || state.maxDamage != stack.getMaxDamage()) {
            state = new SlotState(stack.getItem(), stack.getMaxDamage());
            slotStates.put(slot, state);
        }

        if (remainingPercent > Math.min(100, (int) threshold.getValue() + 5)) {
            state.warned = false;
            state.lastAlertNanos = 0L;
        }

        long now = System.nanoTime();
        long repeatNanos = (long) repeatDelay.getValue() * 1_000_000_000L;
        boolean repeatReady = repeatDelay.getValue() > 0
                && state.lastAlertNanos > 0L
                && now - state.lastAlertNanos >= repeatNanos;
        if (remainingPercent <= (int) threshold.getValue() && (!state.warned || repeatReady)) {
            String message = slotName(slot) + " " + stack.getHoverName().getString()
                    + ": " + remaining + " durability (" + remainingPercent + "%)";
            DynamicIslandManager.notifyInfo("Low Durability", message);
            state.warned = true;
            state.lastAlertNanos = now;
        }
    }

    private String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "Helmet";
            case CHEST -> "Chest";
            case LEGS -> "Leggings";
            case FEET -> "Boots";
            case MAINHAND -> "Main hand";
            case OFFHAND -> "Offhand";
            default -> slot.getName();
        };
    }

    private void reset() {
        slotStates.clear();
        checkTicks = 0;
    }

    private static final class SlotState {
        private final Item item;
        private final int maxDamage;
        private boolean warned;
        private long lastAlertNanos;

        private SlotState(Item item, int maxDamage) {
            this.item = item;
            this.maxDamage = maxDamage;
        }
    }
}
