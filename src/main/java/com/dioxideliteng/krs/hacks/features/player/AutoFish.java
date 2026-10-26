package com.dioxideliteng.krs.hacks.features.player;



import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.utils.ChatUtil;
import com.dioxideliteng.krs.utils.math.MSTimer;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.krs.utils.value.ListValue;
import com.dioxideliteng.krs.utils.input.Keys;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class AutoFish extends Module {

    public AutoFish() {
        super("Auto Fish", ModuleCategory.Player, Keys.KEY_UNKNOWN, false, true);
    }

    @Setting
    private static final ListValue handMode = new ListValue("Interaction Hand Mode", new String[]{"Mainhand", "Offhand"}, "Mainhand");

    private final MSTimer fishTimer = new MSTimer();

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
    public void onUpdate(UpdateEvent event) {
        var player = mc.player;
        var gameMode = mc.gameMode;
        if (player == null || gameMode == null) return;

        String mode = handMode.get();
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

            KrsClient.notificationManager.addNotification(hook == null ? "Wait" : "Success", hook == null ? "Started fishing... (" + mode + ")" : "Fished (" + mode + ")");
            // The server broadcasts the swing for successful item use on its own since 26.3.
            gameMode.useItem(player, hand);
            fishTimer.reset();
        }
    }
}
