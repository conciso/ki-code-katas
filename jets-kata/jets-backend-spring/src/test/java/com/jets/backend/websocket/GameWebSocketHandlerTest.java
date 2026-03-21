package com.jets.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class GameWebSocketHandlerTest {

    private GameWebSocketHandler handler;
    private ObjectMapper objectMapper;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new GameWebSocketHandler(objectMapper);
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("test-session-id");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void pingReturnsPongWithSameTimestamp() throws Exception {
        long timestamp = 1710950400000L;
        String pingJson = """
                {
                    "type": "PING",
                    "data": {
                        "timestamp": %d
                    }
                }
                """.formatted(timestamp);

        handler.handleMessage(session, new TextMessage(pingJson));

        verify(session).sendMessage(argThat(message -> {
            String payload = ((TextMessage) message).getPayload();
            try {
                GameMessage response = objectMapper.readValue(payload, GameMessage.class);
                return "PONG".equals(response.getType())
                        && response.getData() != null
                        && Long.valueOf(timestamp).equals(((Number) response.getData().get("timestamp")).longValue());
            } catch (Exception e) {
                return false;
            }
        }));
    }
}
