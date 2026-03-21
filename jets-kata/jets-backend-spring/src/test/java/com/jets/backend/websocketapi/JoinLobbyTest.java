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
class JoinLobbyTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBroadcastUpdatedLobbyStateToAllPlayersAfterJoin() throws Exception {
        var aliceMessages = new ArrayBlockingQueue<String>(10);
        var bobMessages   = new ArrayBlockingQueue<String>(10);

        // --- Alice verbindet und erstellt eine Lobby ---
        WebSocketSession aliceSession = connect("Alice", aliceMessages);

        String aliceId  = receiveConnected(aliceMessages);
        String lobbyCode = createLobby(aliceSession, aliceMessages);

        // --- Bob verbindet und tritt der Lobby bei ---
        WebSocketSession bobSession = connect("Bob", bobMessages);
        String bobId = receiveConnected(bobMessages);

        sendJoinLobby(bobSession, lobbyCode);

        // --- Bob empfängt LOBBY_STATE mit beiden Spielern ---
        JsonNode bobLobbyState = receiveLobbyState(bobMessages);
        assertThat(bobLobbyState.get("lobbyCode").stringValue()).isEqualTo(lobbyCode);
        assertThat(bobLobbyState.get("hostId").stringValue()).isEqualTo(aliceId);
        assertThat(bobLobbyState.get("players").size()).isEqualTo(2);

        JsonNode bobPlayerNode = findPlayer(bobLobbyState, bobId);
        assertThat(bobPlayerNode.get("name").stringValue()).isEqualTo("Bob");
        assertThat(bobPlayerNode.get("ready").booleanValue()).isFalse();
        assertThat(bobPlayerNode.get("color").stringValue()).isEqualTo("#4444FF");

        // --- Alice empfängt ebenfalls die aktualisierte LOBBY_STATE ---
        JsonNode aliceLobbyState = receiveLobbyState(aliceMessages);
        assertThat(aliceLobbyState.get("lobbyCode").stringValue()).isEqualTo(lobbyCode);
        assertThat(aliceLobbyState.get("players").size()).isEqualTo(2);

        JsonNode alicePlayerNode = findPlayer(aliceLobbyState, aliceId);
        assertThat(alicePlayerNode.get("name").stringValue()).isEqualTo("Alice");
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

    /** Erstellt eine Lobby und gibt den lobbyCode zurück. Konsumiert LOBBY_CREATED + LOBBY_STATE. */
    private String createLobby(WebSocketSession session, ArrayBlockingQueue<String> messages) throws Exception {
        ObjectNode msg = objectMapper.createObjectNode().put("type", "CREATE_LOBBY");
        msg.set("data", objectMapper.createObjectNode());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));

        String lobbyCreatedRaw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(lobbyCreatedRaw).as("Keine LOBBY_CREATED-Nachricht erhalten").isNotNull();
        JsonNode lobbyCreated = objectMapper.readTree(lobbyCreatedRaw);
        assertThat(lobbyCreated.get("type").stringValue()).isEqualTo("LOBBY_CREATED");

        messages.poll(3, TimeUnit.SECONDS); // initiale LOBBY_STATE konsumieren

        return lobbyCreated.get("data").get("lobbyCode").stringValue();
    }

    private void sendJoinLobby(WebSocketSession session, String lobbyCode) throws Exception {
        ObjectNode data = objectMapper.createObjectNode().put("lobbyCode", lobbyCode);
        ObjectNode msg  = objectMapper.createObjectNode().put("type", "JOIN_LOBBY");
        msg.set("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    private JsonNode receiveLobbyState(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine LOBBY_STATE-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("LOBBY_STATE");
        return msg.get("data");
    }

    private JsonNode findPlayer(JsonNode lobbyStateData, String playerId) {
        for (JsonNode player : lobbyStateData.get("players")) {
            if (playerId.equals(player.get("id").stringValue())) {
                return player;
            }
        }
        throw new AssertionError("Spieler " + playerId + " nicht in LOBBY_STATE gefunden");
    }
}

