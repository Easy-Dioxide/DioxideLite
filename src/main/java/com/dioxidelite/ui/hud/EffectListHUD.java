package com.dioxidelite.ui.hud;

import com.dioxidelite.event.events.Render2DEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.render.SkijaUi;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * modern potion HUD, ported from the reference client's PotionHUD.
 * Default: left side at 45% height, icons on, amplifier as roman numeral,
 * potion color tint on each row.
 */
public final class EffectListHUD extends EpsilonHudModule {

    public static final EffectListHUD INSTANCE = new EffectListHUD();

    public enum Anchor { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

    public final DoubleSetting scale = add(new DoubleSetting("Scale", 0.85, 0.5, 2.0, 0.05));
    public final EnumSetting<Anchor> anchor = add(new EnumSetting<>("Anchor", Anchor.TOP_LEFT));
    public final BooleanSetting icons = add(new BooleanSetting("Icons", true));
    public final BooleanSetting duration = add(new BooleanSetting("Duration", false));
    public final BooleanSetting amplifier = add(new BooleanSetting("Amplifier", true));
    public final BooleanSetting potionColor = add(new BooleanSetting("Potion Color", true));
    public final BooleanSetting lowTimeWarning = add(new BooleanSetting("Low Time Warning", true));
    public final BooleanSetting hideAmbient = add(new BooleanSetting("Hide Ambient", false));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final BooleanSetting sortByDuration = add(new BooleanSetting("Sort by Duration", false));
    public final BooleanSetting hideInfinite = add(new BooleanSetting("Hide Infinite", true));

    private static final float ROW_H = 20.0f;
    private static final float ROW_W = 80.0f;
    private static final float GAP = 2.0f;

    private EffectListHUD() {
        super("Potion List", Category.HUD, 7, 45, ROW_W, ROW_H * 4);
        setEnabled(true);
    }

    @Override
    protected void renderHud(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { updateBounds(4, 4); return; }

        float s = scale.get().floatValue();
        float rowW = ROW_W * s;
        float rowH = ROW_H * s;
        float gap = GAP * s;

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        if (hideAmbient.get()) {
            effects.removeIf(MobEffectInstance::isAmbient);
        }
        if (hideInfinite.get()) {
            effects.removeIf(e -> e.getDuration() >= 1_000_000);
        }
        if (effects.isEmpty()) { updateBounds(4, 4); return; }

        if (sortByDuration.get()) {
            effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        }

        updateBounds(rowW, effects.size() * (rowH + gap));

        float x = switch (anchor.get()) {
            case TOP_RIGHT, BOTTOM_RIGHT -> event.width() - rowW - 4.0f;
            case TOP_LEFT, BOTTOM_LEFT -> 4.0f;
        };
        float y = switch (anchor.get()) {
            case TOP_RIGHT, TOP_LEFT -> 4.0f;
            case BOTTOM_RIGHT, BOTTOM_LEFT -> event.height() - effects.size() * (rowH + gap) - 4.0f;
        };

        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance inst = effects.get(i);
            MobEffect effect = inst.getEffect().value();
            float ry = y + i * (rowH + gap);

            // Background
            if (background.get()) {
                SkijaUi.rounded(event.canvas(), x, ry, rowW, rowH, 3.0f * s,
                        withAlpha(0xC814141C, opacityAlpha()));
            }

            // Potion color strip on left
            if (potionColor.get()) {
                int tint = effect.getColor();
                SkijaUi.rounded(event.canvas(), x, ry, 2.0f * s, rowH, 1.0f * s,
                        withAlpha(tint, opacityAlpha()));
            }

            // Name
            String name = effect.getDisplayName().getString();
            if (amplifier.get() && inst.getAmplifier() > 0) {
                name += " " + roman(inst.getAmplifier() + 1);
            }

            float nameSize = 8.5f * s;
            float textX = x + 5.0f * s;
            String display = name;
            float maxW = rowW - 10.0f * s;
            if (SkijaUi.boldTextWidth(display, nameSize) > maxW) {
                while (display.length() > 1 && SkijaUi.boldTextWidth(display + "…", nameSize) > maxW) {
                    display = display.substring(0, display.length() - 1);
                }
                display += "…";
            }
            SkijaUi.boldText(event.canvas(), display, textX, ry + 2.0f * s,
                    nameSize, withAlpha(0xFFFFFFFF, opacityAlpha()));

            // Duration
            if (duration.get()) {
                int dur = inst.getDuration();
                String durStr;
                if (dur >= 1_000_000) {
                    durStr = "**:**";
                } else {
                    long seconds = dur / 20;
                    durStr = String.format("%d:%02d", seconds / 60, seconds % 60);
                }
                float durSize = 7.5f * s;
                int durColor = 0xFFB0B0C0;
                if (lowTimeWarning.get() && dur < 200 && dur < 1_000_000) {
                    durColor = 0xFFFF6B6B;
                }
                SkijaUi.text(event.canvas(), durStr, textX, ry + rowH - 9.0f * s,
                        durSize, withAlpha(durColor, opacityAlpha()));
            }
        }
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
            case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }
}
