package com.opticsvalley.irc;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UsernameAllocationTest {
	@Test
	void duplicateUsernamesReceiveTwoDigitSuffixes() {
		ConcurrentHashMap<String, Object> clients = new ConcurrentHashMap<>();
		Object first = new Object();
		Object second = new Object();
		Object third = new Object();

		assertEquals("Player", IRCServer.registerUniqueUsername(clients, "Player", first));
		assertEquals("Player02", IRCServer.registerUniqueUsername(clients, "Player", second));
		assertEquals("Player03", IRCServer.registerUniqueUsername(clients, "Player", third));
		assertSame(first, clients.get("Player"));
		assertSame(second, clients.get("Player02"));
		assertSame(third, clients.get("Player03"));
	}

	@Test
	void concurrentDuplicateRegistrationsRemainUnique() throws Exception {
		ConcurrentHashMap<String, Object> clients = new ConcurrentHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Future<String>> registrations = IntStream.range(0, 20)
					.mapToObj(index -> executor.submit(() -> IRCServer.registerUniqueUsername(
							clients, "Player", new Object())))
					.toList();

			HashSet<String> assignedNames = new HashSet<>();
			for (Future<String> registration : registrations) {
				assignedNames.add(registration.get());
			}

			assertEquals(20, assignedNames.size());
			assertEquals(20, clients.size());
		} finally {
			executor.shutdownNow();
		}
	}
}
