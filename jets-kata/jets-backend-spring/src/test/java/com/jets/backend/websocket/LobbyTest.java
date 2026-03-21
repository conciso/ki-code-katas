package com.jets.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class LobbyTest {

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
    void createLobbyReturnsLobbyCreatedWithCodeAndHostId() throws Exception {
        String createLobbyJson = """
                {
                    "type": "CREATE_LOBBY",
                    "data": {
                        "playerName": "Alice"
                    }
                }
                """;

        handler.handleMessage(session, new TextMessage(createLobbyJson));

        verify(session).sendMessage(argThat(message -> {
            String payload = ((TextMessage) message).getPayload();
            try {
                GameMessage response = objectMapper.readValue(payload, GameMessage.class);
                Map<String, Object> data = response.getData();
                return "LOBBY_CREATED".equals(response.getType())
                        && data != null
                        && data.get("lobbyCode") != null
                        && data.get("lobbyCode").toString().length() == 6
                        && data.get("hostId") != null;
            } catch (Exception e) {
                return false;
            }
        }));
    }
}
