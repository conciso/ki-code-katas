package com.jets.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public GameWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions.add(session);

        GameMessage welcome = new GameMessage("CONNECTED", Map.of("sessionId", session.getId()));
        session.sendMessage(toTextMessage(welcome));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        GameMessage incoming = objectMapper.readValue(message.getPayload(), GameMessage.class);

        switch (incoming.getType()) {
            case "PING" -> {
                GameMessage pong = new GameMessage("PONG", incoming.getData());
                session.sendMessage(toTextMessage(pong));
            }
            case "CREATE_LOBBY" -> {
                String lobbyCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                String hostId = "p" + UUID.randomUUID().toString().substring(0, 7);
                GameMessage lobbyCreated = new GameMessage("LOBBY_CREATED", Map.of(
                        "lobbyCode", lobbyCode,
                        "hostId", hostId
                ));
                session.sendMessage(toTextMessage(lobbyCreated));
            }
            case "BROADCAST" -> broadcast(incoming, session);
            default -> {
                GameMessage echo = new GameMessage("ECHO", incoming.getData());
                session.sendMessage(toTextMessage(echo));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);

        GameMessage disconnected = new GameMessage("PLAYER_DISCONNECTED", Map.of("sessionId", session.getId()));
        broadcastToAll(disconnected);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("Transport error: " + exception.getMessage());
        sessions.remove(session);
    }

    private void broadcast(GameMessage message, WebSocketSession sender) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.equals(sender)) {
                try {
                    session.sendMessage(toTextMessage(message));
                } catch (IOException e) {
                    System.err.println("Error broadcasting to " + session.getId());
                }
            }
        }
    }

    private void broadcastToAll(GameMessage message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(toTextMessage(message));
                } catch (IOException e) {
                    System.err.println("Error broadcasting to " + session.getId());
                }
            }
        }
    }

    private TextMessage toTextMessage(GameMessage message) throws IOException {
        return new TextMessage(objectMapper.writeValueAsString(message));
    }
}
