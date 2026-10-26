package com.dioxideliteng.krs.hacks.features.level;

import com.dioxideliteng.krs.utils.entity.PlayerUtil;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.render.ViewModel;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import com.dioxideliteng.krs.utils.value.ListValue;
import net.minecraft.world.InteractionHand;
import com.dioxideliteng.krs.utils.input.Keys;

import java.util.Locale;

public class ItemDropChanger extends Module {

    @Setting
    public static final ListValue mode = new ListValue("Mode", new String[]{"Packet", "No Order"}, "No Order");

    public ItemDropChanger() {
        super("Item Drop Changer", ModuleCategory.Level, Keys.KEY_UNKNOWN, false, false);
    }

    public static boolean hookDropItemSwing(InteractionHand hand) {
        if (ModuleManager.getModuleState(ItemDropChanger.class)) {
            return switch (mode.get().toLowerCase(Locale.ROOT)) {
                case "packet" -> {
                    PlayerUtil.INSTANCE.sendSwingPacket(hand);
                    yield false;
                }
                case "no order" -> false;
                default -> true;
            };
        }
        return true;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }
}
