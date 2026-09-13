package com.dioxidelite.irc;

/**
 * Immutable connection parameters for the DioxideLite IRC link.
 * Values are fed from the {@code IrcModule} settings at connect time.
 */
public record IRCClientConfig(
        String host,
        int port,
        int maxMessageLength,
        int connectTimeoutMillis
) {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 16688;
    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 80;
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

    public static IRCClientConfig defaults() {
        return new IRCClientConfig(
                DEFAULT_HOST,
                DEFAULT_PORT,
                DEFAULT_MAX_MESSAGE_LENGTH,
                DEFAULT_CONNECT_TIMEOUT_MILLIS
        );
    }
}
