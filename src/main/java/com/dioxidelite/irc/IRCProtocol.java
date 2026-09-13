package com.dioxidelite.irc;

import java.util.Set;

/**
 * Wire-level control messages shared with the OpticsValley IRC server.
 * The CRASH trigger constant is intentionally not handled by the client
 * (see {@link IRCClient}): a remote server must never be able to kill the
 * local game process, so incoming CRASH frames are dropped as inert text.
 */
public final class IRCProtocol {

    public static final String CRASH_CONTROL_MESSAGE = "&c[CRASH_TRIGGER]&4";
    public static final String RESTART_CONTROL_MESSAGE =
            "&c[OpticsValleyIRC] 服务器正在重启，所有连接将被断开";
    public static final String SHUTDOWN_CONTROL_MESSAGE =
            "&c[OpticsValleyIRC] 服务器正在关闭，所有连接将被断开";
    public static final String DISCONNECT_CONTROL_MESSAGE =
            "&c[OpticsValleyIRC] 与服务器的连接已断开";

    public static final String JOIN_PREFIX = "[OpticsValleyIRC] 用户 ";
    public static final String JOIN_SUFFIX = " 已加入IRC";
    public static final String LEAVE_PREFIX = "[OpticsValleyIRC] 用户 ";
    public static final String LEAVE_SUFFIX = " 已离开IRC";

    private static final Set<String> CONTROL_MESSAGES = Set.of(
            CRASH_CONTROL_MESSAGE,
            RESTART_CONTROL_MESSAGE,
            SHUTDOWN_CONTROL_MESSAGE,
            DISCONNECT_CONTROL_MESSAGE
    );

    private IRCProtocol() {
    }

    public static boolean isControlMessage(String message) {
        return CONTROL_MESSAGES.contains(message);
    }
}
