package com.opticsvalley.protocol;

import java.util.Set;

public final class IRCProtocol {
	public static final String CRASH_CONTROL_MESSAGE = "&c[CRASH_TRIGGER]&4";
	public static final String RESTART_CONTROL_MESSAGE =
			"&c[OpticsValleyIRC] 服务器正在重启，所有连接将被断开";
	public static final String SHUTDOWN_CONTROL_MESSAGE =
			"&c[OpticsValleyIRC] 服务器正在关闭，所有连接将被断开";
	public static final String DISCONNECT_CONTROL_MESSAGE =
			"&c[OpticsValleyIRC] 与服务器的连接已断开";

	public static final String DIOXIDE_CAPABILITY_PREFIX = "\u0000DIOXIDE_LITE\u0001";
	public static final String DIOXIDE_CAPABILITY_ADD = "A";
	public static final String DIOXIDE_CAPABILITY_REMOVE = "R";

	public static String capabilityAdd(String username) {
		return DIOXIDE_CAPABILITY_PREFIX + DIOXIDE_CAPABILITY_ADD + "|" + username;
	}
	public static String capabilityRemove(String username) {
		return DIOXIDE_CAPABILITY_PREFIX + DIOXIDE_CAPABILITY_REMOVE + "|" + username;
	}

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
