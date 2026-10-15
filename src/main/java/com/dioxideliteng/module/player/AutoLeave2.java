package com.dioxideliteng.module.player;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.util.krs.MovementUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.player.AutoLeave).
 *
 * Adaptations:
 *  - ListValue/FloatValue/BooleanValue mapped to ModeSetting/NumberSetting/BooleanSetting;
 *  - Krs onUpdate -> Events.UPDATE; tag() -> setSuffix.
 */
public class AutoLeave2 extends Feature {
    private final ModeSetting mode = new ModeSetting("Mode", this, "Health", new String[]{"Health", "Void", "Both", "No Totem", "Smart"});
    private final NumberSetting health = new NumberSetting("Health", this, 4, 1, 20, 1);
    private final BooleanSetting multiplayerOnly = new BooleanSetting("Multiplayer Only", this, true);

    private boolean left = false;

    public AutoLeave2() {
        super("AutoLeave2", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        left = false;
    }

    @Override
    public void onDisable() {
        left = false;
    }

    @Override
    public void onEvent(Event event) {
        if (event != Events.UPDATE) return;
        setSuffix(mode.getValue());

        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null || left || multiplayerOnly.getValue() && mc.hasSingleplayerServer())
            return;

        boolean healthDanger = usesHealth() && player.getHealth() <= health.getValue();
        boolean voidDanger = usesVoid() && player.getY() < level.getMinY() - 8 && !MovementUtil.isBlockBelow();
        boolean totemDanger = usesTotem() && player.getHealth() <= health.getValue() && !hasTotem();
        if (!healthDanger && !voidDanger && !totemDanger)
            return;

        left = true;
        level.disconnect(Component.literal("Auto Leave"));
    }

    private boolean usesHealth() {
        return mode.is("Health") || mode.is("Both") || mode.is("Smart");
    }

    private boolean usesVoid() {
        return mode.is("Void") || mode.is("Both") || mode.is("Smart");
    }

    private boolean usesTotem() {
        return mode.is("No Totem") || mode.is("Smart");
    }

    private boolean hasTotem() {
        var player = mc.player;
        if (player == null)
            return false;

        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING)
            return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING)
                return true;
        }
        return false;
    }
}
