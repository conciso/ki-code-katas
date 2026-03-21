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
class CreateLobbyTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReceiveLobbyCreatedAndLobbyStateAfterCreateLobby() throws Exception {
        // Kapazität 10: CONNECTED + LOBBY_CREATED + LOBBY_STATE
        var messages = new ArrayBlockingQueue<String>(10);

        var handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                messages.add(message.getPayload());
            }
        };

        WebSocketSession session = new StandardWebSocketClient()
                .execute(handler, "ws://localhost:" + port + "/ws/game?playerName=Alice")
                .get(3, TimeUnit.SECONDS);

        // 1. CONNECTED empfangen – playerId merken
        String connectedRaw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(connectedRaw).isNotNull();
        JsonNode connected = objectMapper.readTree(connectedRaw);
        assertThat(connected.get("type").stringValue()).isEqualTo("CONNECTED");
        String playerId = connected.get("data").get("playerId").stringValue();

        // 2. CREATE_LOBBY senden
        ObjectNode createLobbyData = objectMapper.createObjectNode()
                .put("playerName", "Alice");
        ObjectNode createLobbyMsg = objectMapper.createObjectNode()
                .put("type", "CREATE_LOBBY");
        createLobbyMsg.set("data", createLobbyData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(createLobbyMsg)));

        // 3. LOBBY_CREATED empfangen
        String lobbyCreatedRaw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(lobbyCreatedRaw)
                .as("Keine LOBBY_CREATED-Nachricht erhalten")
                .isNotNull();
        JsonNode lobbyCreated = objectMapper.readTree(lobbyCreatedRaw);
        assertThat(lobbyCreated.get("type").stringValue()).isEqualTo("LOBBY_CREATED");

        JsonNode lobbyCreatedData2 = lobbyCreated.get("data");
        String lobbyCode = lobbyCreatedData2.get("lobbyCode").stringValue();
        assertThat(lobbyCode).hasSize(6);
        assertThat(lobbyCreatedData2.get("hostId").stringValue()).isEqualTo(playerId);

        // 4. LOBBY_STATE empfangen
        String lobbyStateRaw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(lobbyStateRaw)
                .as("Keine LOBBY_STATE-Nachricht erhalten")
                .isNotNull();
        JsonNode lobbyState = objectMapper.readTree(lobbyStateRaw);
        assertThat(lobbyState.get("type").stringValue()).isEqualTo("LOBBY_STATE");

        JsonNode lobbyStateData = lobbyState.get("data");
        assertThat(lobbyStateData.get("lobbyCode").stringValue()).isEqualTo(lobbyCode);
        assertThat(lobbyStateData.get("hostId").stringValue()).isEqualTo(playerId);
        assertThat(lobbyStateData.get("gameMode").stringValue()).isNotBlank();
        assertThat(lobbyStateData.get("players").isArray()).isTrue();
        assertThat(lobbyStateData.get("players").size()).isEqualTo(1);

        JsonNode firstPlayer = lobbyStateData.get("players").get(0);
        assertThat(firstPlayer.get("id").stringValue()).isEqualTo(playerId);
        assertThat(firstPlayer.get("name").stringValue()).isEqualTo("Alice");
        assertThat(firstPlayer.get("ready").booleanValue()).isFalse();
        assertThat(firstPlayer.get("color").stringValue()).isNotBlank();
    }
}

