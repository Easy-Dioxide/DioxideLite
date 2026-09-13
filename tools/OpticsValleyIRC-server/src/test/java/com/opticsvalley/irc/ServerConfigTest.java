package com.opticsvalley.irc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void createsDefaultConfigurationAndSupportsNoGuiOverride() throws Exception {
		Path configPath = temporaryDirectory.resolve("irc-server.properties");

		ServerConfig config = ServerConfig.load(configPath, true);

		assertTrue(Files.exists(configPath));
		assertEquals(16688, config.port());
		assertEquals(80, config.maxMessageLength());
		assertFalse(config.guiEnabled());
	}

	@Test
	void loadsConfiguredLimitsAndResolvesBanFileRelativeToConfig() throws Exception {
		Path configPath = temporaryDirectory.resolve("server").resolve("custom.properties");
		Files.createDirectories(configPath.getParent());
		Files.writeString(
				configPath,
				"""
				server.bind-address=127.0.0.1
				server.port=17777
				server.max-clients=25
				server.gui=false
				message.max-length=80
				storage.ban-file=data/bans.json
				""",
				StandardCharsets.UTF_8
		);

		ServerConfig config = ServerConfig.load(configPath, false);

		assertEquals("127.0.0.1", config.bindAddress());
		assertEquals(17777, config.port());
		assertEquals(25, config.maxClients());
		assertFalse(config.guiEnabled());
		assertEquals(
				configPath.getParent().resolve("data/bans.json").toAbsolutePath().normalize(),
				config.banFile()
		);
	}
}
