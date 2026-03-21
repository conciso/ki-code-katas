package com.jets.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jets.backend.game.*;
import com.jets.backend.lobby.Lobby;
import com.jets.backend.lobby.LobbyService;
import com.jets.backend.model.Client;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final String[] PLAYER_COLORS = {"#FF4444", "#4444FF", "#44FF88", "#FFAA00"};
    private static final double HIT_RADIUS_PLAYER  = 20.0;
    private static final double HIT_RADIUS_SCOUT   = 14.0;
    private static final double HIT_RADIUS_FIGHTER = 18.0;
    private static final double HIT_RADIUS_BOMBER  = 24.0;
    private static final double HIT_RADIUS_BOSS    = 40.0;
    private static final double HIT_RADIUS_POWERUP = 28.0;
    private static final double ENEMY_SPEED_SCOUT   = 4.0;
    private static final double ENEMY_SPEED_FIGHTER = 3.0;
    private static final double ENEMY_SPEED_BOMBER  = 2.0;
    private static final double ENEMY_SPEED_BOSS    = 1.0;

    private final ObjectMapper mapper = new ObjectMapper();
    private final LobbyService lobbyService = new LobbyService();
    private final GameService gameService = new GameService();
    private final AtomicLong colorIndex = new AtomicLong();

    // sessionId → session / client / lobbyCode
    private final Map<String, WebSocketSession> sessions       = new ConcurrentHashMap<>();
    private final Map<String, Client>           sessionClient  = new ConcurrentHashMap<>();
    private final Map<String, String>           sessionLobby   = new ConcurrentHashMap<>();

    // lobbyCode → active game state / loop
    private final Map<String, GameState>          activeGames = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> gameLoops   = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // ---------------------------------------------------------------
    // Connection lifecycle
    // ---------------------------------------------------------------

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String playerName = queryParam(session.getUri().getQuery(), "playerName");
        if (playerName == null || playerName.isBlank()) {
            closeQuietly(session);
            return;
        }
        String playerId = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String color = PLAYER_COLORS[(int) (colorIndex.getAndIncrement() % PLAYER_COLORS.length)];
        Client client = new Client(playerId, playerName, color);

        sessions.put(session.getId(), session);
        sessionClient.put(session.getId(), client);

        send(session, "CONNECTED", mapper.createObjectNode()
                .put("playerId", playerId)
                .put("serverTickRate", 30));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String type = node.get("type").asText();
            JsonNode data = node.path("data");
            Client client = sessionClient.get(session.getId());
            if (client == null) return;

            switch (type) {
                case "CREATE_LOBBY"  -> handleCreateLobby(session, client);
                case "JOIN_LOBBY"    -> handleJoinLobby(session, client, data);
                case "PLAYER_READY"  -> handlePlayerReady(session, client, data);
                case "SET_GAME_MODE" -> handleSetGameMode(session, client, data);
                case "START_GAME"    -> handleStartGame(session, client);
                case "LEAVE_LOBBY"   -> handleLeaveLobby(session, client);
                case "PLAYER_INPUT"  -> handlePlayerInput(session, client, data);
                case "RETURN_TO_LOBBY" -> handleReturnToLobby(session, client);
                case "PING"          -> send(session, "PONG", data);
            }
        } catch (Exception e) {
            sendError(session, "INVALID_MESSAGE", "Nachricht konnte nicht verarbeitet werden");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Client client = sessionClient.remove(session.getId());
        sessions.remove(session.getId());
        String lobbyCode = sessionLobby.remove(session.getId());
        if (client == null || lobbyCode == null) return;

        try {
            lobbyService.leaveLobby(lobbyCode, client);
            Lobby lobby = lobbyService.getLobby(lobbyCode);
            if (lobby.getNumberPlayers() == 0) {
                stopGameLoop(lobbyCode);
                activeGames.remove(lobbyCode);
            } else {
                broadcastToLobby(lobbyCode, "DISCONNECTED", mapper.createObjectNode()
                        .put("playerId", client.getId())
                        .put("playerName", client.getName()));
                broadcastLobbyState(lobbyCode);
            }
        } catch (GameException ignored) {}
    }

    // ---------------------------------------------------------------
    // Lobby handlers
    // ---------------------------------------------------------------

    private void handleCreateLobby(WebSocketSession session, Client client) {
        try {
            Lobby lobby = lobbyService.createLobby(client);
            sessionLobby.put(session.getId(), lobby.getCode());
            send(session, "LOBBY_CREATED", mapper.createObjectNode()
                    .put("lobbyCode", lobby.getCode())
                    .put("hostId", client.getId()));
            broadcastLobbyState(lobby.getCode());
        } catch (GameException e) {
            sendError(session, e.getErrorCode(), e.getMessage());
        }
    }

    private void handleJoinLobby(WebSocketSession session, Client client, JsonNode data) {
        try {
            String code = data.get("lobbyCode").asText().toUpperCase();
            lobbyService.joinLobby(code, client);
            sessionLobby.put(session.getId(), code);
            broadcastLobbyState(code);
        } catch (GameException e) {
            sendError(session, e.getErrorCode(), e.getMessage());
        }
    }

    private void handlePlayerReady(WebSocketSession session, Client client, JsonNode data) {
        String code = sessionLobby.get(session.getId());
        if (code == null) return;
        try {
            lobbyService.setReady(code, client, data.get("ready").asBoolean());
            broadcastLobbyState(code);
        } catch (GameException e) {
            sendError(session, e.getErrorCode(), e.getMessage());
        }
    }

    private void handleSetGameMode(WebSocketSession session, Client client, JsonNode data) {
        String code = sessionLobby.get(session.getId());
        if (code == null) return;
        try {
            lobbyService.setGameMode(code, client, data.get("gameMode").asText());
            broadcastLobbyState(code);
        } catch (GameException e) {
            sendError(session, e.getErrorCode(), e.getMessage());
        }
    }

    private void handleStartGame(WebSocketSession session, Client client) {
        String code = sessionLobby.get(session.getId());
        if (code == null) return;
        try {
            lobbyService.startGame(code, client);
            Lobby lobby = lobbyService.getLobby(code);
            List<Client> players = lobby.getPlayers();

            ObjectNode startData = mapper.createObjectNode()
                    .put("countdown", 3)
                    .put("gameMode", lobby.getGameMode())
                    .put("fieldWidth", 1920)
                    .put("fieldHeight", 1080);
            ArrayNode pa = startData.putArray("players");
            for (int i = 0; i < players.size(); i++) {
                Client p = players.get(i);
                double spawnX = (1920.0 / (players.size() + 1)) * (i + 1);
                pa.addObject()
                        .put("id",     p.getId())
                        .put("name",   p.getName())
                        .put("color",  p.getColor())
                        .put("spawnX", spawnX)
                        .put("spawnY", 540.0);
            }
            broadcastToLobby(code, "GAME_STARTING", startData);

            GameState state = gameService.startGame(players.toArray(new Client[0]));
            gameService.spawnWave(state, 1);
            state.setWave(1);
            activeGames.put(code, state);
            startGameLoop(code, lobby.getGameMode());
        } catch (GameException e) {
            sendError(session, e.getErrorCode(), e.getMessage());
        }
    }

    private void handleLeaveLobby(WebSocketSession session, Client client) {
        String code = sessionLobby.remove(session.getId());
        if (code == null) return;
        try {
            lobbyService.leaveLobby(code, client);
            Lobby lobby = lobbyService.getLobby(code);
            if (lobby.getNumberPlayers() > 0) {
                broadcastLobbyState(code);
            } else {
                stopGameLoop(code);
                activeGames.remove(code);
            }
        } catch (GameException ignored) {}
    }

    private void handlePlayerInput(WebSocketSession session, Client client, JsonNode data) {
        String code = sessionLobby.get(session.getId());
        if (code == null) return;
        GameState state = activeGames.get(code);
        if (state == null) return;
        PlayerInput input = new PlayerInput(
                data.get("up").asBoolean(),
                data.get("down").asBoolean(),
                data.get("left").asBoolean(),
                data.get("right").asBoolean(),
                data.get("shoot").asBoolean(),
                data.get("seq").asInt());
        gameService.applyInput(state, client, input);
    }

    private void handleReturnToLobby(WebSocketSession session, Client client) {
        String code = sessionLobby.get(session.getId());
        if (code == null) return;
        try {
            lobbyService.returnToLobby(code, client);
            stopGameLoop(code);
            activeGames.remove(code);
            broadcastLobbyState(code);
        } catch (GameException e) {
            sendError(session, e.getErrorCode(), e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Game loop
    // ---------------------------------------------------------------

    private void startGameLoop(String lobbyCode, String gameMode) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> gameTick(lobbyCode, gameMode), 100, 33, TimeUnit.MILLISECONDS);
        gameLoops.put(lobbyCode, future);
    }

    private void stopGameLoop(String lobbyCode) {
        ScheduledFuture<?> f = gameLoops.remove(lobbyCode);
        if (f != null) f.cancel(false);
    }

    private void gameTick(String lobbyCode, String gameMode) {
        GameState state = activeGames.get(lobbyCode);
        if (state == null) return;
        try {
            Lobby lobby = lobbyService.getLobby(lobbyCode);
            List<Client> players = new ArrayList<>(lobby.getPlayers());

            gameService.tick(state);
            moveEnemies(state, players);

            List<ObjectNode> events = new ArrayList<>();
            events.addAll(checkProjectileEnemyCollisions(state, players));
            if ("FFA".equals(gameMode)) {
                events.addAll(checkProjectilePlayerCollisions(state, players));
            }
            checkPowerUpPickups(state, players, events);

            for (ObjectNode ev : events) {
                broadcastToLobby(lobbyCode, "GAME_EVENT", ev);
            }

            // Wave complete (COOP)
            if ("COOP".equals(gameMode) && state.getEnemies().isEmpty()) {
                int completedWave = state.getWave();
                int nextWave = completedWave + 1;

                ObjectNode wcData = mapper.createObjectNode()
                        .put("wave", completedWave)
                        .put("nextWaveIn", 5);
                ObjectNode scores = wcData.putObject("scores");
                for (Client p : players) {
                    PlayerState ps = state.getPlayer(p);
                    if (ps != null) scores.put(p.getId(), ps.getScore());
                }
                broadcastToLobby(lobbyCode, "WAVE_COMPLETE", wcData);

                // Temporarily mark wave as in-transition so this block doesn't fire again
                state.setWave(-1);
                scheduler.schedule(() -> {
                    GameState s = activeGames.get(lobbyCode);
                    if (s == null) return;
                    gameService.spawnWave(s, nextWave);
                    s.setWave(nextWave);
                    ObjectNode waveEventDetails = mapper.createObjectNode()
                            .put("wave", nextWave)
                            .put("enemyCount", s.getEnemies().size());
                    ObjectNode waveEvent = mapper.createObjectNode()
                            .put("event", "WAVE_START")
                            .put("x", 0.0).put("y", 0.0);
                    waveEvent.set("details", waveEventDetails);
                    broadcastToLobby(lobbyCode, "GAME_EVENT", waveEvent);
                }, 5, TimeUnit.SECONDS);
            }

            // Game over check
            boolean gameOver = false;
            String reason = "";
            if ("COOP".equals(gameMode)) {
                boolean allDeadNoRespawn = players.stream().allMatch(p -> {
                    PlayerState ps = state.getPlayer(p);
                    return ps != null && !ps.isAlive() && ps.getRespawnTicksRemaining() <= 0;
                });
                if (allDeadNoRespawn) { gameOver = true; reason = "ALL_DEAD"; }
            } else {
                if (gameService.isFfaGameOver(state)) {
                    reason = state.getTick() >= GameService.FFA_TICK_LIMIT ? "TIME_UP" : "SCORE_REACHED";
                    gameOver = true;
                }
            }

            if (gameOver) {
                stopGameLoop(lobbyCode);
                activeGames.remove(lobbyCode);
                sendGameOver(lobbyCode, reason, players, state);
                return;
            }

            broadcastGameState(lobbyCode, state, players);

        } catch (Exception e) {
            System.err.println("Game tick error [" + lobbyCode + "]: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Enemy movement
    // ---------------------------------------------------------------

    private void moveEnemies(GameState state, List<Client> players) {
        Iterator<Enemy> it = state.getEnemies().iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            double speed = switch (enemy.getType()) {
                case "SCOUT"   -> ENEMY_SPEED_SCOUT;
                case "FIGHTER" -> ENEMY_SPEED_FIGHTER;
                case "BOMBER"  -> ENEMY_SPEED_BOMBER;
                case "BOSS"    -> ENEMY_SPEED_BOSS;
                default        -> 3.0;
            };
            switch (enemy.getType()) {
                case "SCOUT", "BOMBER" -> enemy.setY(enemy.getY() + speed);
                case "FIGHTER", "BOSS" -> {
                    Client nearest = nearestAlivePlayer(state, players, enemy.getX(), enemy.getY());
                    if (nearest != null) {
                        PlayerState ps = state.getPlayer(nearest);
                        double dx = ps.getX() - enemy.getX();
                        double dy = ps.getY() - enemy.getY();
                        double len = Math.hypot(dx, dy);
                        if (len > 0) {
                            enemy.setX(enemy.getX() + dx / len * speed);
                            enemy.setY(enemy.getY() + dy / len * speed);
                        }
                    } else {
                        enemy.setY(enemy.getY() + speed);
                    }
                }
            }
            if (enemy.getY() > 1120) it.remove();
        }
    }

    private Client nearestAlivePlayer(GameState state, List<Client> players, double x, double y) {
        Client nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Client p : players) {
            PlayerState ps = state.getPlayer(p);
            if (ps == null || !ps.isAlive()) continue;
            double d = Math.hypot(ps.getX() - x, ps.getY() - y);
            if (d < minDist) { minDist = d; nearest = p; }
        }
        return nearest;
    }

    // ---------------------------------------------------------------
    // Collision detection
    // ---------------------------------------------------------------

    private List<ObjectNode> checkProjectileEnemyCollisions(GameState state, List<Client> players) {
        List<ObjectNode> events = new ArrayList<>();
        Iterator<Projectile> projIt = state.getProjectiles().iterator();
        while (projIt.hasNext()) {
            Projectile proj = projIt.next();
            boolean hit = false;
            for (Enemy enemy : new ArrayList<>(state.getEnemies())) {
                double radius = hitRadiusForType(enemy.getType());
                if (Math.hypot(proj.getX() - enemy.getX(), proj.getY() - enemy.getY()) < radius) {
                    Client shooter = playerById(players, proj.getOwnerId());
                    gameService.hitEnemy(state, enemy, shooter);
                    if (!state.getEnemies().contains(enemy)) {
                        ObjectNode ev = mapper.createObjectNode()
                                .put("event", "EXPLOSION")
                                .put("x", enemy.getX())
                                .put("y", enemy.getY());
                        ev.putObject("details")
                                .put("destroyedType", enemy.getType())
                                .put("destroyedBy", shooter != null ? shooter.getId() : "");
                        events.add(ev);
                    }
                    hit = true;
                    break;
                }
            }
            if (hit) projIt.remove();
        }
        return events;
    }

    private List<ObjectNode> checkProjectilePlayerCollisions(GameState state, List<Client> players) {
        List<ObjectNode> events = new ArrayList<>();
        Iterator<Projectile> projIt = state.getProjectiles().iterator();
        while (projIt.hasNext()) {
            Projectile proj = projIt.next();
            boolean hit = false;
            for (Client target : players) {
                PlayerState ps = state.getPlayer(target);
                if (ps == null || !ps.isAlive() || ps.isInvincible()) continue;
                if (target.getId().equals(proj.getOwnerId())) continue;
                if (Math.hypot(proj.getX() - ps.getX(), proj.getY() - ps.getY()) < HIT_RADIUS_PLAYER) {
                    Client shooter = playerById(players, proj.getOwnerId());
                    boolean aliveBefore = ps.isAlive();
                    gameService.hitPlayerByPlayer(state, target, shooter);

                    ObjectNode hitEv = mapper.createObjectNode()
                            .put("event", "PLAYER_HIT")
                            .put("x", ps.getX()).put("y", ps.getY());
                    hitEv.putObject("details")
                            .put("playerId", target.getId())
                            .put("damage", 1)
                            .put("hitBy", shooter != null ? shooter.getId() : "");
                    events.add(hitEv);

                    if (aliveBefore && !ps.isAlive()) {
                        ObjectNode killEv = mapper.createObjectNode()
                                .put("event", "PLAYER_KILLED")
                                .put("x", ps.getX()).put("y", ps.getY());
                        killEv.putObject("details")
                                .put("playerId", target.getId())
                                .put("killedBy", shooter != null ? shooter.getId() : "");
                        events.add(killEv);
                    }
                    hit = true;
                    break;
                }
            }
            if (hit) projIt.remove();
        }
        return events;
    }

    private void checkPowerUpPickups(GameState state, List<Client> players, List<ObjectNode> events) {
        for (Client player : players) {
            PlayerState ps = state.getPlayer(player);
            if (ps == null || !ps.isAlive()) continue;
            Iterator<PowerUp> puIt = state.getPowerUps().iterator();
            while (puIt.hasNext()) {
                PowerUp pu = puIt.next();
                if (Math.hypot(ps.getX() - pu.getX(), ps.getY() - pu.getY()) < HIT_RADIUS_POWERUP) {
                    gameService.applyPowerUp(state, player, pu.getType());
                    puIt.remove();
                    ObjectNode ev = mapper.createObjectNode()
                            .put("event", "POWERUP_PICKUP")
                            .put("x", pu.getX()).put("y", pu.getY());
                    ev.putObject("details")
                            .put("playerId", player.getId())
                            .put("type", pu.getType().name());
                    events.add(ev);
                    break;
                }
            }
        }
    }

    private double hitRadiusForType(String type) {
        return switch (type) {
            case "SCOUT"   -> HIT_RADIUS_SCOUT;
            case "FIGHTER" -> HIT_RADIUS_FIGHTER;
            case "BOMBER"  -> HIT_RADIUS_BOMBER;
            case "BOSS"    -> HIT_RADIUS_BOSS;
            default        -> 20.0;
        };
    }

    // ---------------------------------------------------------------
    // Broadcast helpers
    // ---------------------------------------------------------------

    private void broadcastLobbyState(String lobbyCode) {
        try {
            Lobby lobby = lobbyService.getLobby(lobbyCode);
            ObjectNode data = mapper.createObjectNode()
                    .put("lobbyCode", lobby.getCode())
                    .put("hostId",    lobby.getHost().getId())
                    .put("gameMode",  lobby.getGameMode());
            ArrayNode pa = data.putArray("players");
            for (Client p : lobby.getPlayers()) {
                pa.addObject()
                        .put("id",    p.getId())
                        .put("name",  p.getName())
                        .put("ready", p.isReady())
                        .put("color", p.getColor());
            }
            broadcastToLobby(lobbyCode, "LOBBY_STATE", data);
        } catch (GameException ignored) {}
    }

    private void broadcastGameState(String lobbyCode, GameState state, List<Client> players) {
        ObjectNode data = mapper.createObjectNode()
                .put("tick",             state.getTick())
                .put("wave",             Math.max(0, state.getWave()))
                .put("enemiesRemaining", state.getEnemies().size());

        ArrayNode pa = data.putArray("players");
        for (Client p : players) {
            PlayerState ps = state.getPlayer(p);
            if (ps == null) continue;
            String activePowerUp = null;
            if (ps.isShielded())                             activePowerUp = "SHIELD";
            else if (ps.getRapidFireTicksRemaining() > 0)   activePowerUp = "RAPID_FIRE";
            else if (ps.getSpeedBoostTicksRemaining() > 0)  activePowerUp = "SPEED_BOOST";

            ObjectNode pn = pa.addObject()
                    .put("id",                 p.getId())
                    .put("x",                  ps.getX())
                    .put("y",                  ps.getY())
                    .put("hp",                 ps.getHp())
                    .put("score",              ps.getScore())
                    .put("alive",              ps.isAlive())
                    .put("respawnIn",          ps.getRespawnTicksRemaining())
                    .put("invincible",         ps.isInvincible())
                    .put("lastProcessedInput", ps.getLastProcessedInput());
            if (activePowerUp != null) pn.put("activePowerUp", activePowerUp);
            else pn.putNull("activePowerUp");
        }

        ArrayNode projArr = data.putArray("projectiles");
        for (Projectile proj : new ArrayList<>(state.getProjectiles())) {
            projArr.addObject()
                    .put("id",    proj.getId())
                    .put("x",     proj.getX())
                    .put("y",     proj.getY())
                    .put("vx",    proj.getVx())
                    .put("vy",    proj.getVy())
                    .put("owner", proj.getOwnerId());
        }

        ArrayNode enemyArr = data.putArray("enemies");
        for (Enemy enemy : new ArrayList<>(state.getEnemies())) {
            enemyArr.addObject()
                    .put("id",   enemy.getId())
                    .put("type", enemy.getType())
                    .put("x",    enemy.getX())
                    .put("y",    enemy.getY())
                    .put("hp",   enemy.getHp());
        }

        ArrayNode puArr = data.putArray("powerUps");
        for (PowerUp pu : new ArrayList<>(state.getPowerUps())) {
            puArr.addObject()
                    .put("id",   pu.getId())
                    .put("type", pu.getType().name())
                    .put("x",    pu.getX())
                    .put("y",    pu.getY());
        }

        broadcastToLobby(lobbyCode, "GAME_STATE", data);
    }

    private void sendGameOver(String lobbyCode, String reason, List<Client> players, GameState state) {
        ObjectNode data = mapper.createObjectNode()
                .put("reason", reason)
                .put("wavesCompleted", Math.max(0, state.getWave()));
        ArrayNode scores = data.putArray("finalScores");
        int total = 0;
        for (Client p : players) {
            PlayerState ps = state.getPlayer(p);
            if (ps == null) continue;
            scores.addObject()
                    .put("playerId", p.getId())
                    .put("name",     p.getName())
                    .put("score",    ps.getScore())
                    .put("kills",    0);
            total += ps.getScore();
        }
        data.put("totalScore", total);
        broadcastToLobby(lobbyCode, "GAME_OVER", data);
    }

    private void broadcastToLobby(String lobbyCode, String type, Object data) {
        String json;
        try {
            json = mapper.writeValueAsString(Map.of("type", type, "data", data));
        } catch (Exception e) { return; }
        TextMessage msg = new TextMessage(json);
        for (Map.Entry<String, String> entry : sessionLobby.entrySet()) {
            if (!lobbyCode.equals(entry.getValue())) continue;
            WebSocketSession s = sessions.get(entry.getKey());
            if (s != null && s.isOpen()) {
                try { s.sendMessage(msg); } catch (IOException ignored) {}
            }
        }
    }

    private void send(WebSocketSession session, String type, Object data) {
        try {
            String json = mapper.writeValueAsString(Map.of("type", type, "data", data));
            session.sendMessage(new TextMessage(json));
        } catch (IOException ignored) {}
    }

    private void sendError(WebSocketSession session, String code, String message) {
        send(session, "ERROR", mapper.createObjectNode()
                .put("code", code)
                .put("message", message));
    }

    // ---------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------

    private Client playerById(List<Client> players, String id) {
        if (id == null) return null;
        return players.stream().filter(p -> id.equals(p.getId())).findFirst().orElse(null);
    }

    private String queryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try { session.close(); } catch (IOException ignored) {}
    }
}
