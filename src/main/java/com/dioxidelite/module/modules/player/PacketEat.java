package com.dioxidelite.module.modules.player;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/player/PacketEat.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PacketEvent;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Keeps the server-side use action active for food that may always be eaten. */
public final class PacketEat extends Module {

    public static final PacketEat INSTANCE = new PacketEat();

    private ItemStack activeFood = ItemStack.EMPTY;

    private PacketEat() {
        super("Packet Eat", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        activeFood = ItemStack.EMPTY;
    }

    @Listen
    private void onPostTick(PlayerTickEvent.Post event) {
        if (noPlayer()) {
            activeFood = ItemStack.EMPTY;
        } else if (mc.player.isUsingItem()) {
            activeFood = mc.player.getUseItem();
        }
    }

    @Listen
    private void onPacketSend(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundPlayerActionPacket packet)
                || packet.getAction() != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM
                || activeFood.isEmpty()) {
            return;
        }

        FoodProperties food = activeFood.get(DataComponents.FOOD);
        if (food != null && food.canAlwaysEat()) {
            event.cancel();
        }
    }
}
