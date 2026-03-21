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
class PingPongTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRespondWithPongContainingSameTimestamp() throws Exception {
        var messages = new ArrayBlockingQueue<String>(10);
        WebSocketSession session = connect("Alice", messages);
        receiveConnected(messages);

        long timestamp = System.currentTimeMillis();
        sendPing(session, timestamp);

        JsonNode pongData = receivePong(messages);
        assertThat(pongData.get("timestamp").longValue()).isEqualTo(timestamp);
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

    private void receiveConnected(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine CONNECTED-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("CONNECTED");
    }

    private void sendPing(WebSocketSession session, long timestamp) throws Exception {
        ObjectNode data = objectMapper.createObjectNode().put("timestamp", timestamp);
        ObjectNode msg = objectMapper.createObjectNode().put("type", "PING");
        msg.set("data", data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    private JsonNode receivePong(ArrayBlockingQueue<String> messages) throws Exception {
        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).as("Keine PONG-Nachricht erhalten").isNotNull();
        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("PONG");
        return msg.get("data");
    }
}
