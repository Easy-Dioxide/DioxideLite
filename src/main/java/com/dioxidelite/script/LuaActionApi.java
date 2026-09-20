package com.dioxidelite.script;

// ---------------------------------------------------------------------------
// 移植来源：SetsunaClient（上游开源版）com/setsuna/script/LuaActionApi.java
// 变更：包名/导入 com.setsuna.* -> com.dioxidelite.*，mixin 方法前缀
//       setsuna$ -> dioxidelite$，字符串中的 setsuna -> dioxidelite。
//       逻辑逐行保留，未做功能改动。
// ---------------------------------------------------------------------------


import com.dioxidelite.DioxideLite;
import com.dioxidelite.manager.RotationManager;
import com.dioxidelite.util.rotation.Priority;
import com.dioxidelite.util.rotation.Rot2f;
import com.dioxidelite.util.rotation.RotationUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/** Rate-limited player and network actions available to functional scripts. */
final class LuaActionApi {

    private static final Set<Object> CURSOR_RELEASE_OWNERS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private LuaActionApi() {
    }

    static LuaTable create(LuaScript script) {
        LuaTable action = new LuaTable();
        action.set("jump", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            consume(script, LuaScript.ActionKind.GENERIC);
            DioxideLite.mc().player.jumpFromGround();
            return LuaValue.TRUE;
        }));
        action.set("rotate", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            Rot2f rotation = rotation(args.arg(first), args.arg(first + 1));
            consume(script, LuaScript.ActionKind.GENERIC);
            RotationManager.INSTANCE.releaseSilentRotation(LuaExecutionGuard.requireOwner());
            applyClientRotation(rotation);
            return LuaValue.TRUE;
        }));
        action.set("rotate_silent", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            Rot2f rotation = rotation(args.arg(first), args.arg(first + 1));
            Priority priority = priority(args.arg(first + 2));
            consume(script, LuaScript.ActionKind.GENERIC);
            return LuaValue.valueOf(RotationManager.INSTANCE.claimSilentRotation(
                    LuaExecutionGuard.requireOwner(), rotation, priority));
        }));
        action.set("release_rotation", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            consume(script, LuaScript.ActionKind.GENERIC);
            RotationManager.INSTANCE.releaseSilentRotation(LuaExecutionGuard.requireOwner());
            return LuaValue.TRUE;
        }));
        action.set("send_rotation", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable() || DioxideLite.mc().getConnection() == null) return unavailable();
            Rot2f rotation = rotation(args.arg(first), args.arg(first + 1));
            boolean onGround = args.arg(first + 2).optboolean(DioxideLite.mc().player.onGround());
            boolean horizontalCollision = args.arg(first + 3)
                    .optboolean(DioxideLite.mc().player.horizontalCollision);
            consume(script, LuaScript.ActionKind.GENERIC);
            DioxideLite.mc().getConnection().send(new ServerboundMovePlayerPacket.Rot(
                    rotation.getYaw(), rotation.getPitch(), onGround, horizontalCollision));
            return LuaValue.TRUE;
        }));
        action.set("look_at", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            double x = worldCoordinate(args.arg(first), "x");
            double y = worldCoordinate(args.arg(first + 1), "y");
            double z = worldCoordinate(args.arg(first + 2), "z");
            String mode = args.arg(first + 3).optjstring("client")
                    .trim().toLowerCase(Locale.ROOT);
            Rot2f rotation = RotationUtils.calculate(new Vec3(x, y, z));
            consume(script, LuaScript.ActionKind.GENERIC);
            return switch (mode) {
                case "client", "visible" -> {
                    RotationManager.INSTANCE.releaseSilentRotation(
                            LuaExecutionGuard.requireOwner());
                    applyClientRotation(rotation);
                    yield LuaValue.TRUE;
                }
                case "silent", "server" -> LuaValue.valueOf(
                        RotationManager.INSTANCE.claimSilentRotation(
                                LuaExecutionGuard.requireOwner(), rotation,
                                priority(args.arg(first + 4))));
                case "packet", "send" -> {
                    if (DioxideLite.mc().getConnection() == null) yield LuaValue.FALSE;
                    DioxideLite.mc().getConnection().send(new ServerboundMovePlayerPacket.Rot(
                            rotation.getYaw(), rotation.getPitch(),
                            DioxideLite.mc().player.onGround(),
                            DioxideLite.mc().player.horizontalCollision));
                    yield LuaValue.TRUE;
                }
                default -> throw new LuaError("look_at mode must be client, silent, or packet");
            };
        }));
        action.set("cursor_grab", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            boolean grabbed = args.arg(first).optboolean(true);
            Object owner = LuaExecutionGuard.requireOwner();
            consume(script, LuaScript.ActionKind.GENERIC);
            setCursorGrabbed(owner, grabbed);
            return LuaValue.valueOf(DioxideLite.mc().mouseHandler.isMouseGrabbed());
        }));
        action.set("set_velocity", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            double x = LuaApiSupport.boundedDouble(args.arg(first), "x", -10.0, 10.0);
            double y = LuaApiSupport.boundedDouble(args.arg(first + 1), "y", -10.0, 10.0);
            double z = LuaApiSupport.boundedDouble(args.arg(first + 2), "z", -10.0, 10.0);
            consume(script, LuaScript.ActionKind.GENERIC);
            DioxideLite.mc().player.setDeltaMovement(new Vec3(x, y, z));
            return LuaValue.TRUE;
        }));
        action.set("sprint", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            boolean sprinting = args.arg(first).optboolean(true);
            consume(script, LuaScript.ActionKind.GENERIC);
            DioxideLite.mc().player.setSprinting(sprinting);
            return LuaValue.TRUE;
        }));
        action.set("swing", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            InteractionHand hand = hand(args.arg(first));
            consume(script, LuaScript.ActionKind.GENERIC);
            DioxideLite.mc().player.swing(hand);
            return LuaValue.TRUE;
        }));
        action.set("use", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable() || DioxideLite.mc().gameMode == null) return unavailable();
            InteractionHand hand = hand(args.arg(first));
            consume(script, LuaScript.ActionKind.GENERIC);
            InteractionResult result = DioxideLite.mc().gameMode.useItem(DioxideLite.mc().player, hand);
            if (!(result instanceof InteractionResult.Success success)) return LuaValue.FALSE;
            if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                DioxideLite.mc().player.swing(hand);
            }
            DioxideLite.mc().gameRenderer.itemInHandRenderer.itemUsed(hand);
            return LuaValue.TRUE;
        }));
        action.set("attack", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            if (!playerAvailable() || DioxideLite.mc().gameMode == null) return LuaValue.FALSE;
            Entity target = LuaEntityApi.find(args.arg(first).checkint());
            if (target == null || target == DioxideLite.mc().player || !target.isAlive()) return LuaValue.FALSE;
            consume(script, LuaScript.ActionKind.ATTACK);
            DioxideLite.mc().gameMode.attack(DioxideLite.mc().player, target);
            DioxideLite.mc().player.swing(InteractionHand.MAIN_HAND);
            return LuaValue.TRUE;
        }));
        action.set("select_slot", LuaApiSupport.method(action, (args, first) -> {
            if (!playerAvailable()) return unavailable();
            int slot = args.arg(first).checkint();
            if (slot < 1 || slot > 9) throw new LuaError("slot must be in the Lua range 1..9");
            consume(script, LuaScript.ActionKind.GENERIC);
            DioxideLite.mc().player.getInventory().setSelectedSlot(slot - 1);
            return LuaValue.TRUE;
        }));
        action.set("send_chat", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            if (DioxideLite.mc().getConnection() == null) return LuaValue.FALSE;
            String message = message(args.arg(first), "chat message");
            consume(script, LuaScript.ActionKind.MESSAGE);
            DioxideLite.mc().getConnection().sendChat(message);
            return LuaValue.TRUE;
        }));
        action.set("send_command", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            if (DioxideLite.mc().getConnection() == null) return LuaValue.FALSE;
            String command = message(args.arg(first), "command");
            while (command.startsWith("/")) command = command.substring(1);
            if (command.isBlank()) throw new LuaError("command must not be empty");
            consume(script, LuaScript.ActionKind.MESSAGE);
            DioxideLite.mc().getConnection().sendCommand(command);
            return LuaValue.TRUE;
        }));
        action.set("module_toggle", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            String moduleId = LuaApiSupport.boundedString(
                    args.arg(first), "module id", 128).trim();
            consume(script, LuaScript.ActionKind.GENERIC);
            return LuaValue.valueOf(LuaClientModuleApi.toggleModule(moduleId));
        }));
        action.set("module_set", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            String moduleId = LuaApiSupport.boundedString(
                    args.arg(first), "module id", 128).trim();
            boolean enabled = args.arg(first + 1).checkboolean();
            consume(script, LuaScript.ActionKind.GENERIC);
            return LuaValue.valueOf(LuaClientModuleApi.setModuleEnabled(moduleId, enabled));
        }));
        action.set("setting_set", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            String moduleId = LuaApiSupport.boundedString(
                    args.arg(first), "module id", 128).trim();
            String settingId = LuaApiSupport.boundedString(
                    args.arg(first + 1), "setting id", 128).trim();
            LuaValue value = args.arg(first + 2);
            consume(script, LuaScript.ActionKind.GENERIC);
            return LuaValue.valueOf(LuaClientModuleApi.setSetting(moduleId, settingId, value));
        }));
        action.set("setting_cycle", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            String moduleId = LuaApiSupport.boundedString(
                    args.arg(first), "module id", 128).trim();
            String settingId = LuaApiSupport.boundedString(
                    args.arg(first + 1), "setting id", 128).trim();
            int direction = args.arg(first + 2).optint(1);
            consume(script, LuaScript.ActionKind.GENERIC);
            return LuaValue.valueOf(
                    LuaClientModuleApi.cycleSetting(moduleId, settingId, direction));
        }));
        action.set("setting_press", LuaApiSupport.method(action, (args, first) -> {
            LuaExecutionGuard.requireRuntimeCallback();
            String moduleId = LuaApiSupport.boundedString(
                    args.arg(first), "module id", 128).trim();
            String settingId = LuaApiSupport.boundedString(
                    args.arg(first + 1), "setting id", 128).trim();
            consume(script, LuaScript.ActionKind.GENERIC);
            return LuaValue.valueOf(LuaClientModuleApi.pressSetting(moduleId, settingId));
        }));
        return action;
    }

    static void releaseOwnedState(Object owner) {
        RotationManager.INSTANCE.releaseSilentRotation(owner);
        synchronized (CURSOR_RELEASE_OWNERS) {
            if (CURSOR_RELEASE_OWNERS.remove(owner)) restoreCursorIfUnowned();
        }
    }

    private static void consume(LuaScript script, LuaScript.ActionKind kind) {
        LuaExecutionGuard.consumeAction();
        script.consumeAction(kind);
    }

    private static Rot2f rotation(LuaValue yawValue, LuaValue pitchValue) {
        float yaw = Mth.wrapDegrees(LuaApiSupport.finiteFloat(yawValue, "yaw"));
        float pitch = LuaApiSupport.boundedFloat(pitchValue, "pitch", -90.0F, 90.0F);
        return new Rot2f(yaw, pitch);
    }

    private static void applyClientRotation(Rot2f rotation) {
        DioxideLite.mc().player.setYRot(rotation.getYaw());
        DioxideLite.mc().player.setXRot(rotation.getPitch());
        DioxideLite.mc().player.yHeadRot = rotation.getYaw();
        DioxideLite.mc().player.yBodyRot = rotation.getYaw();
    }

    private static Priority priority(LuaValue value) {
        if (value.isnil()) return Priority.Medium;
        if (value.isnumber()) {
            int requested = Math.max(0, Math.min(Priority.values().length - 1, value.checkint()));
            return Priority.values()[requested];
        }
        String name = value.checkjstring().trim();
        for (Priority priority : Priority.values()) {
            if (priority.name().equalsIgnoreCase(name)) return priority;
        }
        throw new LuaError("priority must be 0..4 or lowest/low/medium/high/highest");
    }

    private static double worldCoordinate(LuaValue value, String name) {
        double result = LuaApiSupport.finiteDouble(value, name);
        if (Math.abs(result) > 30_000_000.0) {
            throw new LuaError(name + " is outside the supported world range");
        }
        return result;
    }

    private static void setCursorGrabbed(Object owner, boolean grabbed) {
        synchronized (CURSOR_RELEASE_OWNERS) {
            if (grabbed) {
                CURSOR_RELEASE_OWNERS.remove(owner);
                restoreCursorIfUnowned();
            } else {
                CURSOR_RELEASE_OWNERS.add(owner);
                DioxideLite.mc().mouseHandler.releaseMouse();
            }
        }
    }

    private static void restoreCursorIfUnowned() {
        if (CURSOR_RELEASE_OWNERS.isEmpty()
                && DioxideLite.mc().screen == null
                && !DioxideLite.mc().mouseHandler.isMouseGrabbed()) {
            DioxideLite.mc().mouseHandler.grabMouse();
        }
    }

    private static LuaValue unavailable() {
        LuaExecutionGuard.requireRuntimeCallback();
        return LuaValue.FALSE;
    }

    private static boolean playerAvailable() {
        LuaExecutionGuard.requireRuntimeCallback();
        return DioxideLite.mc().player != null && DioxideLite.mc().level != null;
    }

    private static InteractionHand hand(LuaValue value) {
        String name = value.optjstring("main").trim().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "main", "main_hand", "mainhand" -> InteractionHand.MAIN_HAND;
            case "off", "off_hand", "offhand" -> InteractionHand.OFF_HAND;
            default -> throw new LuaError("hand must be main or off");
        };
    }

    private static String message(LuaValue value, String name) {
        String result = LuaApiSupport.boundedString(value, name, 256).trim();
        if (result.isEmpty() || result.indexOf('\n') >= 0 || result.indexOf('\r') >= 0) {
            throw new LuaError(name + " must be one non-empty line");
        }
        return result;
    }
}
