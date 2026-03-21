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
class PlayerReadyTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBroadcastUpdatedLobbyStateAfterPlayerReady() throws Exception {
        var aliceMessages = new ArrayBlockingQueue<String>(10);
        var bobMessages = new ArrayBlockingQueue<String>(10);

        // Alice erstellt Lobby, Bob tritt bei
        WebSocketSession aliceSession = connect("Alice", aliceMessages);
        String aliceId = receiveConnected(aliceMessages);
        String lobbyCode = createLobby(aliceSession, aliceMessages);

        WebSocketSession bobSession = connect("Bob", bobMessages);
        String bobId = receiveConnected(bobMessages);
        sendJoinLobby(bobSession, lobbyCode);
        receiveLobbyState(aliceMessages); // Alice bekommt Update nach Bob's Beitritt
        receiveLobbyState(bobMessages);   // Bob bekommt initiale LOBBY_STATE

        // Alice sendet PLAYER_READY
        sendPlayerReady(aliceSession, true);

        // Beide empfangen aktualisierte LOBBY_STATE
        JsonNode aliceLobbyState = receiveLobbyState(aliceMessages);
        JsonNode bobLobbyState = receiveLobbyState(bobMessages);

        assertThat(findPlayer(aliceLobbyState, aliceId).get("ready").booleanValue()).isTrue();
        assertThat(findPlayer(aliceLobbyState, bobId).get("ready").booleanValue()).isFalse();

        assertThat(findPlayer(bobLobbyState, aliceId).get("ready").booleanValue()).isTrue();
        assertThat(findPlayer(bobLobbyState, bobId).get("ready").booleanValue()).isFalse();
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
        assertThat(lobbyCreatedRaw).as("Keine LOBBY_CREATED-Nachricht erhalten").isNotNull();
        JsonNode lobbyCreated = objectMapper.readTree(lobbyCreatedRaw);

        messages.poll(3, TimeUnit.SECONDS); // initiale LOBBY_STATE konsumieren

        return lobbyCreated.get("data").get("lobbyCode").stringValue();
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

    private JsonNode receiveLobbyState(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine LOBBY_STATE-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("LOBBY_STATE");
        return msg.get("data");
    }

    private JsonNode findPlayer(JsonNode lobbyStateData, String playerId) {
        for (JsonNode player : lobbyStateData.get("players")) {
            if (playerId.equals(player.get("id").stringValue())) return player;
        }
        throw new AssertionError("Spieler " + playerId + " nicht in LOBBY_STATE gefunden");
    }
}
