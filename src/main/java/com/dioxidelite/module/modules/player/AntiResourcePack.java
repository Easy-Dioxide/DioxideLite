package com.dioxidelite.module.modules.player;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/player/AntiResourcePack.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.DioxideLite;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

import java.util.function.Consumer;

/** Spoofs a successful server resource-pack load without downloading the pack. */
public final class AntiResourcePack extends Module {

    public static final AntiResourcePack INSTANCE = new AntiResourcePack();

    private AntiResourcePack() {
        super("AntiResourcePack", Category.PLAYER);
    }

    public boolean handlePush(ClientboundResourcePackPushPacket packet,
                              Consumer<ServerboundResourcePackPacket> sender) {
        if (!isEnabled()) {
            return false;
        }

        sender.accept(new ServerboundResourcePackPacket(
                packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
        sender.accept(new ServerboundResourcePackPacket(
                packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        DioxideLite.LOGGER.info("Blocked server resource pack: {}", packet.id());
        return true;
    }
}
