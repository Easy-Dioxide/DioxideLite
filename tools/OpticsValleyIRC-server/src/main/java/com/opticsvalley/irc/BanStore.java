package com.opticsvalley.irc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

final class BanStore {
	private static final Type BAN_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path path;

	BanStore(Path path) {
		this.path = path;
	}

	Map<String, String> load() throws IOException {
		if (!Files.exists(path)) {
			return new HashMap<>();
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Map<String, String> loaded = gson.fromJson(reader, BAN_MAP_TYPE);
			return loaded == null ? new HashMap<>() : new HashMap<>(loaded);
		}
	}

	synchronized void save(Map<String, String> bans) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
			gson.toJson(new HashMap<>(bans), BAN_MAP_TYPE, writer);
		}

		try {
			Files.move(
					temporary,
					path,
					StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING
			);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
