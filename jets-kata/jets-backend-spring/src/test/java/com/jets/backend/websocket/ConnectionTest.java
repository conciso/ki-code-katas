package com.jets.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ConnectionTest {

    private GameWebSocketHandler handler;
    private ObjectMapper objectMapper;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new GameWebSocketHandler(objectMapper);
        session = mock(WebSocketSession.class);
    }

    @Test
    void connectionReturnsConnectedMessageWithPlayerIdAndServerTickRate() throws Exception {
        String playerName = "TestPlayer";
        when(session.getUri()).thenReturn(URI.create("/ws/game?playerName=" + playerName));

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(argThat(message -> {
            String payload = ((TextMessage) message).getPayload();
            try {
                GameMessage response = objectMapper.readValue(payload, GameMessage.class);
                Map<String, Object> data = response.getData();
                return "CONNECTED".equals(response.getType())
                        && data != null
                        && data.get("playerId") != null
                        && Integer.valueOf(30).equals(data.get("serverTickRate"));
            } catch (Exception e) {
                return false;
            }
        }));
    }
}
