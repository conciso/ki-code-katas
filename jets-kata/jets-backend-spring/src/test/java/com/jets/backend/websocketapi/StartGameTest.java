package com.jets.backend.websocketapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StartGameTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldStartGameAndBroadcastGameStartingWhenHostAndAllPlayersReady() throws Exception {
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);

        // --- Alice verbindet und erstellt eine Lobby ---
        WebSocketSession aliceSession = connect("Alice", aliceMessages);
        String aliceId = receiveConnected(aliceMessages);
        String lobbyCode = createLobby(aliceSession, aliceMessages);

        // --- Bob verbindet und tritt bei ---
        WebSocketSession bobSession = connect("Bob", bobMessages);
        String bobId = receiveConnected(bobMessages);
        sendJoinLobby(bobSession, lobbyCode);
        receiveLobbyState(bobMessages); // Bob empfängt LOBBY_STATE
        receiveLobbyState(aliceMessages); // Alice empfängt LOBBY_STATE

        // --- Beide Spieler signalisieren Bereitschaft ---
        sendPlayerReady(aliceSession, true);
        receiveLobbyState(aliceMessages); // Alice: LOBBY_STATE mit ready=true
        receiveLobbyState(bobMessages);   // Bob: LOBBY_STATE mit ready=true

        sendPlayerReady(bobSession, true);
        receiveLobbyState(aliceMessages); // Alice: LOBBY_STATE mit ready=true
        receiveLobbyState(bobMessages);   // Bob: LOBBY_STATE mit ready=true

        // --- Host (Alice) startet das Spiel ---
        sendStartGame(aliceSession);

        // --- GAME_STARTING sollte an beide Spieler gesendet werden ---
        JsonNode aliceGameStarting = receiveGameStarting(aliceMessages);
        JsonNode bobGameStarting = receiveGameStarting(bobMessages);

        assertThat(aliceGameStarting.get("countdown").intValue()).isGreaterThan(0);
        assertThat(aliceGameStarting.get("gameMode").stringValue()).isEqualTo("COOP");
        assertThat(aliceGameStarting.get("fieldWidth").intValue()).isGreaterThan(0);
        assertThat(aliceGameStarting.get("fieldHeight").intValue()).isGreaterThan(0);

        // --- Spieler sollten spawn-Positionen haben ---
        assertThat(aliceGameStarting.get("players").isArray()).isTrue();
        assertThat(aliceGameStarting.get("players").size()).isEqualTo(2);

        JsonNode alicePlayer = findPlayerInGameStarting(aliceGameStarting, aliceId);
        assertThat(alicePlayer.get("name").stringValue()).isEqualTo("Alice");
        assertThat(alicePlayer.has("spawnX")).isTrue();
        assertThat(alicePlayer.has("spawnY")).isTrue();

        JsonNode bobPlayer = findPlayerInGameStarting(aliceGameStarting, bobId);
        assertThat(bobPlayer.get("name").stringValue()).isEqualTo("Bob");
    }

    @Test
    void shouldReturnErrorWhenNonHostStartsGame() throws Exception {
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = connect("Alice", aliceMessages);
        receiveConnected(aliceMessages);
        String lobbyCode = createLobby(aliceSession, aliceMessages);

        WebSocketSession bobSession = connect("Bob", bobMessages);
        receiveConnected(bobMessages);
        sendJoinLobby(bobSession, lobbyCode);
        receiveLobbyState(bobMessages);
        receiveLobbyState(aliceMessages);

        sendPlayerReady(aliceSession, true);
        receiveLobbyState(aliceMessages);
        receiveLobbyState(bobMessages);

        sendPlayerReady(bobSession, true);
        receiveLobbyState(aliceMessages);
        receiveLobbyState(bobMessages);

        // Bob ist nicht Host und darf START_GAME nicht auslösen
        sendStartGame(bobSession);

        JsonNode error = receiveError(bobMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("NOT_HOST");
    }

    @Test
    void shouldReturnErrorWhenNotAllPlayersAreReady() throws Exception {
        var aliceMessages = new ArrayBlockingQueue<String>(20);
        var bobMessages   = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = connect("Alice", aliceMessages);
        receiveConnected(aliceMessages);
        String lobbyCode = createLobby(aliceSession, aliceMessages);

        WebSocketSession bobSession = connect("Bob", bobMessages);
        receiveConnected(bobMessages);
        sendJoinLobby(bobSession, lobbyCode);
        receiveLobbyState(bobMessages);
        receiveLobbyState(aliceMessages);

        // Nur Alice ist bereit
        sendPlayerReady(aliceSession, true);
        receiveLobbyState(aliceMessages);
        receiveLobbyState(bobMessages);

        // Host versucht zu starten, obwohl Bob nicht ready ist
        sendStartGame(aliceSession);

        JsonNode error = receiveError(aliceMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("NOT_ALL_READY");
    }


    @Test
    void shouldReturnErrorWhenOnlyOnePlayerInLobby() throws Exception {
        var aliceMessages = new ArrayBlockingQueue<String>(20);

        WebSocketSession aliceSession = connect("Alice", aliceMessages);
        receiveConnected(aliceMessages);
        createLobby(aliceSession, aliceMessages);

        // Alice ist alleine in der Lobby und bereit
        sendPlayerReady(aliceSession, true);
        receiveLobbyState(aliceMessages);

        // Host versucht zu starten, obwohl nur 1 Spieler in der Lobby
        sendStartGame(aliceSession);

        JsonNode error = receiveError(aliceMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("NOT_ENOUGH_PLAYERS");
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private WebSocketSession connect(String playerName, ArrayBlockingQueue<String> messages) throws Exception {
        var handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                messages.add(message.getPayload());
            }
        };
        return new StandardWebSocketClient()
                .execute(handler, "ws://localhost:" + port + "/ws/game?playerName=" + playerName)
                .get(3, TimeUnit.SECONDS);
    }

    private String receiveConnected(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine CONNECTED-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("CONNECTED");
        return msg.get("data").get("playerId").stringValue();
    }

    private String createLobby(WebSocketSession session, ArrayBlockingQueue<String> messages) throws Exception {
        ObjectNode msg = objectMapper.createObjectNode().put("type", "CREATE_LOBBY");
        msg.set("data", objectMapper.createObjectNode());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));

        String lobbyCreatedRaw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(lobbyCreatedRaw).isNotNull();
        JsonNode lobbyCreated = objectMapper.readTree(lobbyCreatedRaw);
        String lobbyCode = lobbyCreated.get("data").get("lobbyCode").stringValue();

        messages.poll(3, TimeUnit.SECONDS); // initiale LOBBY_STATE konsumieren

        return lobbyCode;
    }

    private void sendJoinLobby(WebSocketSession session, String lobbyCode) throws Exception {
        ObjectNode data = objectMapper.createObjectNode().put("lobbyCode", lobbyCode);
        ObjectNode msg = objectMapper.createObjectNode().put("type", "JOIN_LOBBY");
        msg.set("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    private void sendPlayerReady(WebSocketSession session, boolean ready) throws Exception {
        ObjectNode data = objectMapper.createObjectNode().put("ready", ready);
        ObjectNode msg = objectMapper.createObjectNode().put("type", "PLAYER_READY");
        msg.set("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    private void sendStartGame(WebSocketSession session) throws Exception {
        ObjectNode msg = objectMapper.createObjectNode().put("type", "START_GAME");
        msg.set("data", objectMapper.createObjectNode());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    private JsonNode receiveLobbyState(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine LOBBY_STATE-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("LOBBY_STATE");
        return msg.get("data");
    }

    private JsonNode receiveGameStarting(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine GAME_STARTING-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("GAME_STARTING");
        return msg.get("data");
    }

    private JsonNode receiveError(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine ERROR-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("ERROR");
        return msg.get("data");
    }

    private JsonNode findPlayerInGameStarting(JsonNode gameStartingData, String playerId) {
        for (JsonNode player : gameStartingData.get("players")) {
            if (playerId.equals(player.get("id").stringValue())) {
                return player;
            }
        }
        throw new AssertionError("Spieler " + playerId + " nicht in GAME_STARTING gefunden");
    }
}

