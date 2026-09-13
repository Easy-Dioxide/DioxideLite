package com.opticsvalley.irc;

import com.opticsvalley.protocol.IRCProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

public class ClientHandler implements Runnable {
	static final int MAX_USERNAME_LENGTH = 32;
	static final int MAX_MESSAGE_LENGTH = 80;
	private static final long RATE_LIMIT_WARNING_INTERVAL_NANOS = 1_000_000_000L;
	private static final int MAX_DISCARDED_CHARACTERS = 4096;
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

	private final Socket socket;
	private final IRCServer server;
	private final ServerConfig config;
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final BlockingQueue<String> outboundMessages;
	private final RateLimiter rateLimiter;
	private final Object writerLock = new Object();

	private volatile String username;
	private volatile boolean dioxideClient;
	private volatile BufferedReader in;
	private volatile PrintWriter out;
	private volatile Thread messageWriter;
	private int rateLimitViolations;
	private long lastRateLimitWarning;

	public ClientHandler(Socket socket, IRCServer server) {
		this.socket = socket;
		this.server = server;
		this.config = server.getConfig();
		this.outboundMessages = new ArrayBlockingQueue<>(config.outboundQueueCapacity());
		this.rateLimiter = new RateLimiter(
				config.rateLimitPerSecond(),
				config.rateLimitBurst(),
				System::nanoTime
		);
	}

