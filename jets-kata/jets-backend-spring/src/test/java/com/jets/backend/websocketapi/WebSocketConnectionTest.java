package com.jets.backend.websocketapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
class WebSocketConnectionTest {

    @LocalServerPort
    int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReceiveConnectedMessageAfterHandshake() throws Exception {
        var messages = new ArrayBlockingQueue<String>(1);

        var handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                messages.add(message.getPayload());
            }
        };

        new StandardWebSocketClient()
                .execute(handler, "ws://localhost:" + port + "/ws/game?playerName=TestPlayer")
                .get(3, TimeUnit.SECONDS);

        String raw = messages.poll(3, TimeUnit.SECONDS);
        assertThat(raw).isNotNull();

        JsonNode msg = objectMapper.readTree(raw);
        assertThat(msg.get("type").stringValue()).isEqualTo("CONNECTED");

        JsonNode data = msg.get("data");
        assertThat(data.get("playerId").stringValue()).isNotBlank();
        assertThat(data.get("serverTickRate").intValue()).isGreaterThan(0);
    }
}
