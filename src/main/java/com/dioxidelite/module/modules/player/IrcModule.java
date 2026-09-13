package com.dioxidelite.module.modules.player;

import com.dioxidelite.DioxideLite;
import com.dioxidelite.irc.IRCClient;
import com.dioxidelite.irc.IRCClientConfig;
import com.dioxidelite.module.Category;
import com.dioxidelite.module.Module;
import com.dioxidelite.setting.settings.IntSetting;
import com.dioxidelite.setting.settings.StringSetting;

import java.util.Set;

/**
 * IRC link for DioxideLite (Player category, enabled by default).
 *
 * <p>When enabled the client keeps a lightweight IRC connection to the
 * configured server, mirrors chat frames into the game chat and tracks the
 * online usernames so their name tags can carry the DioxideLite logo.</p>
 *
 * <p>No automation: this is a plain chat bridge, it never sends packets on
 * behalf of the player and ignores the legacy remote "crash" control.</p>
 */
public final class IrcModule extends Module {

    public static final IrcModule INSTANCE = new IrcModule();

    public final StringSetting host = add(new StringSetting("Host", IRCClientConfig.DEFAULT_HOST));
    public final IntSetting port = add(new IntSetting("Port", IRCClientConfig.DEFAULT_PORT, 1, 65535, 1));

    private volatile IRCClient client;

    private IrcModule() {
        super("IRC", Category.PLAYER);
        setEnabled(true);
    }

    @Override
    protected void onEnable() {
        // Session may not be ready during mod bootstrap; defer the first connect
        // into the main loop where the username is guaranteed to exist.
        mc.execute(() -> {
            if (isEnabled() && client == null) {
                connectIrc();
            }
        });
    }

    @Override
    protected void onDisable() {
        IRCClient current = client;
        client = null;
        if (current != null) {
            current.disconnect();
        }
    }

    /** (Re)builds the IRC client from the current settings and connects. */
    public synchronized void connectIrc() {
        IRCClient current = client;
        if (current != null) {
            current.disconnect();
            client = null;
        }
        String username = mc.getUser().getName();
        if (username == null || username.isBlank()) {
            DioxideLite.LOGGER.warn("IRC: 无法获取玩家名，跳过连接");
            return;
        }
        IRCClientConfig config = new IRCClientConfig(
                host.get().isBlank() ? IRCClientConfig.DEFAULT_HOST : host.get().trim(),
                port.get(),
                IRCClientConfig.DEFAULT_MAX_MESSAGE_LENGTH,
                IRCClientConfig.DEFAULT_CONNECT_TIMEOUT_MILLIS
        );
        IRCClient fresh = new IRCClient(username, config);
        fresh.setStateListener(this::onStateChanged);
        client = fresh;
        fresh.connect();
    }

    /** Disconnects and stops background threads. */
    public synchronized void disconnectIrc() {
        IRCClient current = client;
        client = null;
        if (current != null) {
            current.disconnect();
        }
    }

    public IRCClient client() {
        return client;
    }

    public boolean isConnected() {
        IRCClient current = client;
        return current != null && current.isConnected();
    }

    /** True when the given (clean, lowercase-compared) username is IRC-online. */
    public static boolean isIrcUser(String username) {
        IrcModule module = INSTANCE;
        if (!module.isEnabled()) {
            return false;
        }
        IRCClient current = module.client;
        if (current == null || username == null) {
            return false;
        }
        return current.onlineUsers().contains(username);
    }

    /** Read-only view of the tracked IRC-online usernames. */
    public static Set<String> ircOnlineUsers() {
        IRCClient current = INSTANCE.client;
        return current == null ? Set.of() : current.onlineUsers();
    }

    private void onStateChanged() {
        // State consumers (Dynamic Island, ClickGUI) read IrcModule lazily,
        // so a no-op wake-up is enough.
    }
}
