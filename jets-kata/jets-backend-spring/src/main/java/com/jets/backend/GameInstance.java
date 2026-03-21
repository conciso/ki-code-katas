package com.jets.backend;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GameInstance {

    private final String lobbyCode;
    private final List<PlayerState> players;
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> sessions;
    private final AtomicInteger tick = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final double[] SPAWN_X = {200, 600, 1000, 1400};
    private static final double SPAWN_Y = 540;
    private static final long TICK_RATE_MS = 1000 / 30;

    public GameInstance(String lobbyCode, List<Player> lobbyPlayers, List<WebSocketSession> sessions) {
        this.lobbyCode = lobbyCode;
        this.sessions = sessions;
        this.players = new CopyOnWriteArrayList<>();
        for (int i = 0; i < lobbyPlayers.size(); i++) {
            Player p = lobbyPlayers.get(i);
            players.add(new PlayerState(p.getId(), p.getName(), SPAWN_X[i], SPAWN_Y));
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, TICK_RATE_MS, TICK_RATE_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public void applyInput(String playerId, boolean up, boolean down, boolean left, boolean right, boolean shoot) {
        players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .ifPresent(p -> p.applyInput(up, down, left, right, shoot));
    }

    private void tick() {
        tick.incrementAndGet();

        for (PlayerState p : players) {
            p.update();
            if (p.canShoot()) {
                projectiles.add(new Projectile(
                        UUID.randomUUID().toString().substring(0, 6), p.getX(), p.getY(), p.getId()));
                p.onShoot();
            }
        }

        projectiles.removeIf(Projectile::isOutOfBounds);
        for (Projectile proj : projectiles) {
            proj.update();
        }

        broadcast(buildGameState());
    }

    private void broadcast(String message) {
        TextMessage msg = new TextMessage(message);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(msg);
                } catch (Exception ignored) {}
            }
        }
    }

    private String buildGameState() {
        StringBuilder playersJson = new StringBuilder("[");
        for (int i = 0; i < players.size(); i++) {
            PlayerState p = players.get(i);
            playersJson.append("{\"id\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"hp\":%d,\"score\":%d,\"alive\":true}"
                    .formatted(p.getId(), p.getX(), p.getY(), p.getHp(), p.getScore()));
            if (i < players.size() - 1) playersJson.append(",");
        }
        playersJson.append("]");

        StringBuilder projectilesJson = new StringBuilder("[");
        List<Projectile> snapshot = List.copyOf(projectiles);
        for (int i = 0; i < snapshot.size(); i++) {
            Projectile proj = snapshot.get(i);
            projectilesJson.append("{\"id\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"vx\":%.1f,\"vy\":%.1f,\"owner\":\"%s\"}"
                    .formatted(proj.getId(), proj.getX(), proj.getY(), proj.getVx(), proj.getVy(), proj.getOwner()));
            if (i < snapshot.size() - 1) projectilesJson.append(",");
        }
        projectilesJson.append("]");

        return "{\"type\":\"GAME_STATE\",\"data\":{\"tick\":%d,\"players\":%s,\"projectiles\":%s}}"
                .formatted(tick.get(), playersJson, projectilesJson);
    }
}
