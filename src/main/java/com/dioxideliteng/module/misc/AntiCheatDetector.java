package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.ui.dynamicIsland.DynamicIslandManager;
import com.dioxideliteng.util.krs.PluginsDetector;
import java.util.List;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.AntiCheatDetector).
 * Krs parity: once the plugin scan has finished (plChecked), the detected
 * anticheats are surfaced as a notification — "Checking..." while the scan is
 * still running, "No plugins found" / "No acs found" when the scan came back
 * empty, or the comma-joined anticheat names. The notification is re-armed on
 * world change and while the player/level is missing.
 * Notable adaptations vs the original:
 *  - ModuleManager.plChecked replaced with the ported PluginsDetector.plChecked
 *    (Krs kept that flag on ModuleManager; the static core of PluginsDetector is
 *    ported under util/krs).
 *  - Client.notificationManager.addNotification replaced with
 *    DynamicIslandManager.notifyInfo.
 *  - Krs WorldEvent replaced with Events.WORLD, UpdateEvent with Events.UPDATE.
 *  - Deviation: Krs re-arms shouldNotify from ModuleManager on world join; that
 *    hook belongs to the nulling-category wiring and is not ported here.
 */
public class AntiCheatDetector extends Feature {
    public static boolean shouldNotify = false;

    public AntiCheatDetector() {
        super("Anti Cheat Detector", Category.MISC);
    }

    private static String getAcStrings() {
        List<String> antiCheats = PluginsDetector.getAntiCheats();

        if (antiCheats == null) {
            if (!PluginsDetector.plChecked)
                return "Checking...";
            else if (PluginsDetector.plugins == null || PluginsDetector.plugins.length == 0)
                return "No plugins found";
            else if (PluginsDetector.detectedAcs == null)
                return "No acs found";
        } else if (!antiCheats.isEmpty()) {
            return String.join(", ", antiCheats);
        }

        return "";
    }

    @Override
    public void onDisable() {
        shouldNotify = false;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.WORLD) {
            shouldNotify = false;
        } else if (event == Events.UPDATE) {
            if (mc.player == null || mc.level == null) {
                shouldNotify = false;
                return;
            }

            if (shouldNotify && PluginsDetector.plChecked) {
                DynamicIslandManager.notifyInfo("Anti Cheat Detector", getAcStrings());
                shouldNotify = false;
            }
        }
    }
}
