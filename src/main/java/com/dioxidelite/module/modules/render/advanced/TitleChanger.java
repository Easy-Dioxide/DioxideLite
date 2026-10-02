package com.dioxidelite.module.modules.render.advanced;

import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.TickEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.StringSetting;

/**
 * 移植自 OpenOpal TitleChangerModule。
 * 自定义 Minecraft 窗口标题。
 */
public final class TitleChanger extends Module {

    public static final TitleChanger INSTANCE = new TitleChanger();

    public final StringSetting title = add(new StringSetting("Title", "DioxideLite"));

    private TitleChanger() {
        super("Title Changer", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        applyTitle();
    }

    @Override
    protected void onDisable() {
        if (mc.getWindow() != null) {
            mc.getWindow().setTitle("Minecraft");
        }
    }

    @Listen
    private void onTick(TickEvent.Pre event) {
        applyTitle();
    }

    private void applyTitle() {
        if (mc.getWindow() == null) return;
        String value = title.get() == null ? "" : title.get().trim();
        mc.getWindow().setTitle(value.isEmpty() ? "Minecraft" : value);
    }
}
