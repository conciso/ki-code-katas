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
class JoinLobbyErrorTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnErrorWhenLobbyNotFound() throws Exception {
        var messages = new ArrayBlockingQueue<String>(10);
        WebSocketSession session = connect("Alice", messages);
        receiveConnected(messages);

        sendJoinLobby(session, "UNGUELTIG");

        JsonNode error = receiveError(messages);
        assertThat(error.get("code").stringValue()).isEqualTo("LOBBY_NOT_FOUND");
    }

    @Test
    void shouldReturnErrorWhenLobbyFull() throws Exception {
        // Host erstellt Lobby
        var hostMessages = new ArrayBlockingQueue<String>(10);
        WebSocketSession hostSession = connect("Host", hostMessages);
        receiveConnected(hostMessages);
        String lobbyCode = createLobby(hostSession, hostMessages);

        // 3 weitere Spieler füllen die Lobby (max 4)
        for (int i = 1; i <= 3; i++) {
            var messages = new ArrayBlockingQueue<String>(10);
            WebSocketSession s = connect("Player" + i, messages);
            receiveConnected(messages);
            sendJoinLobby(s, lobbyCode);
            // Host bekommt LOBBY_STATE Updates - konsumieren
            hostMessages.poll(3, TimeUnit.SECONDS);
        }

        // 5. Spieler versucht beizutreten
        var fifthMessages = new ArrayBlockingQueue<String>(10);
        WebSocketSession fifthSession = connect("Fifth", fifthMessages);
        receiveConnected(fifthMessages);
        sendJoinLobby(fifthSession, lobbyCode);

        JsonNode error = receiveError(fifthMessages);
        assertThat(error.get("code").stringValue()).isEqualTo("LOBBY_FULL");
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

    private JsonNode receiveError(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine ERROR-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("ERROR");
        return msg.get("data");
    }
}
