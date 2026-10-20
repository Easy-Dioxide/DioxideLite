package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.MSTimer;
import com.dioxideliteng.util.krs.PacketUtil;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoFish).
 *
 * Adaptations:
 *  - static ListValue mapped to an instance ModeSetting (this client's settings are per-Feature);
 *  - {@code Client.notificationManager.addNotification} replaced with DynamicIslandManager.notifyInfo;
 *  - Krs onUpdate -> Events.UPDATE.
 */
public class AutoFish2 extends Feature {

    private final ModeSetting handMode = new ModeSetting("Interaction Hand Mode", this, "Mainhand", new String[]{"Mainhand", "Offhand"});

    private final MSTimer fishTimer = new MSTimer();

    public AutoFish2() {
        super("AutoFish2", Category.PLAYER);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    private static InteractionHand fishHand(String mode) {
        return mode.equalsIgnoreCase("offhand") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.UPDATE) return;

        var player = mc.player;
        var gameMode = mc.gameMode;
        if (player == null || gameMode == null) return;

        String mode = handMode.getValue();
        InteractionHand hand = fishHand(mode);
        if ((hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem()).getItem() != Items.FISHING_ROD)
            return;

        if (fishTimer.hasTimePassed(500L)) {
            var hook = player.fishing;
            boolean canFish = hook == null;
            if (!canFish) {
                Vec3 delta = hook.getDeltaMovement();
                canFish = delta.x == 0.0 && -0.2 >= delta.y && delta.z == 0.0;
            }

            if (!canFish)
                return;

            DynamicIslandManager.notifyInfo(hook == null ? "Wait" : "Success", hook == null ? "Started fishing... (" + mode + ")" : "Fished (" + mode + ")");
            if (gameMode.useItem(player, hand).consumesAction())
                PacketUtil.sendPacket(ServerboundPunchPacket.INSTANCE);
            fishTimer.reset();
        }
    }
}
