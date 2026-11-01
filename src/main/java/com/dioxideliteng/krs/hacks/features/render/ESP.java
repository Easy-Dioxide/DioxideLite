package com.dioxideliteng.krs.hacks.features.render;

import com.dioxideliteng.event.impl.EventEntityOutline;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.player.Freecam;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.utils.value.BooleanValue;
import com.dioxideliteng.util.ClientColors;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * KRS player ESP.
 *
 * <p>KRS rendered the silhouette with a private GPU capture pipeline (model submission capture, depth reads, alpha masks).
 * That pipeline is not ported. The same effect - a coloured silhouette of players that shows through walls - comes from
 * vanilla's glow outline, which DioxideLiteNG already exposes as {@link EventEntityOutline}. The selection rules are
 * KRS's: players only, entities that already glow keep their own outline, and the local player is outlined only when
 * {@code Local} is on and the camera is third person or Freecam. The colour is DioxideLiteNG's theme colour, like the
 * native PlayerESP outline.
 */
public class ESP extends Module {
    @Setting
    private static final BooleanValue local = new BooleanValue("Local", true);

    public ESP() {
        super("ESP", ModuleCategory.Render, Keys.KEY_UNKNOWN, false, true);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    /** Called by the bridge for every entity DioxideLiteNG is about to extract for rendering. */
    public static void applyOutline(EventEntityOutline event) {
        // A non-zero colour means vanilla team glow or another module already chose an outline: leave it alone.
        if (event.getColor() != 0) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Player) || entity.isCurrentlyGlowing()) return;
        if (mc.player == null || mc.level == null) return;

        if (entity instanceof LocalPlayer
            && !(local.get() && (!mc.options.getCameraType().isFirstPerson() || ModuleManager.getModuleState(Freecam.class)))) {
            return;
        }

        event.setColor(ClientColors.colorAtOffset(0));
    }
}
