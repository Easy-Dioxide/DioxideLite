package com.dioxideliteng.krs.utils.network;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class WebAccessUtil {
    private static final int MAX_RESPONSE_BYTES = 1_000_000;
    private static final int MAX_PLAYERS = 512;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            runnable -> {
                Thread thread = new Thread(runnable, "krs-web-access");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );
    private static final AtomicBoolean REQUEST_IN_FLIGHT = new AtomicBoolean();

    private WebAccessUtil() {
    }

    public static void getFukumaiPlayersInfoAsync(Consumer<List<PlayerInfo>> callback) {
        if (callback == null || !REQUEST_IN_FLIGHT.compareAndSet(false, true))
            return;

        try {
            EXECUTOR.execute(() -> {
                try {
                    callback.accept(getFukumaiPlayersInfo());
                } catch (RuntimeException ignored) {
                } finally {
                    REQUEST_IN_FLIGHT.set(false);
                }
            });
        } catch (RejectedExecutionException ignored) {
            REQUEST_IN_FLIGHT.set(false);
        }
    }

    public static List<PlayerInfo> getFukumaiPlayersInfo() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://map.fukumaisaba.net/tiles/players.json"))
                    .header("accept", "*/*")
                    .header("accept-language", "ja,en-US;q=0.9,en;q=0.8")
                    .header("referer", "https://map.fukumaisaba.net/")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.limiting(
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8), MAX_RESPONSE_BYTES));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                return null;

            String body = response.body();
            if (body == null)
                return null;

            JSONObject jsonObj = new JSONObject(body);
            var playersArray = jsonObj.getJSONArray("players");
            ArrayList<PlayerInfo> playerList = new ArrayList<>();
            int playerCount = Math.min(playersArray.length(), MAX_PLAYERS);
            for (int i = 0; i < playerCount; i++) {
                try {
                    JSONObject player = playersArray.getJSONObject(i);
                    playerList.add(new PlayerInfo(
                            player.getString("name"),
                            player.getInt("x"),
                            player.getInt("y"),
                            player.getInt("z"),
                            player.getInt("health")
                    ));
                } catch (RuntimeException ignored) {
                }
            }

            return playerList.isEmpty() ? null : List.copyOf(playerList);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
        CLIENT.shutdownNow();
        REQUEST_IN_FLIGHT.set(false);
    }

    public record PlayerInfo(String name, int x, int y, int z, int health) {
        public String getName() {
            return name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getHealth() {
            return health;
        }
    }
}
