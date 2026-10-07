package com.dioxidelite.module.modules.movement;

// ---------------------------------------------------------------------------
// 移植来源：DioxideLite（来源开源版）com/dioxidelite/module/modules/movement/InvMove.java
// 变更：包名/导入 com.dioxidelite.* -> com.dioxidelite.*，mixin 方法前缀
//       dioxidelite$ -> dioxidelite$，字符串中的 dioxidelite -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.mojang.blaze3d.platform.InputConstants;
import com.dioxidelite.event.Listen;
import com.dioxidelite.event.Priority;
import com.dioxidelite.event.events.KeyInputEvent;
import com.dioxidelite.event.events.PacketEvent;
import com.dioxidelite.event.events.PlayerTickEvent;
import com.dioxidelite.i18n.TranslationKey;
import com.dioxidelite.mixin.KeyMappingAccessor;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.module.modules.movement.invmove.InvMoveContext;
import com.dioxidelite.module.modules.movement.invmove.InvMoveEngine;
import com.dioxidelite.module.modules.movement.invmove.InvMoveKey;
import com.dioxidelite.module.modules.movement.invmove.InvMovePacketAction;
import com.dioxidelite.module.modules.movement.invmove.InvMoveScreen;
import com.dioxidelite.setting.settings.BooleanSetting;
import com.dioxidelite.util.network.PacketUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.lwjgl.glfw.GLFW;

/**
 * 背包移动（InvMove）—— 打开界面时依然可以正常移动。
 *
 * <p><b>为什么需要它：</b>原版在打开任何界面时会调用 {@code KeyMapping.releaseAll()}，
 * 把所有按键状态清成"没按"，于是玩家在背包里想边走边翻东西只能原地站桩。
 * 本模块在每个玩家 tick 的开头（{@code PlayerTickEvent.Pre}，早于 {@code aiStep} 读输入）
 * 把物理上真的按着的移动键重新写回 {@code KeyMapping}，原版输入系统就会照常驱动移动。
 *
 * <p>涉及的键：前 / 左 / 后 / 右、跳跃、潜行、冲刺。
 *
 * <p>设置项：
 * <ul>
 *   <li>{@code Allow Containers} — 箱子 / 工作台等容器界面内是否也允许移动</li>
 *   <li>{@code Allow Sneak} — 背包界面内是否允许潜行（默认禁止，避免翻背包时误触）</li>
 *   <li>{@code Auto Sprint} — 背包界面内移动时自动冲刺</li>
 * </ul>
 */
public final class InvMove extends Module {
    public static final InvMove INSTANCE = new InvMove();

    /** HUD 后缀：正在背包界面里移动。 */
    private static final TranslationKey STATE_INVENTORY =
            TranslationKey.of("DioxideLite.module.invmove.state.inventory", "Inventory");

    /** HUD 后缀：正在容器界面里移动。 */
    private static final TranslationKey STATE_CONTAINER =
            TranslationKey.of("DioxideLite.module.invmove.state.container", "Container");

    /** 容器界面（箱子 / 工作台 / 熔炉等）内是否允许移动。 */
    private final BooleanSetting containers = add(new BooleanSetting("Allow Containers", false));

    /** 背包界面内是否允许潜行。 */
    private final BooleanSetting sneak = add(new BooleanSetting("Allow Sneak", false));

    /** 背包界面内移动时自动冲刺。 */
    private final BooleanSetting autoSprint = add(new BooleanSetting("Auto Sprint", true));

    private final MinecraftContext context = new MinecraftContext();
    private final InvMoveEngine engine = new InvMoveEngine(context);

    private InvMove() {
        super("InvMove", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        engine.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        engine.setEnabled(false);
    }

    /** HUD 上显示当前是否生效（背包 / 容器），方便一眼确认模块状态。 */
    @Override
    public String getInfo() {
        return switch (context.screen()) {
            case INVENTORY -> STATE_INVENTORY.get();
            case HANDLED_OTHER -> containers.get() ? STATE_CONTAINER.get() : null;
            default -> null;
        };
    }

    @Listen
    private void onKey(KeyInputEvent event) {
        InvMoveKey key = context.keyForCode(event.key());
        if (key != null) {
            engine.onKeyEvent(key, event.action() != GLFW.GLFW_RELEASE);
        }
    }

    @Listen
    private void onTick(PlayerTickEvent.Pre event) {
        engine.tick();
    }

    @Listen(priority = Priority.HIGHEST)
    private void onPacketSend(PacketEvent.Send event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundContainerClickPacket click) {
            engine.onClickSlotPacket(click.containerId());
        } else if (packet instanceof ServerboundContainerClosePacket close
                && engine.onCloseInventoryPacket(close.getContainerId()) == InvMovePacketAction.CANCEL) {
            event.setCancelled(true);
        }
    }

    @Listen
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundOpenScreenPacket
                || event.getPacket() instanceof ClientboundContainerClosePacket) {
            engine.onServerScreenChange();
        }
    }

    private final class MinecraftContext implements InvMoveContext {
        @Override
        public boolean isAvailable() {
            return mc.player != null && mc.level != null;
        }

        @Override
        public InvMoveScreen screen() {
            Screen screen = mc.screen;
            if (screen == null) return InvMoveScreen.NONE;
            // 生存背包 InventoryScreen 与创造背包 CreativeModeInventoryScreen
            // 都算"背包"——后者是 AbstractContainerScreen 的子类，必须单独判，
            // 否则创造模式下翻背包时移动会完全失效。
            if (screen instanceof InventoryScreen) return InvMoveScreen.INVENTORY;
            if (screen instanceof CreativeModeInventoryScreen) return InvMoveScreen.INVENTORY;
            if (screen instanceof ChatScreen) return InvMoveScreen.CHAT;
            if (screen instanceof AbstractContainerScreen<?>) return InvMoveScreen.HANDLED_OTHER;
            return InvMoveScreen.OTHER;
        }

        @Override
        public boolean allowContainers() {
            return containers.get();
        }

        @Override
        public boolean allowSneak() {
            return sneak.get();
        }

        @Override
        public boolean autoSprint() {
            return autoSprint.get();
        }

        @Override
        public boolean isPhysicalKeyPressed(InvMoveKey key) {
            KeyMapping mapping = mapping(key);
            InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping).dioxidelite$getKey();
            return bound.getType() == InputConstants.Type.KEYSYM
                    && InputConstants.isKeyDown(mc.getWindow(), bound.getValue());
        }

        @Override
        public void setLogicalKeyPressed(InvMoveKey key, boolean pressed) {
            mapping(key).setDown(pressed);
        }

        @Override
        public void sendCloseInventoryPacket() {
            PacketUtils.sendSilently(new ServerboundContainerClosePacket(0));
        }

        private InvMoveKey keyForCode(int keyCode) {
            for (InvMoveKey key : new InvMoveKey[]{
                    InvMoveKey.FORWARD, InvMoveKey.LEFT, InvMoveKey.BACK,
                    InvMoveKey.RIGHT, InvMoveKey.JUMP, InvMoveKey.SNEAK}) {
                InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping(key)).dioxidelite$getKey();
                if (bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() == keyCode) {
                    return key;
                }
            }
            return null;
        }

        private KeyMapping mapping(InvMoveKey key) {
            return switch (key) {
                case FORWARD -> mc.options.keyUp;
                case LEFT -> mc.options.keyLeft;
                case BACK -> mc.options.keyDown;
                case RIGHT -> mc.options.keyRight;
                case JUMP -> mc.options.keyJump;
                case SNEAK -> mc.options.keyShift;
                case SPRINT -> mc.options.keySprint;
            };
        }
    }
}
