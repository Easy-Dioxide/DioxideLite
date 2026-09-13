package com.opticsvalley.irc;

import com.opticsvalley.protocol.IRCProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IRCServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(IRCServer.class);
	private static final long CONTROL_MESSAGE_DELIVERY_DELAY_MILLIS = 200L;

	private final Object lifecycleLock = new Object();
	private final ServerConfig config;
	private final BanStore banStore;
	private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
	private final Set<ClientHandler> connections = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashMap<String, String> banReasons = new ConcurrentHashMap<>();
	private final ExecutorService clientExecutor;
	private final ExecutorService lifecycleExecutor;
	private final Semaphore connectionSlots;
	private final Scanner consoleInput = new Scanner(System.in);

	private volatile ServerSocket serverSocket;
	private volatile ServerGUI gui;
	private volatile ServerState state = ServerState.STOPPED;

	public IRCServer(ServerConfig config) throws IOException {
		this.config = config;
		this.banStore = new BanStore(config.banFile());
		this.banReasons.putAll(banStore.load());
		this.clientExecutor = createClientExecutor(config.maxClients());
		this.connectionSlots = new Semaphore(config.maxClients(), true);
		this.lifecycleExecutor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "IRC-Lifecycle");
			thread.setDaemon(false);
			return thread;
		});
	}

	public void start() throws IOException {
		synchronized (lifecycleLock) {
			if (state != ServerState.STOPPED) {
				throw new IllegalStateException("IRC服务端已经启动");
			}
			state = ServerState.STARTING;
			try {
				serverSocket = createServerSocket();
				state = ServerState.RUNNING;
			} catch (IOException e) {
				state = ServerState.STOPPED;
				throw e;
			}
		}

		LOGGER.info(
				"OpticsValley IRC {} 已启动，监听 {}:{}，最大连接数 {}",
				getVersion(),
				config.bindAddress(),
				config.port(),
				config.maxClients()
		);
		LOGGER.info("已加载 {} 条封禁记录", banReasons.size());

		startGUI();
		startCommandProcessor();
		acceptConnections(serverSocket);
	}

	private static ExecutorService createClientExecutor(int maxClients) {
		AtomicInteger threadNumber = new AtomicInteger();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				maxClients,
				maxClients,
				60L,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(maxClients),
				runnable -> {
					Thread thread = new Thread(
							runnable,
							"IRC-Client-" + threadNumber.incrementAndGet()
					);
					thread.setDaemon(true);
					return thread;
				},
				new ThreadPoolExecutor.AbortPolicy()
		);
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	private ServerSocket createServerSocket() throws IOException {
		ServerSocket socket = new ServerSocket();
		socket.setReuseAddress(true);
		socket.bind(
				new InetSocketAddress(config.bindAddress(), config.port()),
				config.maxClients()
		);
		return socket;
	}

	private void acceptConnections(ServerSocket listeningSocket) {
		while (state == ServerState.RUNNING
				&& serverSocket == listeningSocket
				&& !listeningSocket.isClosed()) {
			try {
				Socket clientSocket = listeningSocket.accept();
				clientSocket.setTcpNoDelay(true);

				if (!connectionSlots.tryAcquire()) {
					rejectConnection(clientSocket, "&c[OpticsValleyIRC] 服务器连接人数已满");
					continue;
				}

				ClientHandler handler = new ClientHandler(clientSocket, this);
				connections.add(handler);
				try {
					clientExecutor.execute(handler);
				} catch (RejectedExecutionException e) {
					handler.disconnect();
				}
			} catch (IOException e) {
				if (state == ServerState.RUNNING && serverSocket == listeningSocket) {
					LOGGER.error("接受客户端连接失败", e);
				}
			}
		}
	}

	private void rejectConnection(Socket clientSocket, String message) {
		try (Socket rejectedSocket = clientSocket;
		     PrintWriter writer = new PrintWriter(
				     new OutputStreamWriter(
						     rejectedSocket.getOutputStream(),
						     StandardCharsets.UTF_8
				     ),
				     true
		     )) {
			writer.println(message);
		} catch (IOException e) {
			LOGGER.debug("拒绝客户端连接时写入提示失败", e);
		}
	}

	void unregisterConnection(ClientHandler handler) {
		if (connections.remove(handler)) {
			connectionSlots.release();
		}
	}

	private void startGUI() {
		if (!config.guiEnabled()) {
			LOGGER.info("GUI已通过配置或 --nogui 禁用");
			return;
		}
		if (GraphicsEnvironment.isHeadless()) {
			LOGGER.warn("当前环境不支持图形界面，服务端将以无GUI模式运行");
			return;
		}

		SwingUtilities.invokeLater(() -> {
			gui = new ServerGUI(this);
			updateGUIUserList();
		});
	}

	private void startCommandProcessor() {
		Thread commandThread = new Thread(() -> {
			while (state != ServerState.STOPPED) {
				try {
					if (consoleInput.hasNextLine()) {
						processCommand(consoleInput.nextLine().trim());
					}
				} catch (NoSuchElementException | IllegalStateException e) {
					return;
				}
			}
		}, "IRC-Console");
		commandThread.setDaemon(true);
		commandThread.start();
	}

	public void processCommand(String command) {
		if (command == null || command.isBlank()) {
			return;
		}

		String[] parts = command.split("\\s+", 3);
		String cmd = parts[0].toLowerCase(Locale.ROOT);
		String arg = parts.length > 1 ? parts[1] : "";
		String reason = parts.length > 2 ? parts[2] : "未指定原因";

		switch (cmd) {
			case "ban", "/ban" -> {
				if (arg.isEmpty()) {
					LOGGER.info("用法: ban <用户名> [原因]");
				} else {
					banUser(arg, reason);
				}
			}
			case "unban", "/unban" -> {
				if (arg.isEmpty()) {
					LOGGER.info("用法: unban <用户名>");
				} else {
					unbanUser(arg);
				}
			}
			case "crash", "/crash" -> {
				if (arg.isEmpty()) {
					LOGGER.info("用法: crash <用户名>");
				} else {
					crashUser(arg, "CONSOLE");
				}
			}
			case "opengui", "/opengui" -> openGUI();
			case "stop", "/stop" -> shutdown();
			case "reboot", "/reboot" -> restart();
			default -> LOGGER.info("未知命令: {}", safeForLog(cmd));
		}
	}

	private void openGUI() {
		if (GraphicsEnvironment.isHeadless()) {
			LOGGER.warn("当前环境不支持图形界面");
			return;
		}
		SwingUtilities.invokeLater(() -> {
			if (gui == null) {
				gui = new ServerGUI(this);
			} else {
				gui.setVisible(true);
				gui.toFront();
				gui.requestFocus();
			}
			updateGUIUserList();
		});
	}

	private void updateGUIUserList() {
		ServerGUI currentGUI = gui;
		if (currentGUI != null) {
			List<String> userList = new ArrayList<>(clients.keySet());
			userList.sort(String.CASE_INSENSITIVE_ORDER);
			currentGUI.updateUserList(userList);
		}
	}

	public void banUser(String username, String reason) {
		banReasons.put(username, reason);
		saveBans();
		LOGGER.warn("AUDIT action=ban target={} reason={}", safeForLog(username), safeForLog(reason));

		ClientHandler client = clients.get(username);
		if (client != null) {
			client.sendMessage("&c[OpticsValleyIRC] 你已被封禁，无法发送消息！原因: " + reason);
			broadcastSystemMessage(
					"&c[OpticsValleyIRC] 用户 " + username + " 已被封禁，原因: " + reason
			);
		}
		updateGUIUserList();
	}

	public void unbanUser(String username) {
		if (banReasons.remove(username) == null) {
			LOGGER.info("该用户未被封禁: {}", safeForLog(username));
			return;
		}

		saveBans();
		LOGGER.warn("AUDIT action=unban target={}", safeForLog(username));
		ClientHandler client = clients.get(username);
		if (client != null) {
			client.sendMessage("&a[OpticsValleyIRC] 你已被解封，可以正常聊天了！");
		}
		broadcastSystemMessage("&a[OpticsValleyIRC] 用户 " + username + " 已被解封");
	}

	private void saveBans() {
		try {
			banStore.save(banReasons);
		} catch (IOException e) {
			LOGGER.error("保存封禁记录失败: {}", config.banFile(), e);
		}
	}

	public boolean isUserBanned(String username) {
		return banReasons.containsKey(username);
	}

	public void broadcast(String username, String message) {
		if (isUserBanned(username)) {
			ClientHandler sender = clients.get(username);
			if (sender != null) {
				String reason = banReasons.getOrDefault(username, "未指定原因");
				sender.sendMessage("&c[OpticsValleyIRC] 你已被封禁，无法发送消息！原因: " + reason);
			}
			return;
		}

		LOGGER.info("[{}]: {}", safeForLog(username), safeForLog(message));
		String formattedMessage = "&e[OpticsValleyIRC]&a<" + username + ">&r: " + message;
		for (ClientHandler client : clients.values()) {
			client.sendMessage(formattedMessage);
		}
	}

	boolean handleCapabilityFrame(ClientHandler sender, String message) {
		String prefix = com.opticsvalley.protocol.IRCProtocol.DIOXIDE_CAPABILITY_PREFIX;
		if (message == null || !message.startsWith(prefix)) return false;
		String payload = message.substring(prefix.length());
		int separator = payload.indexOf('|');
		if (separator <= 0 || separator >= payload.length() - 1) return true;
		String action = payload.substring(0, separator);
		String requestedUser = payload.substring(separator + 1).trim();
		String username = sender.getUsernameForProtocol();
		if (username == null || !username.equals(requestedUser)) return true;

		if (com.opticsvalley.protocol.IRCProtocol.DIOXIDE_CAPABILITY_ADD.equals(action)) {
			if (!sender.isDioxideClient()) {
				sender.setDioxideClient(true);
				for (ClientHandler client : clients.values()) {
					if (client != sender && client.isDioxideClient()) {
						client.sendMessage(com.opticsvalley.protocol.IRCProtocol.capabilityAdd(username));
					}
				}
				for (ClientHandler client : clients.values()) {
					if (client != sender && client.isDioxideClient()) {
						String peer = client.getUsernameForProtocol();
						if (peer != null) sender.sendMessage(com.opticsvalley.protocol.IRCProtocol.capabilityAdd(peer));
					}
				}
			}
			return true;
		}
		if (com.opticsvalley.protocol.IRCProtocol.DIOXIDE_CAPABILITY_REMOVE.equals(action)) {
			if (sender.isDioxideClient()) {
				sender.setDioxideClient(false);
				for (ClientHandler client : clients.values()) {
					if (client != sender && client.isDioxideClient()) {
						client.sendMessage(com.opticsvalley.protocol.IRCProtocol.capabilityRemove(username));
					}
				}
			}
			return true;
		}
		return true;
	}

	public String addClient(String requestedUsername, ClientHandler handler) {
		String username = registerUniqueUsername(clients, requestedUsername, handler);
		if (!username.equals(requestedUsername)) {
			handler.sendMessage("&e[OpticsValleyIRC] 用户名 " + requestedUsername
					+ " 已被占用，你的IRC用户名已调整为 " + username);
		}

		LOGGER.info(
				"用户连接: requested={} assigned={}",
				safeForLog(requestedUsername),
				safeForLog(username)
		);
		broadcastSystemMessage("&a[OpticsValleyIRC] 用户 " + username + " 已加入IRC");

		if (isUserBanned(username)) {
			String reason = banReasons.getOrDefault(username, "未指定原因");
			handler.sendMessage("&c[OpticsValleyIRC] 你已被封禁，无法发送消息！原因: " + reason);
		}
		updateGUIUserList();
		return username;
	}

	static <T> String registerUniqueUsername(
			ConcurrentHashMap<String, T> clientMap,
			String requestedUsername,
			T handler
	) {
		String username = requestedUsername;
		int suffix = 2;
		while (clientMap.putIfAbsent(username, handler) != null) {
			username = requestedUsername + String.format(Locale.ROOT, "%02d", suffix++);
		}
		return username;
	}

	public void removeClient(String username, ClientHandler handler) {
		if (!clients.remove(username, handler)) {
			return;
		}
		LOGGER.info("用户断开: {}", safeForLog(username));
		if (handler.isDioxideClient()) {
			for (ClientHandler client : clients.values()) {
				if (client.isDioxideClient()) {
					client.sendMessage(com.opticsvalley.protocol.IRCProtocol.capabilityRemove(username));
				}
			}
		}
		broadcastSystemMessage("&7[OpticsValleyIRC] 用户 " + username + " 已离开IRC");
		updateGUIUserList();
	}

	public void broadcastSystemMessage(String message) {
		LOGGER.info("System: {}", safeForLog(stripColorCodes(message)));
		for (ClientHandler client : clients.values()) {
			client.sendMessage(message);
		}
	}

	private void broadcastControlMessage(String message) {
		if (!IRCProtocol.isControlMessage(message)) {
			throw new IllegalArgumentException("拒绝广播未注册的IRC控制消息");
		}
		for (ClientHandler client : clients.values()) {
			client.sendControlMessage(message);
		}
	}

	public void crashUser(String username) {
		crashUser(username, "LOCAL");
	}

	public void crashUser(String username, String source) {
		ClientHandler client = clients.get(username);
		if (client == null) {
			LOGGER.info("用户 {} 不在线", safeForLog(username));
			return;
		}

		LOGGER.warn(
				"AUDIT action=crash target={} source={}",
				safeForLog(username),
				safeForLog(source)
		);
		client.sendControlMessage(IRCProtocol.CRASH_CONTROL_MESSAGE);
		broadcastSystemMessage("&4[OpticsValleyIRC] 管理员已使用户 "
				+ username + " 的游戏崩溃");
	}

	public void restart() {
		synchronized (lifecycleLock) {
			if (state != ServerState.RUNNING) {
				LOGGER.warn("当前状态 {} 不允许重启", state);
				return;
			}
			state = ServerState.RESTARTING;
		}

		try {
			lifecycleExecutor.execute(this::performRestart);
		} catch (RejectedExecutionException e) {
			LOGGER.warn("服务端生命周期线程已经关闭，无法重启");
		}
	}

	private void performRestart() {
		LOGGER.info("正在重启服务器...");
		broadcastControlMessage(IRCProtocol.RESTART_CONTROL_MESSAGE);
		waitForControlMessageDelivery();
		closeAllConnections();
		closeServerSocket();

		if (state != ServerState.RESTARTING) {
			return;
		}

		try {
			ServerSocket newSocket = createServerSocket();
			synchronized (lifecycleLock) {
				if (state != ServerState.RESTARTING) {
					newSocket.close();
					return;
				}
				serverSocket = newSocket;
				state = ServerState.RUNNING;
			}

			Thread acceptor = new Thread(
					() -> acceptConnections(newSocket),
					"IRC-Acceptor"
			);
			acceptor.setDaemon(false);
			acceptor.start();
			updateGUIUserList();
			LOGGER.info("服务器已在 {}:{} 重启完成", config.bindAddress(), config.port());
		} catch (IOException e) {
			LOGGER.error("重启后重新绑定端口失败", e);
			synchronized (lifecycleLock) {
				state = ServerState.STOPPING;
			}
			performShutdown();
		}
	}

	public void shutdown() {
		synchronized (lifecycleLock) {
			if (state == ServerState.STOPPING || state == ServerState.STOPPED) {
				return;
			}
			state = ServerState.STOPPING;
		}

		try {
			lifecycleExecutor.execute(this::performShutdown);
		} catch (RejectedExecutionException e) {
			LOGGER.warn("服务端生命周期线程已经关闭");
		}
	}

	private void performShutdown() {
		LOGGER.info("正在关闭服务器...");
		broadcastControlMessage(IRCProtocol.SHUTDOWN_CONTROL_MESSAGE);
		waitForControlMessageDelivery();
		closeAllConnections();
		closeServerSocket();
		clientExecutor.shutdownNow();
		saveBans();

		ServerGUI currentGUI = gui;
		gui = null;
		if (currentGUI != null) {
			SwingUtilities.invokeLater(currentGUI::dispose);
		}

		synchronized (lifecycleLock) {
			state = ServerState.STOPPED;
		}
		LOGGER.info("服务器已关闭");
		lifecycleExecutor.shutdown();
	}

	private void waitForControlMessageDelivery() {
		try {
			Thread.sleep(CONTROL_MESSAGE_DELIVERY_DELAY_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void closeAllConnections() {
		for (ClientHandler handler : new ArrayList<>(connections)) {
			handler.disconnect();
		}
		clients.clear();
	}

	private void closeServerSocket() {
		ServerSocket currentSocket = serverSocket;
		serverSocket = null;
		if (currentSocket != null && !currentSocket.isClosed()) {
			try {
				currentSocket.close();
			} catch (IOException e) {
				LOGGER.warn("关闭服务端监听套接字失败", e);
			}
		}
	}

	public ServerConfig getConfig() {
		return config;
	}

	public ServerState getState() {
		return state;
	}

	private static String stripColorCodes(String message) {
		return message.replaceAll("&[0-9a-fk-or]", "");
	}

	private static String safeForLog(String value) {
		return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
	}

	private static String getVersion() {
		String version = IRCServer.class.getPackage().getImplementationVersion();
		return version == null ? "development" : version;
	}

	public static void main(String[] args) {
		try {
			ServerConfig config = ServerConfig.load(args);
			IRCServer server = new IRCServer(config);
			server.start();
		} catch (Exception e) {
			LOGGER.error("启动服务器失败", e);
			System.exit(1);
		}
	}

	public enum ServerState {
		STOPPED,
		STARTING,
		RUNNING,
		RESTARTING,
		STOPPING
	}
}
