package com.opticsvalley.irc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BanStoreTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void savesAndLoadsBanReasons() throws Exception {
		Path banFile = temporaryDirectory.resolve("data").resolve("bans.json");
		BanStore store = new BanStore(banFile);

		store.save(Map.of("Player", "刷屏", "Player02", "测试原因"));

		assertEquals(
				Map.of("Player", "刷屏", "Player02", "测试原因"),
				store.load()
		);
	}
}
