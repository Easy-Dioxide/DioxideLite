package com.dioxidelite.module.modules.render;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;

/** First-person combat presentation only; contains no combat automation. */
public final class CombatVisuals extends Module {
    public static final CombatVisuals INSTANCE = new CombatVisuals();
    public final BooleanSetting swordSwing = add(new BooleanSetting("Sword Swing Animation", true));
    public final DoubleSetting swingStrength = add(new DoubleSetting("Swing Strength", 0.9, 0.0, 1.5, 0.05)
            .visibleWhen(swordSwing::get));
    public final BooleanSetting blockAnimation = add(new BooleanSetting("Block Animation", false));
    public final DoubleSetting blockOffset = add(new DoubleSetting("Block Offset", 1.0, 0.0, 1.5, 0.05)
            .visibleWhen(blockAnimation::get));
    private CombatVisuals() { super("Combat Visuals", Category.RENDER); }
}
