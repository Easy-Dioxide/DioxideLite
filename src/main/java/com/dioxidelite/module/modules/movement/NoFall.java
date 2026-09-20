package com.dioxidelite.module.modules.movement;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/module/modules/movement/NoFall.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.event.Listen;
import com.dioxidelite.event.events.KeyboardInputEvent;
import com.dioxidelite.event.events.SendPositionEvent;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.DoubleSetting;
import com.dioxidelite.setting.settings.EnumSetting;

public final class NoFall extends Module {

    public static final NoFall INSTANCE = new NoFall();

    private enum Mode {
        NCP,
        GroundSpoof,
        GrimSimulation
    }

    private final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.NCP));
    private final DoubleSetting fallDistance = add(new DoubleSetting(
            "Fall Distance", 3.0, 3.0, 16.0, 1.0));

    private boolean falling;
    private boolean simulateJump;

    private NoFall() {
        super("No Fall", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        resetState();
    }

    @Override
    public String getInfo() {
        return mode.get().name();
    }

    @Listen
    private void onSendPosition(SendPositionEvent event) {
        if (noPlayer()) {
            resetState();
            return;
        }

        if (mc.player.fallDistance > fallDistance.get()) {
            falling = true;
        }

        if (falling && mc.player.onGround()) {
            switch (mode.get()) {
                case NCP, GroundSpoof -> event.setOnGround(true);
                case GrimSimulation -> {
                    event.setY(event.getY() + 0.1);
                    simulateJump = true;
                }
            }
            falling = false;
        }

        if (mode.is(Mode.NCP) && mc.player.fallDistance > fallDistance.get()) {
            event.setOnGround(true);
        }
    }

    @Listen
    private void onKeyboardInput(KeyboardInputEvent event) {
        if (simulateJump) {
            event.setJump(true);
            simulateJump = false;
        }
    }

    private void resetState() {
        falling = false;
        simulateJump = false;
    }
}
