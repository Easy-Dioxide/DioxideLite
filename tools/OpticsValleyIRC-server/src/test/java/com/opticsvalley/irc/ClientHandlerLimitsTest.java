package com.opticsvalley.irc;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientHandlerLimitsTest {
	@Test
	void acceptsExactlyEightyChineseCharacters() throws Exception {
		String message = "字".repeat(80);
		BufferedReader reader = new BufferedReader(new StringReader(message + "\r\n"));

		assertEquals(message, ClientHandler.readLimitedLine(reader, 80));
	}

	@Test
	void countsEmojiAsSingleUnicodeCharacters() throws Exception {
		String message = "😀".repeat(80);
		BufferedReader reader = new BufferedReader(new StringReader(message + "\n"));

		assertEquals(message, ClientHandler.readLimitedLine(reader, 80));
	}

	@Test
	void rejectsTheEightyFirstCharacterWithoutReadingAnUnboundedLine() {
		String message = "字".repeat(81) + "\n";
		BufferedReader reader = new BufferedReader(new StringReader(message));

		assertThrows(
				ClientHandler.LineTooLongException.class,
				() -> ClientHandler.readLimitedLine(reader, 80)
		);
	}

	@Test
	void returnsNullForImmediateEndOfStream() throws Exception {
		BufferedReader reader = new BufferedReader(new StringReader(""));

		assertNull(ClientHandler.readLimitedLine(reader, 80));
	}

	@Test
	void oversizedMessageCanBeDiscardedWithoutLosingTheNextMessage() throws Exception {
		BufferedReader reader = new BufferedReader(new StringReader("超限内容\n下一条\n"));

		assertTrue(ClientHandler.discardLineRemainder(reader, 80));
		assertEquals("下一条", ClientHandler.readLimitedLine(reader, 80));
	}

	@Test
	void refusesToDrainAnUnboundedMalformedLine() throws Exception {
		BufferedReader reader = new BufferedReader(new StringReader("x".repeat(20)));

		assertFalse(ClientHandler.discardLineRemainder(reader, 10));
	}

	@Test
	void rateLimiterAllowsBurstThenRefillsAtFiveMessagesPerSecond() {
		AtomicLong clock = new AtomicLong();
		ClientHandler.RateLimiter limiter =
				new ClientHandler.RateLimiter(5.0, 10, clock::get);

		for (int i = 0; i < 10; i++) {
			assertTrue(limiter.tryAcquire());
		}
		assertFalse(limiter.tryAcquire());

		clock.addAndGet(200_000_000L);
		assertTrue(limiter.tryAcquire());
		assertFalse(limiter.tryAcquire());
	}
}
