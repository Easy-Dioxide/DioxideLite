package com.dioxidelite.module.modules.render;

import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;

/** Master switch for the Dynamic Island visual in the ClickGUI Render category. */
public final class DioxideIslandModule extends Module {

    public static final DioxideIslandModule INSTANCE = new DioxideIslandModule();

    private DioxideIslandModule() {
        super("Dynamic Island", Category.RENDER);
    }
}
