package com.dioxidelite.module.modules.combat;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/combat/AutoTotem.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.util.player.InvHelper;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class AutoTotem extends Module {

    public static final AutoTotem INSTANCE = new AutoTotem();

    private final BooleanSetting strict = add(new BooleanSetting("Strict", true));
    private final DoubleSetting health = add(new DoubleSetting("Health", 16.0, 0.0, 36.0, 0.5));
    private final BooleanSetting checkGapple = add(new BooleanSetting("Check Gapple", true));

    private AutoTotem() {
        super("Auto Totem", Category.COMBAT);
    }

    @Override
    public String getInfo() {
        return noPlayer() ? null : Integer.toString(InvHelper.getItemCount(Items.TOTEM_OF_UNDYING));
    }

    @Listen
    private void onTick(PlayerTickEvent.Pre event) {
        if (noPlayer() || mc.gameMode == null || !shouldHoldTotem()) {
            return;
        }
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        int slot = InvHelper.getItemSlot(Items.TOTEM_OF_UNDYING);
        if (slot >= 0) {
            moveItemToOffhand(slot);
        }
    }

    private boolean shouldHoldTotem() {
        float totalHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (totalHealth <= health.get()) {
            return true;
        }
        if (mc.player.getOffhandItem().isEmpty() || mc.player.getY() < -64.0) {
            return true;
        }
        if (!checkGapple.get()) {
            return false;
        }

        Item mainHandItem = mc.player.getMainHandItem().getItem();
        return mainHandItem == Items.GOLDEN_APPLE || mainHandItem == Items.ENCHANTED_GOLDEN_APPLE;
    }

    private void moveItemToOffhand(int inventorySlot) {
        int containerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        int containerId = mc.player.inventoryMenu.containerId;

        if (!strict.get()) {
            mc.gameMode.handleContainerInput(
                    containerId, containerSlot, 40, ContainerInput.SWAP, mc.player);
            return;
        }

        mc.gameMode.handleContainerInput(
                containerId, containerSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(
                containerId, 45, 0, ContainerInput.PICKUP, mc.player);
        if (!mc.player.inventoryMenu.getCarried().isEmpty()) {
            mc.gameMode.handleContainerInput(
                    containerId, containerSlot, 0, ContainerInput.PICKUP, mc.player);
        }
    }
}
