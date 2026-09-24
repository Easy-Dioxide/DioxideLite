package com.dioxidelite.module.modules.player;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;

/** OpenOnyx FastPlace compatibility module; the cooldown hook is kept isolated in the client layer. */
public final class FastPlace extends Module {
    public static final FastPlace INSTANCE = new FastPlace();
    public final BooleanSetting place = add(new BooleanSetting("Place", true));
    public final BooleanSetting interact = add(new BooleanSetting("Interact", true));
    public final BooleanSetting jump = add(new BooleanSetting("Jump", false));
    private FastPlace() { super("Fast Place", Category.PLAYER); }
    @Listen private void onTick(PlayerTickEvent.Pre event) {
        // The actual vanilla right-click delay is version-sensitive in 26.1.2.
        // Keep this module registered without touching private Minecraft fields.
    }
}
