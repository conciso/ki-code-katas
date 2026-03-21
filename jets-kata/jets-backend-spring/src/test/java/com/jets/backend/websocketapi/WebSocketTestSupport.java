package com.jets.backend.websocketapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketTestSupport {

    private final ObjectMapper objectMapper;
    private final int port;

    WebSocketTestSupport(ObjectMapper objectMapper, int port) {
        this.objectMapper = objectMapper;
        this.port = port;
    }

    WebSocketSession connect(String playerName, ArrayBlockingQueue<String> messages) throws Exception {
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

    String receiveConnected(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine CONNECTED-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("CONNECTED");
        return msg.get("data").get("playerId").stringValue();
    }

    String createLobby(WebSocketSession session, ArrayBlockingQueue<String> messages) throws Exception {
        ObjectNode msg = objectMapper.createObjectNode().put("type", "CREATE_LOBBY");
        msg.set("data", objectMapper.createObjectNode());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));

        String lobbyCreatedRaw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(lobbyCreatedRaw).isNotNull();
        JsonNode lobbyCreated = objectMapper.readTree(lobbyCreatedRaw);
        String lobbyCode = lobbyCreated.get("data").get("lobbyCode").stringValue();

        messages.poll(3, TimeUnit.SECONDS);

        return lobbyCode;
    }

    void sendJoinLobby(WebSocketSession session, String lobbyCode) throws Exception {
        ObjectNode data = objectMapper.createObjectNode().put("lobbyCode", lobbyCode);
        ObjectNode msg = objectMapper.createObjectNode().put("type", "JOIN_LOBBY");
        msg.set("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    void sendPlayerReady(WebSocketSession session, boolean ready) throws Exception {
        ObjectNode data = objectMapper.createObjectNode().put("ready", ready);
        ObjectNode msg = objectMapper.createObjectNode().put("type", "PLAYER_READY");
        msg.set("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    void sendStartGame(WebSocketSession session) throws Exception {
        ObjectNode msg = objectMapper.createObjectNode().put("type", "START_GAME");
        msg.set("data", objectMapper.createObjectNode());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    JsonNode receiveLobbyState(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine LOBBY_STATE-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("LOBBY_STATE");
        return msg.get("data");
    }

    JsonNode receiveGameStarting(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine GAME_STARTING-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("GAME_STARTING");
        return msg.get("data");
    }

    JsonNode receiveError(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine ERROR-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("ERROR");
        return msg.get("data");
    }

    JsonNode findPlayerInGameStarting(JsonNode gameStartingData, String playerId) {
        for (JsonNode player : gameStartingData.get("players")) {
            if (playerId.equals(player.get("id").stringValue())) {
                return player;
            }
        }
        throw new AssertionError("Spieler " + playerId + " nicht in GAME_STARTING gefunden");
    }
}

