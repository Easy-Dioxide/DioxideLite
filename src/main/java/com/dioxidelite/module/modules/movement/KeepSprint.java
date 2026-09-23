package com.dioxidelite.module.modules.movement;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（上游开源版）com/dioxidelite/module/modules/movement/KeepSprint.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.KeyboardInputEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.EnumSetting;

public final class KeepSprint extends Module {

    public static final KeepSprint INSTANCE = new KeepSprint();

    public enum Mode {
        Normal
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Normal));

    private KeepSprint() {
        super("Keep Sprint", Category.MOVEMENT);
    }

    @Override
    public String getInfo() {
        return mode.displayValue();
    }

    @Listen
    private void onKeyboardInput(KeyboardInputEvent event) {
        if (mode.is(Mode.Normal)) {
            event.setSprint(true);
        }
    }
}
