package com.opticsvalley.irc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ServerConfig(
		String bindAddress,
		int port,
		int maxClients,
		boolean guiEnabled,
		int maxUsernameLength,
		int maxMessageLength,
		int handshakeTimeoutMillis,
		double rateLimitPerSecond,
		int rateLimitBurst,
		int maxRateLimitViolations,
		int outboundQueueCapacity,
		Path banFile
) {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);
	private static final Path DEFAULT_CONFIG_PATH = Path.of("irc-server.properties");

	public static ServerConfig load(String[] args) throws IOException {
		Path configPath = DEFAULT_CONFIG_PATH;
		boolean noGui = false;
		for (String argument : args) {
			if (argument.startsWith("--config=")) {
				configPath = Path.of(argument.substring("--config=".length()));
			} else if ("--nogui".equalsIgnoreCase(argument)) {
				noGui = true;
			}
		}
		return load(configPath, noGui);
	}

	static ServerConfig load(Path configPath, boolean forceNoGui) throws IOException {
		Path absoluteConfigPath = configPath.toAbsolutePath().normalize();
		Properties properties = defaultProperties();
		Path parent = absoluteConfigPath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		if (Files.exists(absoluteConfigPath)) {
			try (Reader reader = Files.newBufferedReader(absoluteConfigPath, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
		} else {
			try (Writer writer = Files.newBufferedWriter(absoluteConfigPath, StandardCharsets.UTF_8)) {
				properties.store(writer, "OpticsValley IRC server configuration");
			}
			LOGGER.info("已创建默认服务端配置: {}", absoluteConfigPath);
		}

		Path banPath = Path.of(properties.getProperty("storage.ban-file", "bans.json").trim());
		if (!banPath.isAbsolute() && parent != null) {
			banPath = parent.resolve(banPath);
		}

		return new ServerConfig(
				properties.getProperty("server.bind-address", "0.0.0.0").trim(),
				readInt(properties, "server.port", 16688, 1, 65535),
				readInt(properties, "server.max-clients", 100, 1, 10000),
				!forceNoGui && readBoolean(properties, "server.gui", true),
				readInt(properties, "username.max-length", 32, 1, 128),
				readInt(properties, "message.max-length", 80, 1, 10000),
				readInt(properties, "connection.handshake-timeout-millis", 10000, 1000, 120000),
				readDouble(properties, "rate-limit.messages-per-second", 5.0, 0.1, 1000.0),
				readInt(properties, "rate-limit.burst", 10, 1, 10000),
				readInt(properties, "rate-limit.max-violations", 20, 1, 10000),
				readInt(properties, "connection.outbound-queue-capacity", 100, 1, 10000),
				banPath.toAbsolutePath().normalize()
		);
	}

	private static Properties defaultProperties() {
		Properties properties = new Properties();
		properties.setProperty("server.bind-address", "0.0.0.0");
		properties.setProperty("server.port", "16688");
		properties.setProperty("server.max-clients", "100");
		properties.setProperty("server.gui", "true");
		properties.setProperty("username.max-length", "32");
		properties.setProperty("message.max-length", "80");
		properties.setProperty("connection.handshake-timeout-millis", "10000");
		properties.setProperty("connection.outbound-queue-capacity", "100");
		properties.setProperty("rate-limit.messages-per-second", "5.0");
		properties.setProperty("rate-limit.burst", "10");
		properties.setProperty("rate-limit.max-violations", "20");
		properties.setProperty("storage.ban-file", "bans.json");
		return properties;
	}

	private static int readInt(Properties properties, String key, int fallback, int min, int max) {
		try {
			int value = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
			return Math.max(min, Math.min(max, value));
		} catch (NumberFormatException e) {
			LOGGER.warn("配置项 {} 不是有效整数，使用默认值 {}", key, fallback);
			return fallback;
		}
	}

	private static double readDouble(
			Properties properties,
			String key,
			double fallback,
			double min,
			double max
	) {
		try {
			double value = Double.parseDouble(properties.getProperty(key, Double.toString(fallback)).trim());
			return Math.max(min, Math.min(max, value));
		} catch (NumberFormatException e) {
			LOGGER.warn("配置项 {} 不是有效数字，使用默认值 {}", key, fallback);
			return fallback;
		}
	}

	private static boolean readBoolean(Properties properties, String key, boolean fallback) {
		String value = properties.getProperty(key);
		return value == null ? fallback : Boolean.parseBoolean(value.trim());
	}
}
