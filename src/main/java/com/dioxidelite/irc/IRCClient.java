package com.dioxidelite.irc;

import com.dioxidelite.DioxideLite;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IRC link for DioxideLite. Maintains the socket, the reconnect policy and the
 * set of IRC-online usernames (derived from the server's join/leave frames).
 *
 * <p>Security note: the legacy server "crash user" control frame is deliberately
 * ignored — a remote host must never be able to terminate the local game.</p>
 */
public final class IRCClient {

    private static final int INITIAL_RECONNECT_DELAY = 2000;
    private static final int MAX_RECONNECT_DELAY = 30000;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    private final String username;
    private final IRCClientConfig config;
    private final Object resourceLock = new Object();
    private final AtomicBoolean connectionInProgress = new AtomicBoolean(false);

    private final Set<String> onlineUsers = Collections.synchronizedSet(new LinkedHashSet<>());

    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile BufferedReader in;
    private volatile boolean connected;
    private volatile boolean shouldReconnect = true;
    private volatile int reconnectAttempts;
    private volatile int currentReconnectDelay = INITIAL_RECONNECT_DELAY;
    private volatile Thread messageListener;
    private volatile ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> reconnectTask;

    private volatile Runnable stateListener;

    public IRCClient(String username, IRCClientConfig config) {
        this.username = username;
        this.config = config;
        this.scheduler = createScheduler();
    }

    /** Optional callback fired whenever the connected flag or user set changes. */
    public void setStateListener(Runnable listener) {
        this.stateListener = listener;
    }

    /** Read-only view of the usernames currently known to be on the IRC. */
    public Set<String> onlineUsers() {
        return onlineUsers;
    }

    public String username() {
        return username;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect() {
        shouldReconnect = true;
        reconnectAttempts = 0;
        currentReconnectDelay = INITIAL_RECONNECT_DELAY;
        cancelPendingReconnect();
        submitConnect();
    }

    public void reconnect() {
        if (connected) {
            sendGameMessage("§a[OpticsValleyIRC] 已经连接到服务器");
            return;
        }
        if (connectionInProgress.get() || hasPendingReconnect()) {
            sendGameMessage("§e[OpticsValleyIRC] 已经在尝试重新连接中...");
            return;
        }
        sendGameMessage("§e[OpticsValleyIRC] 正在尝试重新连接...");
        shouldReconnect = true;
        reconnectAttempts = 0;
        currentReconnectDelay = INITIAL_RECONNECT_DELAY;
        submitConnect();
    }

    private void submitConnect() {
        try {
            getOrCreateScheduler().execute(this::tryConnect);
        } catch (RejectedExecutionException e) {
            DioxideLite.LOGGER.warn("IRC连接任务提交失败", e);
        }
    }

    private void tryConnect() {
        if (!shouldReconnect || connected || !connectionInProgress.compareAndSet(false, true)) {
            return;
        }
        Socket newSocket = new Socket();
        try {
            newSocket.connect(new InetSocketAddress(config.host(), config.port()),
                    config.connectTimeoutMillis());
            PrintWriter newOut = new PrintWriter(
                    new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader newIn = new BufferedReader(
                    new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));

            if (!shouldReconnect) {
                closeQuietly(newIn, newOut, newSocket);
                return;
            }

            synchronized (resourceLock) {
                closeResourcesLocked();
                socket = newSocket;
                out = newOut;
                in = newIn;
                connected = true;
                notifyStateChanged();
            }

            newOut.println(username);
            if (newOut.checkError()) {
                throw new IOException("发送用户名失败");
            }

            reconnectAttempts = 0;
            currentReconnectDelay = INITIAL_RECONNECT_DELAY;
            onlineUsers.clear();
            onlineUsers.add(username);
            notifyStateChanged();
            sendGameMessage("§a[OpticsValleyIRC] 已连接到IRC服务器");
            startMessageListener(newSocket, newIn);
        } catch (ConnectException e) {
            closeQuietly(null, null, newSocket);
            handleConnectionFailure("无法连接到服务器: " + e.getMessage());
        } catch (IOException e) {
            closeQuietly(null, null, newSocket);
            closeCurrentConnection(newSocket);
            handleConnectionFailure("连接失败: " + e.getMessage());
        } finally {
            connectionInProgress.set(false);
        }
    }

    private void handleConnectionFailure(String errorMessage) {
        connected = false;
        reconnectAttempts++;
        notifyStateChanged();

        if (reconnectAttempts <= MAX_RECONNECT_ATTEMPTS && shouldReconnect) {
            sendGameMessage("§c[OpticsValleyIRC] " + errorMessage);
            sendGameMessage("§e[OpticsValleyIRC] 将在" + (currentReconnectDelay / 1000)
                    + "秒后重试... (尝试 " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");
            scheduleReconnect(currentReconnectDelay);
            currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY);
        } else if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
            sendGameMessage("§4[OpticsValleyIRC] 多次重连失败，请使用/irc connect手动重连");
            shouldReconnect = false;
        }
    }

    private void scheduleReconnect(int delayMillis) {
        if (!shouldReconnect) {
            return;
        }
        cancelPendingReconnect();
        try {
            reconnectTask = getOrCreateScheduler()
                    .schedule(this::tryConnect, delayMillis, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            DioxideLite.LOGGER.warn("IRC重连任务提交失败", e);
        }
    }

    private void startMessageListener(Socket listenerSocket, BufferedReader listenerInput) {
        Thread listener = new Thread(() -> {
            try {
                String message;
                while (connected && isCurrentSocket(listenerSocket)
                        && (message = listenerInput.readLine()) != null) {
                    if (!handleIncomingMessage(message, listenerSocket)) {
                        return;
                    }
                }
                if (connected && isCurrentSocket(listenerSocket)) {
                    handleDisconnect(listenerSocket, "与服务器的连接已断开");
                }
            } catch (SocketException e) {
                handleDisconnect(listenerSocket, "连接异常: " + e.getMessage());
            } catch (IOException e) {
                handleDisconnect(listenerSocket, "读取消息失败: " + e.getMessage());
            } catch (RuntimeException e) {
                handleDisconnect(listenerSocket, "未知错误: " + e.getMessage());
                DioxideLite.LOGGER.error("IRC消息处理失败", e);
            }
        }, "IRC-MessageListener");
        listener.setDaemon(true);
        messageListener = listener;
        listener.start();
    }

    private boolean handleIncomingMessage(String message, Socket listenerSocket) {
        if (IRCProtocol.isControlMessage(message)) {
            if (IRCProtocol.CRASH_CONTROL_MESSAGE.equals(message)) {
                // Security: never honour a remote crash request. Drop it silently.
                DioxideLite.LOGGER.warn("收到服务器崩溃指令，已忽略（DioxideLite 不响应远程崩溃控制）");
                return true;
            }
            if (IRCProtocol.RESTART_CONTROL_MESSAGE.equals(message)) {
                sendGameMessage(convertColorCodes(message));
                sendGameMessage("§e[OpticsValleyIRC] 服务器正在重启，3秒后尝试重连...");
                closeCurrentConnection(listenerSocket);
                scheduleReconnect(3000);
                return false;
            }
            // SHUTDOWN / DISCONNECT
            sendGameMessage(convertColorCodes(message));
            closeCurrentConnection(listenerSocket);
            return false;
        }

        if (trackPresence(message)) {
            return true;
        }
        sendGameMessage(convertColorCodes(message));
        return true;
    }

    /** Detects the server's join/leave system frames and updates the online set. */
    private boolean trackPresence(String message) {
        String clean = message == null ? "" : message.replace('&', '§');
        String joined = extractBetween(clean, IRCProtocol.JOIN_PREFIX, IRCProtocol.JOIN_SUFFIX);
        if (joined != null) {
            String user = joined.trim();
            if (!user.isEmpty()) {
                onlineUsers.add(user);
                notifyStateChanged();
            }
            return true;
        }
        String left = extractBetween(clean, IRCProtocol.LEAVE_PREFIX, IRCProtocol.LEAVE_SUFFIX);
        if (left != null) {
            String user = left.trim();
            if (!user.isEmpty()) {
                onlineUsers.remove(user);
                notifyStateChanged();
            }
            return true;
        }
        return false;
    }

    private static String extractBetween(String text, String prefix, String suffix) {
        int start = text.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        start += prefix.length();
        int end = text.indexOf(suffix, start);
        if (end < 0) {
            return null;
        }
        return text.substring(start, end);
    }

    private void handleDisconnect(Socket disconnectedSocket, String reason) {
        if (!closeCurrentConnection(disconnectedSocket)) {
            return;
        }
        sendGameMessage("§c[OpticsValleyIRC] " + reason);
        if (shouldReconnect) {
            sendGameMessage("§e[OpticsValleyIRC] 连接断开，将在2秒后重新连接...");
            scheduleReconnect(INITIAL_RECONNECT_DELAY);
        }
    }

    public void sendMessage(String message) {
        int messageLength = countMessageCharacters(message);
        if (messageLength > config.maxMessageLength()) {
            sendGameMessage("§c[OpticsValleyIRC] 消息不能超过"
                    + config.maxMessageLength() + "个字符（当前" + messageLength + "个）");
            return;
        }
        if (!connected) {
            sendGameMessage("§c[OpticsValleyIRC] 未连接到服务器，无法发送消息");
            if (shouldReconnect) {
                reconnect();
            } else {
                sendGameMessage("§e[OpticsValleyIRC] 使用/irc connect重新连接到服务器");
            }
            return;
        }
        try {
            getOrCreateScheduler().execute(() -> {
                Socket currentSocket = socket;
                PrintWriter currentOut = out;
                if (!connected || currentSocket == null || currentOut == null) {
                    sendGameMessage("§c[OpticsValleyIRC] 未连接到服务器，无法发送消息");
                    return;
                }
                currentOut.println(message);
                if (currentOut.checkError()) {
                    handleDisconnect(currentSocket, "发送消息时连接断开");
                }
            });
        } catch (RejectedExecutionException e) {
            sendGameMessage("§c[OpticsValleyIRC] 发送消息失败");
        }
    }

    static int countMessageCharacters(String message) {
        return message == null ? 0 : message.codePointCount(0, message.length());
    }

    private boolean closeCurrentConnection(Socket expectedSocket) {
        synchronized (resourceLock) {
            if (expectedSocket != null && socket != expectedSocket) {
                return false;
            }
            boolean wasConnected = connected;
            connected = false;
            closeResourcesLocked();
            onlineUsers.clear();
            notifyStateChanged();
            return wasConnected;
        }
    }

    private void closeResourcesLocked() {
        Socket currentSocket = socket;
        BufferedReader currentIn = in;
        PrintWriter currentOut = out;
        Thread currentListener = messageListener;
        socket = null;
        in = null;
        out = null;
        messageListener = null;
        if (currentListener != null && currentListener != Thread.currentThread()) {
            currentListener.interrupt();
        }
        closeQuietly(currentIn, currentOut, currentSocket);
    }

    private static void closeQuietly(BufferedReader reader, PrintWriter writer, Socket targetSocket) {
        if (targetSocket != null) {
            try {
                targetSocket.close();
            } catch (IOException ignored) {
                // closed
            }
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // closed
            }
        }
        if (writer != null) {
            writer.close();
        }
    }

    public void disconnect() {
        shouldReconnect = false;
        cancelPendingReconnect();
        closeCurrentConnection(null);
        connectionInProgress.set(false);
        ScheduledExecutorService currentScheduler = scheduler;
        if (currentScheduler != null) {
            currentScheduler.shutdownNow();
        }
        sendGameMessage("§7[OpticsValleyIRC] 已断开IRC连接");
    }

    private boolean isCurrentSocket(Socket expectedSocket) {
        return socket == expectedSocket;
    }

    private synchronized ScheduledExecutorService getOrCreateScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = createScheduler();
        }
        return scheduler;
    }

    private ScheduledExecutorService createScheduler() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "IRC-Network");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void cancelPendingReconnect() {
        ScheduledFuture<?> currentTask = reconnectTask;
        if (currentTask != null) {
            currentTask.cancel(false);
            reconnectTask = null;
        }
    }

    private boolean hasPendingReconnect() {
        ScheduledFuture<?> currentTask = reconnectTask;
        return currentTask != null && !currentTask.isDone();
    }

    private void notifyStateChanged() {
        Runnable listener = stateListener;
        if (listener != null) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                DioxideLite.LOGGER.warn("IRC状态监听器执行失败", e);
            }
        }
    }

    private void sendGameMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) {
            DioxideLite.LOGGER.info("[IRC] {}", message);
            return;
        }
        client.execute(() -> client.gui.getChat().addClientSystemMessage(Component.literal(message)));
    }

    private static String convertColorCodes(String message) {
        return message == null ? "" : message.replace('&', '§');
    }
}
