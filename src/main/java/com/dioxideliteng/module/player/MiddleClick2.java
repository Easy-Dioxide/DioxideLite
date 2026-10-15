package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.event.impl.EventMouseButton;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.MiddleClick).
 *
 * Adaptations:
 *  - ListValue/IntValue mapped to ModeSetting/NumberSetting;
 *  - Krs onMouseClick (MouseClickEvent) -> Events.MOUSE_BUTTON (EventMouseButton);
 *  - Krs tag() -> setSuffix.
 */
public class MiddleClick2 extends Feature {
    private final ModeSetting itemMode = new ModeSetting("Item", this, "Pearl", new String[]{"Pearl", "Firework", "Wind Charge"});
    private final NumberSetting delay = new NumberSetting("Delay", this, 250, 0, 2000, 1);

    private final MSTimer useTimer = new MSTimer();

    public MiddleClick2() {
        super("MiddleClick2", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        useTimer.reset();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventMouseButton mouseEvent)) return;
        setSuffix(itemMode.getValue());

        var player = mc.player;
        var gameMode = mc.gameMode;
        if (player == null || gameMode == null || mc.gui.screen() != null
                || mouseEvent.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || mouseEvent.getAction() != GLFW.GLFW_PRESS
                || !useTimer.hasTimePassed((long) delay.getValue()))
            return;

        int slot = findItemSlot(targetItem());
        if (slot == -1)
            return;

        int oldSlot = player.getInventory().getSelectedSlot();
        if (slot != oldSlot) {
            player.getInventory().setSelectedSlot(slot);
            PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(slot));
        }

        if (gameMode.useItem(player, InteractionHand.MAIN_HAND).consumesAction())
            PacketUtil.sendPacket(ServerboundPunchPacket.INSTANCE);

        if (slot != oldSlot) {
            player.getInventory().setSelectedSlot(oldSlot);
            PacketUtil.sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
        }

        useTimer.reset();
        mouseEvent.setCancelled(true);
    }

    private Item targetItem() {
        return switch (itemMode.getValue().toLowerCase(Locale.ROOT)) {
            case "firework" -> Items.FIREWORK_ROCKET;
            case "wind charge" -> Items.WIND_CHARGE;
            default -> Items.ENDER_PEARL;
        };
    }

    private int findItemSlot(Item item) {
        var player = mc.player;
        if (player == null)
            return -1;

        for (int i = 0; i <= 8; i++) {
            if (player.getInventory().getItem(i).getItem() == item)
                return i;
        }
        return -1;
    }
}
