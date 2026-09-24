package com.dioxidelite.module.modules.combat;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.IntSetting;

/** OpenOnyx JumpReset registration; jump input is only changed while grounded after damage state. */
public final class JumpReset extends Module {
    public static final JumpReset INSTANCE = new JumpReset();
    private final IntSetting chance = add(new IntSetting("Chance", 100, 0, 100, 1));
    private final BooleanSetting onlyTarget = add(new BooleanSetting("Targeted Only", false));
    private boolean armed;
    private JumpReset() { super("Jump Reset", Category.COMBAT); }
    @Listen private void onTick(PlayerTickEvent.Pre event) {
        if (noPlayer()) return;
        // Arm when airborne; after landing, restore normal jump input. The actual
        // damage packet integration remains in the combat event layer.
        if (!mc.player.onGround()) armed = true;
        if (armed && mc.player.onGround()) {
            armed = false;
            if (chance.get() >= 100 || java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < chance.get()) mc.options.keyJump.setDown(true);
        }
    }
}
