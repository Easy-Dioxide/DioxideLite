package com.dioxideliteng.krs.hacks.features.level;



import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.events.features.UpdateEvent;
import com.dioxideliteng.krs.events.features.WorldEvent;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleCategory;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.nulling.PluginsDetector;
import com.dioxideliteng.krs.utils.input.Keys;

import java.util.List;

public class AntiCheatDetector extends Module {

    public AntiCheatDetector() {
        super("Anti Cheat Detector", ModuleCategory.Level, Keys.KEY_UNKNOWN, false, false);
    }

    public static boolean shouldNotify = false;

    private static String getAcStrings() {
        List<String> antiCheats = PluginsDetector.getAntiCheats();

        if (antiCheats == null) {
            if (!ModuleManager.plChecked)
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
    public void onWorld(WorldEvent event) {
        shouldNotify = false;
    }

    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.level == null) {
            shouldNotify = false;
            return;
        }

        if (shouldNotify && ModuleManager.plChecked) {
            KrsClient.notificationManager.addNotification("Anti Cheat Detector", getAcStrings());
            shouldNotify = false;
        }
    }
}