	@Override
	public void run() {
		try {
			socket.setSoTimeout(config.handshakeTimeoutMillis());
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
			startMessageWriter();

			String requestedUsername = readLimitedLine(in, config.maxUsernameLength());
			if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
				sendFinalMessage("&c[OpticsValleyIRC] 用户名不能为空");
				return;
			}

			socket.setSoTimeout(0);
			username = server.addClient(requestedUsername.trim(), this);

			while (running.get()) {
				String message;
				try {
					message = readLimitedLine(in, config.maxMessageLength());
				} catch (LineTooLongException e) {
					if (!discardLineRemainder(in, MAX_DISCARDED_CHARACTERS)) {
						sendFinalMessage("&c[OpticsValleyIRC] 消息异常过长，连接已断开");
						return;
					}
					sendMessage("&c[OpticsValleyIRC] 消息不能超过"
							+ config.maxMessageLength() + "个字符，当前消息未发送");
					continue;
				}

				if (message == null) {
					return;
				}

				if (!rateLimiter.tryAcquire()) {
					handleRateLimitViolation();
					if (rateLimitViolations >= config.maxRateLimitViolations()) {
						sendFinalMessage("&c[OpticsValleyIRC] 消息发送过于频繁，连接已断开");
						return;
					}
					continue;
				}

				rateLimitViolations = Math.max(0, rateLimitViolations - 1);
				if (server.handleCapabilityFrame(this, message)) continue;
				server.broadcast(username, message);
			}
		} catch (SocketTimeoutException e) {
			sendFinalMessage("&c[OpticsValleyIRC] 登录超时，请重新连接");
		} catch (LineTooLongException e) {
			sendFinalMessage("&c[OpticsValleyIRC] 用户名不能超过"
					+ config.maxUsernameLength() + "个字符");
		} catch (SocketException e) {
			LOGGER.info("客户端连接断开: {}", displayUsername());
		} catch (IOException e) {
			LOGGER.warn("客户端连接异常: {} - {}", displayUsername(), e.getMessage());
		} finally {
			disconnect();
		}
	}

	private void handleRateLimitViolation() {
		rateLimitViolations++;
		long now = System.nanoTime();
		if (now - lastRateLimitWarning >= RATE_LIMIT_WARNING_INTERVAL_NANOS) {
			lastRateLimitWarning = now;
			sendMessage("&e[OpticsValleyIRC] 消息发送过快，当前消息已被丢弃");
		}
	}

	private void startMessageWriter() {
		Thread writer = new Thread(() -> {
			try {
				while (running.get()) {
					String message = outboundMessages.take();
					PrintWriter currentOut = out;
					if (currentOut == null) {
						return;
					}

					synchronized (writerLock) {
						currentOut.println(message);
						if (currentOut.checkError()) {
							throw new IOException("写入客户端失败");
						}
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (IOException e) {
				if (running.get()) {
					LOGGER.warn("发送消息给 {} 失败: {}", displayUsername(), e.getMessage());
					disconnect();
				}
			}
		}, "IRC-MessageWriter-" + socket.getRemoteSocketAddress());
		writer.setDaemon(true);
		messageWriter = writer;
		writer.start();
	}

	public void sendMessage(String message) {
		if (!running.get()) {
			return;
		}

		if (!outboundMessages.offer(message)) {
			LOGGER.warn("客户端发送队列已满，断开慢客户端: {}", displayUsername());
			disconnect();
		}
	}

	public void sendControlMessage(String message) {
		if (!IRCProtocol.isControlMessage(message)) {
			throw new IllegalArgumentException("拒绝发送未注册的IRC控制消息");
		}
		sendMessage(message);
	}

	private void sendFinalMessage(String message) {
		PrintWriter currentOut = out;
		if (currentOut == null || socket.isClosed()) {
			return;
		}

		synchronized (writerLock) {
			currentOut.println(message);
			currentOut.flush();
		}
	}

	String getUsernameForProtocol() { return username; }

	boolean isDioxideClient() { return dioxideClient; }

	void setDioxideClient(boolean value) { dioxideClient = value; }

	public void disconnect() {
		if (!running.compareAndSet(true, false)) {
			return;
		}

		String registeredUsername = username;
		username = null;
		if (registeredUsername != null) {
			server.removeClient(registeredUsername, this);
		}

		Thread writer = messageWriter;
		messageWriter = null;
		if (writer != null && writer != Thread.currentThread()) {
			writer.interrupt();
		}

		try {
			socket.close();
		} catch (IOException ignored) {
			// 套接字已经关闭时无需额外处理。
		}

		BufferedReader currentIn = in;
		in = null;
		if (currentIn != null) {
			try {
				currentIn.close();
			} catch (IOException ignored) {
				// 输入流已经关闭时无需额外处理。
			}
		}

		PrintWriter currentOut = out;
		out = null;
		if (currentOut != null) {
			currentOut.close();
		}

		outboundMessages.clear();
		server.unregisterConnection(this);
	}

	private String displayUsername() {
		return username == null ? "未知用户" : username;
	}

	static String readLimitedLine(BufferedReader reader, int maxCodePoints) throws IOException {
		StringBuilder line = new StringBuilder(Math.min(maxCodePoints, 128));
		boolean receivedAnyCharacter = false;

		while (true) {
			int next = reader.read();
			if (next == -1) {
				return receivedAnyCharacter ? line.toString() : null;
			}

			receivedAnyCharacter = true;
			if (next == '\n') {
				return line.toString();
			}
			if (next == '\r') {
				continue;
			}

			line.append((char) next);
			if (line.codePointCount(0, line.length()) > maxCodePoints) {
				throw new LineTooLongException(maxCodePoints);
			}
		}
	}

	static boolean discardLineRemainder(BufferedReader reader, int maxCharacters) throws IOException {
		int discarded = 0;
		while (discarded <= maxCharacters) {
			int next = reader.read();
			if (next == -1 || next == '\n') {
				return true;
			}
			discarded++;
		}
		return false;
	}

	static final class LineTooLongException extends IOException {
		LineTooLongException(int maxCodePoints) {
			super("行内容超过" + maxCodePoints + "个字符");
		}
	}

	static final class RateLimiter {
		private final double permitsPerSecond;
		private final int burstCapacity;
		private final LongSupplier nanoTime;
		private double availablePermits;
		private long lastRefillTime;

		RateLimiter(double permitsPerSecond, int burstCapacity, LongSupplier nanoTime) {
			this.permitsPerSecond = permitsPerSecond;
			this.burstCapacity = burstCapacity;
			this.nanoTime = nanoTime;
			this.availablePermits = burstCapacity;
			this.lastRefillTime = nanoTime.getAsLong();
		}

		synchronized boolean tryAcquire() {
			long now = nanoTime.getAsLong();
			long elapsed = Math.max(0, now - lastRefillTime);
			availablePermits = Math.min(
					burstCapacity,
					availablePermits + elapsed / 1_000_000_000.0 * permitsPerSecond
			);
			lastRefillTime = now;

			if (availablePermits < 1.0) {
				return false;
			}
			availablePermits -= 1.0;
			return true;
		}
	}
}
