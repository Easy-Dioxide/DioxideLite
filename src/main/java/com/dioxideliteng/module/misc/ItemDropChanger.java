package com.dioxideliteng.module.misc;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.ModeSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.ItemDropChanger).
 * Krs parity: a mixin on the item-drop swing path calls hookDropItemSwing; when the
 * module is on and the mode is "Packet" a swing packet is sent to the server and the
 * local swing animation is suppressed ("no order" also suppresses the local swing
 * without sending anything, any other state falls through to vanilla).
 * Notable adaptations vs the original:
 *  - Krs @Setting static ListValue replaced with an instance ModeSetting plus a static
 *    instance reference, because DioxideLite settings are per-instance while the mixin
 *    hook is static (same pattern as Krs Xray/CaveFinder static hooks).
 *  - ServerboundSwingPacket no longer exists in 26.3; the packet send is replaced by
 *    player.swing(hand, SwingAnimation.DEFAULT, true), matching the Scaffold2 precedent.
 */
public class ItemDropChanger extends Feature {
    private static volatile ItemDropChanger instance;

    private final ModeSetting mode = new ModeSetting("Mode", this, "No Order",
            new String[]{"Packet", "No Order"});

    public ItemDropChanger() {
        super("Item Drop Changer", Category.MISC);
        this.instance = this;
    }

    /** @return true to keep the vanilla local swing, false to suppress it. */
    public static boolean hookDropItemSwing(InteractionHand hand) {
        ItemDropChanger self = instance;
        if (self == null || !self.isEnabled())
            return true;

        if (self.mode.is("Packet")) {
            if (mc.player != null)
                mc.player.swing(hand, SwingAnimation.DEFAULT, true);
            return false;
        }
        if (self.mode.is("No Order"))
            return false;

        return true;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
