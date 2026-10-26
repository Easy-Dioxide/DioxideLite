package com.dioxideliteng.krs.command;

import com.dioxideliteng.krs.KrsClient;
import com.dioxideliteng.krs.hacks.Module;
import com.dioxideliteng.krs.hacks.ModuleManager;
import com.dioxideliteng.krs.hacks.features.exploit.ServerCrasher;
import com.dioxideliteng.krs.hacks.features.nulling.PluginsDetector;
import com.dioxideliteng.krs.hacks.features.player.ChatCommands;
import com.dioxideliteng.krs.utils.ChatUtil;
import com.dioxideliteng.krs.utils.IMinecraft;
import com.dioxideliteng.krs.utils.input.Keys;
import com.dioxideliteng.krs.utils.network.ServerLookup;
import com.dioxideliteng.krs.utils.packet.PacketUtil;
import com.dioxideliteng.krs.utils.pathfinder.MainPathFinder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * KRS 的聊天命令系统。
 *
 * <p>从 KRS 的 ImGui 面板（{@code ModuleRenderable}）中拆出：原版把命令解析与 ImGui 绘制写在
 * 同一个类里，移植时只保留命令逻辑，界面输出改走 {@link #commandLogs} 与聊天栏。
 * 命令语义与原版逐条保持一致。</p>
 */
public final class KrsCommands implements IMinecraft {

    private static final int MAX_COMMAND_LOGS = 50;
    private static final int MAX_COMMAND_LOG_LENGTH = 4_096;

    /** 命令输出缓冲，供 NG 的界面或调试面板读取。 */
    public static final List<String> commandLogs = new CopyOnWriteArrayList<>();

    private KrsCommands() {
    }

    private static void showLog(Boolean chat, String message) {
        if (chat) {
            ChatUtil.printChat(message);
        }
        addCommandLog(message);
    }

    private static void log(String log) {
        addCommandLog(log);
    }

    public static void addCommandLog(String log) {
        String safeLog = log == null ? "" : log;
        if (safeLog.length() > MAX_COMMAND_LOG_LENGTH) {
            safeLog = safeLog.substring(0, MAX_COMMAND_LOG_LENGTH);
        }

        synchronized (commandLogs) {
            while (commandLogs.size() >= MAX_COMMAND_LOGS) {
                commandLogs.remove(0);
            }
            commandLogs.add(safeLog);
        }
    }

    public static void executeCommand(String command, Boolean chatMode) {
        if (command.isBlank()) {
            if (chatMode)
                showLog(true, ChatCommands.prefix.get() + "help to show every commands");
            return;
        }

        if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("commands")) {
            showLog(chatMode, "help/commands -> show every commands");
            showLog(chatMode, "say <message> -> force send messages");
            showLog(chatMode, "t/toggle <module> -> toggle module");
            showLog(chatMode, "ign/name -> copy your current username");
            showLog(chatMode, "bind <module> <key> -> bind module");
            showLog(chatMode, "crash -> crash the server");
            showLog(chatMode, "copynbt -> copy your holding item nbt");
            showLog(chatMode, "pl/plugins -> detect server plugins");
            showLog(chatMode, "transaction -> debug transaction packets (x10)");
            showLog(chatMode, "session -> show your current session id");
            showLog(chatMode, "vclip <height> -> teleport up from current position");
            showLog(chatMode, "tp <x> <y> <z> or <player> -> teleport to everywhere");
            showLog(chatMode, "notify <title> <message> -> show custom notification");
            showLog(chatMode, "server <address> -> dump raw IP, vulns, linked servers, and all lookup data");
        } else if (command.equalsIgnoreCase("server") || command.toLowerCase(Locale.ROOT).startsWith("server ")) {
            String target = command.length() > 7 ? command.substring(7).trim() : "";
            String error = ServerLookup.start(target, lines -> mc.execute(() -> {
                for (String line : lines)
                    showLog(chatMode, line);
            }));
            showLog(chatMode, Objects.requireNonNullElse(error, "Looking up server..."));
        } else if (command.toLowerCase().startsWith("t " ) || command.toLowerCase().startsWith("toggle " )) {
            String moduleName = "";

            if (command.toLowerCase().startsWith("t " ))
                moduleName = command.toLowerCase().replace("t ", "").replace(" ", "");
            if (command.toLowerCase().startsWith("toggle " ))
                moduleName = command.toLowerCase().replace("toggle ", "").replace(" ", "");

            for (Module module : ModuleManager.modules) {
                if (moduleName.equals(module.moduleName.toLowerCase().replace(" ", ""))) {
                    module.toggle();
                    KrsClient.notificationManager.addNotification("Toggle", module.tempEnabled ? "Enabled " + module.moduleName : "Disabled " + module.moduleName);
                    return;
                }
            }

            showLog(chatMode, "Module " + moduleName + " was not found");
        } else if (command.equalsIgnoreCase("crash")) {
            showLog(chatMode, "Crashing server...");
            for (Module module : ModuleManager.modules) {
                if (module instanceof ServerCrasher && !module.tempEnabled)
                    module.toggle();
            }
        } else if (command.equalsIgnoreCase("transaction")) {
            showLog(chatMode, "Logging transactions...");
            ModuleManager.gettingTransactions = true;
        } else if (command.equalsIgnoreCase("ign") || command.equalsIgnoreCase("name")) {
            showLog(chatMode, "Copied username to your clipboard!");
            KrsClient.copyToClipboard(mc.getGameProfile().name());
        } else if (command.equalsIgnoreCase("copynbt")) {
            if (mc.player != null) {
                MutableComponent text = Component.empty();

                try {
                    DataAccessor dataCommandObject = new EntityDataAccessor(mc.player);
                    NbtPathArgument.NbtPath handPath = NbtPathArgument.NbtPath.of("SelectedItem");
                    List<Tag> nbtElement = handPath.get(dataCommandObject.getData());
                    if (!nbtElement.isEmpty()) {
                        text.append(" ").append(NbtUtils.toPrettyComponent(nbtElement.getFirst()));
                    }
                    showLog(chatMode, "Copied nbt to your clipboard!");
                } catch (CommandSyntaxException ignored) {
                    text.append(" ").append("{}");
                    showLog(chatMode, "No NBTs found");
                }

                ChatUtil.printModifiedChat(text);
                KrsClient.copyToClipboard(text.getString().substring(1));
            } else showLog(chatMode, "player is null");
        } else if (command.equalsIgnoreCase("pl") || command.equalsIgnoreCase("plugins")) {
            showLog(chatMode, "Detecting plugins...");
            for (Module module : ModuleManager.modules) {
                if (module instanceof PluginsDetector && !module.tempEnabled)
                    module.toggle();
            }
        } else if (command.startsWith("say ")) {
            String message = command.toLowerCase().replace("say ", "");
            if (mc.player != null) {
                if (message.startsWith("/"))
                    mc.player.connection.sendCommand(message.substring(1));
                else mc.player.connection.sendChat(message);
            }
        } else if (command.startsWith("bind ")) {
            String moduleName;
            moduleName = command.toLowerCase().replace("bind ", "");

            String[] parts = moduleName.split(" ");

            if (parts.length == 2) {
                for (Module module : ModuleManager.allModules) {
                    if (Objects.equals(module.moduleName.toLowerCase().replace(" ", ""), parts[0])) {
                        try {
                            int newKey = InputConstants.getKey("key.keyboard." + parts[1].toLowerCase()).getValue();
                            module.key = newKey;
                            showLog(chatMode, "Bound " + keyName(newKey) + " key to " + module.moduleName + " module");
                        } catch (NumberFormatException ignored) {
                            module.key = Keys.KEY_UNKNOWN;
                            showLog(chatMode, "Unbound " + module.moduleName + " module");
                        }
                        return;
                    }
                }
            }

            showLog(chatMode, "Module " + moduleName + " was not found");
        } else if (command.startsWith("tp ")) {
            String tpStr;
            tpStr = command.toLowerCase().replace("tp ", "");

            String[] parts = tpStr.split(" ");

            if (mc.player != null && mc.level != null) {
                if (parts.length == 1) {
                    boolean found = false;

                    for (Player playerEntity : mc.level.players()) {
                        if (playerEntity.getName().getString().equalsIgnoreCase(parts[0])) {
                            found = true;

                            ArrayList<Vec3> paths;
                            paths = MainPathFinder.computePath(mc.player.position(), playerEntity.position());

                            if (paths == null || paths.isEmpty()) return;

                            for (Vec3 path : paths) {
                                PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(path.x, path.y, path.z, true, mc.player.horizontalCollision));
                            }

                            mc.player.setPos(playerEntity.position());

                            showLog(chatMode, "teleported");
                            KrsClient.notificationManager.addNotification("Success", "Teleported!");

                            break;
                        }
                    }

                    if (found) return;
                } else if (parts.length == 3) {
                    try {
                        String realX = parts[0];
                        boolean addXFix = false;
                        if (realX.contains("~")) {
                            realX = realX.replace("~", "");
                            if (realX.isBlank())
                                realX = "0";
                            addXFix = true;
                        }
                        double xPos = Double.parseDouble(realX);
                        if (addXFix) xPos += mc.player.position().x;

                        String realY = parts[1];
                        boolean addYFix = false;
                        if (realY.contains("~")) {
                            realY = realY.replace("~", "");
                            if (realY.isBlank())
                                realY = "0";
                            addYFix = true;
                        }
                        double yPos = Double.parseDouble(realY);
                        if (addYFix) yPos += mc.player.position().y;

                        String realZ = parts[2];
                        boolean addZFix = false;
                        if (realZ.contains("~")) {
                            realZ = realZ.replace("~", "");
                            if (realZ.isBlank())
                                realZ = "0";
                            addZFix = true;
                        }
                        double zPos = Double.parseDouble(realZ);
                        if (addZFix) zPos += mc.player.position().z;

                        ArrayList<Vec3> paths;
                        paths = MainPathFinder.computePath(mc.player.position(), new Vec3(xPos, yPos, zPos));

                        if (paths == null || paths.isEmpty()) return;

                        for (Vec3 path : paths) {
                            PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(path.x, path.y, path.z, true, mc.player.horizontalCollision));
                        }

                        mc.player.setPos(paths.getLast());

                        showLog(chatMode, "teleported");
                        KrsClient.notificationManager.addNotification("Success", "Teleported!");
                        return;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            showLog(chatMode, "Failed to teleport");
            KrsClient.notificationManager.addNotification("Failed", "Failed to teleport");
        } else if (command.startsWith("vclip ")) {
            String distance;
            distance = command.toLowerCase().replace("vclip ", "");

            try {
                double dist = Double.parseDouble(distance);
                if (mc.player != null) {
                    if (dist > 10 || dist < -10) {
                        for (int i = 0; i <= 9; i++) {
                            PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.position().x, mc.player.position().y, mc.player.position().z, true, mc.player.horizontalCollision));
                        }
                    }
                    mc.player.setPos(mc.player.position().x, mc.player.position().y + dist, mc.player.position().z);
                    showLog(chatMode, "Vertical clipped");
                    KrsClient.notificationManager.addNotification("Success", "Vertical clipped");
                }
            } catch (NumberFormatException ignored) {
                showLog(chatMode, "Failed to vclip");
                KrsClient.notificationManager.addNotification("Failed", "Failed to vclip");
            }
        } else if (command.equalsIgnoreCase("session")) {
            if (mc.getConnection() != null)
                showLog(chatMode, String.valueOf(mc.getConnection().getLocalGameProfile().id()));
            else showLog(chatMode, "User is null");
        } else if (command.startsWith("notify ")) {
            int index = command.indexOf(" ");
            String notifies = (index != -1) ? command.substring(index + 1) : "";
            String[] parts = notifies.split(" ");

            if (parts.length == 2) {
                showLog(chatMode, "Notified");
                KrsClient.notificationManager.addNotification(parts[0].replace("|", " "), parts[1].replace("|", " "));
                return;
            }

            showLog(chatMode, "Failed to notify");
        } else {
            showLog(chatMode, "Unknown command: " + command);
        }
    }

    public static String keyName(int key) {
        if (key == Keys.KEY_UNKNOWN)
            return "NONE";

        String name = Keys.printableKeyName(key);
        if (name != null)
            return name.toUpperCase();

        return fallbackKeyName(key).toUpperCase();
    }

    private static String fallbackKeyName(int key) {
        return switch (key) {
            case Keys.KEY_UNKNOWN, Keys.KEY_SLASH, Keys.KEY_DELETE -> "NONE";
            case Keys.KEY_SPACE -> "Space";
            case Keys.KEY_ESCAPE -> "Escape";
            case Keys.KEY_ENTER -> "Enter";
            case Keys.KEY_TAB -> "Tab";
            case Keys.KEY_BACKSPACE -> "Backspace";
            case Keys.KEY_INSERT -> "Insert";
            case Keys.KEY_RIGHT -> "Right";
            case Keys.KEY_LEFT -> "Left";
            case Keys.KEY_DOWN -> "Down";
            case Keys.KEY_UP -> "Up";
            case Keys.KEY_PAGE_UP -> "Page Up";
            case Keys.KEY_PAGE_DOWN -> "Page Down";
            case Keys.KEY_HOME -> "Home";
            case Keys.KEY_END -> "End";
            case Keys.KEY_CAPS_LOCK -> "Caps Lock";
            case Keys.KEY_SCROLL_LOCK -> "Scroll Lock";
            case Keys.KEY_NUM_LOCK -> "Num Lock";
            case Keys.KEY_PRINT_SCREEN -> "Print Screen";
            case Keys.KEY_PAUSE -> "Pause";
            case Keys.KEY_F1 -> "F1";
            case Keys.KEY_F2 -> "F2";
            case Keys.KEY_F3 -> "F3";
            case Keys.KEY_F4 -> "F4";
            case Keys.KEY_F5 -> "F5";
            case Keys.KEY_F6 -> "F6";
            case Keys.KEY_F7 -> "F7";
            case Keys.KEY_F8 -> "F8";
            case Keys.KEY_F9 -> "F9";
            case Keys.KEY_F10 -> "F10";
            case Keys.KEY_F11 -> "F11";
            case Keys.KEY_F12 -> "F12";
            case Keys.KEY_F13 -> "F13";
            case Keys.KEY_F14 -> "F14";
            case Keys.KEY_F15 -> "F15";
            case Keys.KEY_F16 -> "F16";
            case Keys.KEY_F17 -> "F17";
            case Keys.KEY_F18 -> "F18";
            case Keys.KEY_F19 -> "F19";
            case Keys.KEY_F20 -> "F20";
            case Keys.KEY_F21 -> "F21";
            case Keys.KEY_F22 -> "F22";
            case Keys.KEY_F23 -> "F23";
            case Keys.KEY_F24 -> "F24";
            case Keys.KEY_KP_0 -> "Keypad 0";
            case Keys.KEY_KP_1 -> "Keypad 1";
            case Keys.KEY_KP_2 -> "Keypad 2";
            case Keys.KEY_KP_3 -> "Keypad 3";
            case Keys.KEY_KP_4 -> "Keypad 4";
            case Keys.KEY_KP_5 -> "Keypad 5";
            case Keys.KEY_KP_6 -> "Keypad 6";
            case Keys.KEY_KP_7 -> "Keypad 7";
            case Keys.KEY_KP_8 -> "Keypad 8";
            case Keys.KEY_KP_9 -> "Keypad 9";
            case Keys.KEY_KP_DECIMAL -> "Keypad .";
            case Keys.KEY_KP_DIVIDE -> "Keypad /";
            case Keys.KEY_KP_MULTIPLY -> "Keypad *";
            case Keys.KEY_KP_SUBTRACT -> "Keypad -";
            case Keys.KEY_KP_ADD -> "Keypad +";
            case Keys.KEY_KP_ENTER -> "Keypad Enter";
            case Keys.KEY_KP_EQUAL -> "Keypad =";
            case Keys.KEY_LEFT_SHIFT -> "Left Shift";
            case Keys.KEY_LEFT_CONTROL -> "Left Control";
            case Keys.KEY_LEFT_ALT -> "Left Alt";
            case Keys.KEY_LEFT_SUPER -> "Left Super";
            case Keys.KEY_RIGHT_SHIFT -> "Right Shift";
            case Keys.KEY_RIGHT_CONTROL -> "Right Control";
            case Keys.KEY_RIGHT_ALT -> "Right Alt";
            case Keys.KEY_RIGHT_SUPER -> "Right Super";
            case Keys.KEY_MENU -> "Menu";
            default -> String.valueOf(key);
        };
    }
}
