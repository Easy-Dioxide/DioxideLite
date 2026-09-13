package com.opticsvalley.irc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IRCServerLifecycleTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void serverRestartsAndStopsWithoutSystemExit() throws Exception {
		ServerConfig config = new ServerConfig(
				"127.0.0.1",
				0,
				4,
				false,
				32,
				80,
				1000,
				5.0,
				10,
				20,
				10,
				temporaryDirectory.resolve("bans.json")
		);
		IRCServer server = new IRCServer(config);
		AtomicReference<Throwable> startupFailure = new AtomicReference<>();
		Thread serverThread = new Thread(() -> {
			try {
				server.start();
			} catch (Throwable throwable) {
				startupFailure.set(throwable);
			}
		}, "IRC-Lifecycle-Test");
		serverThread.start();

		try {
			awaitState(server, IRCServer.ServerState.RUNNING);
			server.restart();
			awaitState(server, IRCServer.ServerState.RUNNING);
			server.shutdown();
			awaitState(server, IRCServer.ServerState.STOPPED);
			serverThread.join(2000);

			assertNull(startupFailure.get());
			assertEquals(IRCServer.ServerState.STOPPED, server.getState());
		} finally {
			server.shutdown();
		}
	}

	private static void awaitState(IRCServer server, IRCServer.ServerState expected)
			throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
		while (server.getState() != expected && Instant.now().isBefore(deadline)) {
			Thread.sleep(10);
		}
		assertEquals(expected, server.getState());
	}
}
