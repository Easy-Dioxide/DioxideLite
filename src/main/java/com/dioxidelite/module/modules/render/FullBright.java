package com.dioxidelite.module.modules.render;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.EnumSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Epsilon's lightmap/potion full-bright implementation. */
public final class FullBright extends Module {

    public static final FullBright INSTANCE = new FullBright();

    private enum Mode {
        Gamma,
        Potion
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Gamma)
            .onChange(value -> {
                if (value == Mode.Gamma && mc.player != null) {
                    mc.player.removeEffect(MobEffects.NIGHT_VISION);
                }
            }));

    private FullBright() {
        super("Fullbright", Category.RENDER);
    }

    public boolean isGammaMode() {
        return isEnabled() && mode.is(Mode.Gamma);
    }

    @Override
    protected void onDisable() {
        if (noPlayer() || mode.is(Mode.Gamma)) return;
        mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @Listen
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        if (mode.is(Mode.Potion)) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0));
        }
    }
}
