package com.dioxidelite.module.modules.render;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.setting.settings.EnumSetting;

/**
 * Dynamic Island presentation controller.
 *
 * <p>The island is a presentation surface only.  The style selector deliberately
 * lives on the module so it is persisted by the normal DioxideLite config and is
 * therefore available from ClickGUI without introducing a second settings store.</p>
 */
public final class DioxideIslandModule extends Module {

    public static final DioxideIslandModule INSTANCE = new DioxideIslandModule();

    public enum Style {
        DIOXIDE,
        OPAI_ONYX,
        ONYX_MINIMAL,
        ONYX_GLASS
    }

    public final EnumSetting<Style> style =
            add(new EnumSetting<>("Island Style", Style.DIOXIDE));

    public final BooleanSetting musicLyrics =
            add(new BooleanSetting("Music Lyrics", true));

    public final BooleanSetting onyxNotifications =
            add(new BooleanSetting("Onyx Notifications", true));

    private DioxideIslandModule() {
        super("Dynamic Island", Category.RENDER);
        setEnabled(true);
    }
}
